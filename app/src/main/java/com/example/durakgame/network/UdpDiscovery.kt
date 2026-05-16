package com.example.durakgame.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class UdpDiscovery {

    companion object {
        const val DISCOVERY_PORT = 55556
        const val TAG = "UdpDiscovery"
    }

    data class DiscoveredHost(
        val hostAddress: String,
        val gameCode: String,
        val port: Int
    )

    private val _hosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val hosts: StateFlow<List<DiscoveredHost>> = _hosts.asStateFlow()

    private var announceJob: Job? = null
    private var listenJob: Job? = null
    private var socket: DatagramSocket? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startAnnouncing(gameCode: String, port: Int) {
        announceJob = scope.launch {
            try {
                socket = DatagramSocket()
                val message = "DURAK:$gameCode:$port".toByteArray()
                val address = InetAddress.getByName("255.255.255.255")

                while (isActive) {
                    val packet = DatagramPacket(message, message.size, InetSocketAddress(address, DISCOVERY_PORT))
                    socket?.send(packet)
                    delay(1000) // Рассылаем каждую секунду
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Announce error: ${e.message}")
            }
        }
    }

    fun startListening() {
        _hosts.value = emptyList()
        listenJob = scope.launch {
            try {
                socket = DatagramSocket(DISCOVERY_PORT)
                socket?.broadcast = true
                val buffer = ByteArray(256)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()

                    if (message.startsWith("DURAK:")) {
                        val parts = message.split(":")
                        if (parts.size == 3) {
                            val gameCode = parts[1]
                            val port = parts[2].toIntOrNull() ?: 55555
                            val host = DiscoveredHost(
                                hostAddress = packet.address.hostAddress ?: "unknown",
                                gameCode = gameCode,
                                port = port
                            )
                            val current = _hosts.value.toMutableList()
                            if (!current.any { it.hostAddress == host.hostAddress && it.gameCode == host.gameCode }) {
                                current.add(host)
                                _hosts.value = current
                            }

                            // Удаляем старые хосты через 5 секунд
                            delay(5000)
                            _hosts.value = _hosts.value.filter {
                                it.hostAddress != host.hostAddress || it.gameCode != host.gameCode
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Listen error: ${e.message}")
            }
        }
    }

    fun stop() {
        announceJob?.cancel()
        listenJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
    }
}