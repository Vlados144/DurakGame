package com.example.durakgame.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NsdHelper(private val context: Context) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredServices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredServices: StateFlow<List<NsdServiceInfo>> = _discoveredServices.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    companion object {
        const val SERVICE_TYPE = "_durak._tcp."
        const val TAG = "NsdHelper"
    }

    fun registerService(port: Int, gameCode: String) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "DurakGame-$gameCode-$port"  // ← добавили порт
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Игра зарегистрирована: ${serviceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Ошибка регистрации: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Игра снята с регистрации")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Ошибка снятия с регистрации: $errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка registerService: ${e.message}")
        }
    }

    fun unregisterService() {
        try {
            registrationListener?.let {
                nsdManager.unregisterService(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка unregisterService: ${e.message}")
        }
        registrationListener = null
    }

    fun startDiscovery() {
        _discoveredServices.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Поиск начат")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Найдена игра: ${service.serviceName}")
                if (service.serviceType == SERVICE_TYPE) {
                    try {
                        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.e(TAG, "Ошибка разрешения: $errorCode")
                            }

                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                Log.d(TAG, "Разрешён: ${serviceInfo.serviceName}, хост: ${serviceInfo.host}")
                                val currentList = _discoveredServices.value.toMutableList()
                                if (!currentList.any { it.serviceName == serviceInfo.serviceName }) {
                                    currentList.add(serviceInfo)
                                    _discoveredServices.value = currentList
                                }
                            }
                        })
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка resolveService: ${e.message}")
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Игра потеряна: ${service.serviceName}")
                _discoveredServices.value = _discoveredServices.value.filter {
                    it.serviceName != service.serviceName
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Поиск остановлен")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Ошибка запуска поиска: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Ошибка остановки поиска: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка startDiscovery: ${e.message}")
        }
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка stopDiscovery: ${e.message}")
        }
        discoveryListener = null
    }

    fun getServiceHost(serviceInfo: NsdServiceInfo): String? {
        return serviceInfo.host?.hostAddress
    }
}