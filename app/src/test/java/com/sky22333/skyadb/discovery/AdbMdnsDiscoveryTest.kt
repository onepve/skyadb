package com.sky22333.skyadb.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class AdbMdnsDiscoveryTest {
    @Test
    fun serviceTypes_matchAdbMdnsRecords() {
        assertEquals("_adb-tls-pairing._tcp.", AdbMdnsServiceType.Pairing.nsdType)
        assertEquals("_adb-tls-connect._tcp.", AdbMdnsServiceType.Connect.nsdType)
        assertEquals("_adb._tcp.", AdbMdnsServiceType.Legacy.nsdType)
    }

    @Test
    fun endpoint_exposesStableIdAndEndpointText() {
        val endpoint = AdbMdnsEndpoint(
            name = "Redmi",
            host = "192.168.1.23",
            port = 37125,
            type = AdbMdnsServiceType.Pairing,
        )

        assertEquals("Pairing:192.168.1.23:37125", endpoint.id)
        assertEquals("192.168.1.23:37125", endpoint.endpoint)
    }
}
