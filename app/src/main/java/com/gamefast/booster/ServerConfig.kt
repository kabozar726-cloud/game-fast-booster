package com.gamefast.booster

/**
 * VPN server list. These are PLACEHOLDERS — replace endpoint + publicKey
 * with the real values from each rented WireGuard server (see server-setup.md).
 * Never ship real private keys in the app. Only public keys belong here.
 */
data class VpnServer(
    val id: String,
    val name: String,
    val region: String,
    val endpoint: String,      // e.g. "203.0.113.10:51820"
    val publicKey: String,     // server's WireGuard public key (base64)
    val allowedIps: String = "0.0.0.0/0, ::/0",
)

object Servers {
    val all = listOf(
        VpnServer(
            id = "uae",
            name = "Middle East (UAE)",
            region = "Closest to PUBG Middle East servers",
            endpoint = "REPLACE_WITH_UAE_SERVER_IP:51820",
            publicKey = "REPLACE_WITH_UAE_SERVER_PUBLIC_KEY",
        ),
        VpnServer(
            id = "singapore",
            name = "Asia (Singapore)",
            region = "Good for Asia-region matches",
            endpoint = "REPLACE_WITH_SG_SERVER_IP:51820",
            publicKey = "REPLACE_WITH_SG_SERVER_PUBLIC_KEY",
        ),
        VpnServer(
            id = "europe",
            name = "Europe (Germany)",
            region = "Good for Europe-region matches",
            endpoint = "REPLACE_WITH_EU_SERVER_IP:51820",
            publicKey = "REPLACE_WITH_EU_SERVER_PUBLIC_KEY",
        ),
    )

    /** WireGuard quick-config text for one server + this device's keypair. */
    fun wgQuickConfig(server: VpnServer, privateKey: String, address: String): String {
        return """
            [Interface]
            PrivateKey = $privateKey
            Address = $address
            DNS = 1.1.1.1

            [Peer]
            PublicKey = ${server.publicKey}
            Endpoint = ${server.endpoint}
            AllowedIPs = ${server.allowedIps}
            PersistentKeepalive = 25
        """.trimIndent()
    }
}
