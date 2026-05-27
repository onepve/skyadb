package com.sky22333.skyadb.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.ArrayDeque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAdbMdnsDiscovery(
    context: Context,
) : AdbMdnsDiscovery {
    private val nsdManager = context.applicationContext.getSystemService(NsdManager::class.java)
    private val lock = Any()
    private val listeners = mutableMapOf<AdbMdnsServiceType, NsdManager.DiscoveryListener>()
    private val endpoints = linkedMapOf<String, AdbMdnsEndpoint>()
    private val pendingResolves = ArrayDeque<Pair<NsdServiceInfo, AdbMdnsServiceType>>()
    private var resolving = false
    private var active = false

    private val mutableState = MutableStateFlow(AdbMdnsDiscoveryState())
    override val state: StateFlow<AdbMdnsDiscoveryState> = mutableState.asStateFlow()

    override fun start() {
        synchronized(lock) {
            if (active) return
            active = true
            endpoints.clear()
            pendingResolves.clear()
            resolving = false
            mutableState.value = AdbMdnsDiscoveryState(running = true)
        }

        AdbMdnsServiceType.entries.forEach(::startServiceType)
    }

    override fun stop() {
        val currentListeners = synchronized(lock) {
            active = false
            resolving = false
            pendingResolves.clear()
            endpoints.clear()
            listeners.values.toList().also { listeners.clear() }
        }

        currentListeners.forEach { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }

        synchronized(lock) {
            mutableState.value = AdbMdnsDiscoveryState(running = false)
        }
    }

    private fun startServiceType(type: AdbMdnsServiceType) {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                updateError(null)
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                enqueueResolve(serviceInfo, type)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                removeEndpoint(type = type, name = serviceInfo.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                updateError("自动发现启动失败，可继续使用网段扫描。")
                runCatching { nsdManager.stopServiceDiscovery(this) }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { nsdManager.stopServiceDiscovery(this) }
            }
        }

        synchronized(lock) {
            if (!active) return
            listeners[type] = listener
        }

        runCatching {
            nsdManager.discoverServices(type.nsdType, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            updateError("自动发现不可用，可继续使用网段扫描。")
        }
    }

    private fun enqueueResolve(serviceInfo: NsdServiceInfo, type: AdbMdnsServiceType) {
        synchronized(lock) {
            if (!active) return
            pendingResolves.add(serviceInfo to type)
        }
        resolveNext()
    }

    private fun resolveNext() {
        val next = synchronized(lock) {
            if (!active || resolving || pendingResolves.isEmpty()) return
            resolving = true
            pendingResolves.removeFirst()
        }

        runCatching {
            nsdManager.resolveService(
                next.first,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        synchronized(lock) {
                            resolving = false
                        }
                        resolveNext()
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress.orEmpty()
                        val port = serviceInfo.port
                        if (host.isNotBlank() && port in 1..65535) {
                            addEndpoint(
                                AdbMdnsEndpoint(
                                    name = serviceInfo.serviceName.ifBlank { next.second.label },
                                    host = host,
                                    port = port,
                                    type = next.second,
                                ),
                            )
                        }
                        synchronized(lock) {
                            resolving = false
                        }
                        resolveNext()
                    }
                },
            )
        }.onFailure {
            synchronized(lock) {
                resolving = false
            }
            resolveNext()
        }
    }

    private fun addEndpoint(endpoint: AdbMdnsEndpoint) {
        synchronized(lock) {
            if (!active) return
            endpoints[endpoint.id] = endpoint
            publishStateLocked(error = null)
        }
    }

    private fun removeEndpoint(type: AdbMdnsServiceType, name: String) {
        synchronized(lock) {
            if (!active) return
            endpoints.entries.removeAll { (_, endpoint) ->
                endpoint.type == type && endpoint.name == name
            }
            publishStateLocked(error = mutableState.value.error)
        }
    }

    private fun updateError(error: String?) {
        synchronized(lock) {
            if (!active) return
            publishStateLocked(error = error)
        }
    }

    private fun publishStateLocked(error: String?) {
        mutableState.value = AdbMdnsDiscoveryState(
            running = active,
            endpoints = endpoints.values.sortedWith(
                compareBy<AdbMdnsEndpoint> { it.type.ordinal }
                    .thenBy { it.name }
                    .thenBy { it.host }
                    .thenBy { it.port },
            ),
            error = error,
        )
    }
}
