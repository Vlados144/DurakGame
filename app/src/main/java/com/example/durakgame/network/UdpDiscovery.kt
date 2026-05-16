package com.example.durakgame.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        announceJob?.cancel()
        announceJob = scope.launch {
            try {
                val announceSocket = DatagramSocket()
                val message = "DURAK:$gameCode:$port".toByteArray()
                val address = InetAddress.getByName("255.255.255.255")

                while (isActive) {
                    try {
                        val packet = DatagramPacket(message, message.size, InetSocketAddress(address, DISCOVERY_PORT))
                        announceSocket.send(packet)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Send broadcast error: ${e.message}")
                    }
                    delay(1000) // Рассылаем каждую секунду
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Announce error: ${e.message}")
            }
        }
    }

    fun startListening() {
        _hosts.value = emptyList()
        listenJob?.cancel()
        listenJob = scope.launch {
            try {
                socket?.close()
                socket = DatagramSocket(DISCOVERY_PORT).apply { broadcast = true }
                val buffer = ByteArray(256)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket?.receive(packet)
                    } catch (e: Exception) {
                        if (!isActive) break
                        continue
                    }
                    val message = String(packet.data, 0, packet.length).trim()

                    if (message.startsWith("DURAK:")) {
                        val parts = message.split(":")
                        if (parts.size >= 3) {
                            val gameCode = parts[1]
                            val port = parts[2].toIntOrNull() ?: 55555
                            val hostAddr = packet.address.hostAddress ?: "unknown"

                            val newHost = DiscoveredHost(
                                hostAddress = hostAddr,
                                gameCode = gameCode,
                                port = port
                            )

                            // Обновляем список: заменяем старую запись от этого IP на новую
                            _hosts.update { current ->
                                val map = current.associateBy { it.hostAddress }.toMutableMap()
                                map[hostAddr] = newHost
                                map.values.toList()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) android.util.Log.e(TAG, "Listen error: ${e.message}")
            }
        }
    }

    fun stop() {
        announceJob?.cancel()
        listenJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}