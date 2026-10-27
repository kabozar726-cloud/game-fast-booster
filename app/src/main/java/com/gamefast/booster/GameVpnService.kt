package com.gamefast.booster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

/**
 * Real Android VPN service (VpnService + WireGuard).
 *
 * This is the part a web page can NEVER do: only a native app installed
 * on the phone can ask Android for VPN permission and actually route
 * traffic. The first Connect shows Android's system VPN consent dialog.
 *
 * Nothing in this service touches the PUBG game client, its files, or
 * any anti-cheat system. It only routes network traffic through the
 * selected WireGuard server.
 */
class GameVpnService : VpnService() {

    private val backend: GoBackend by lazy { GoBackend(applicationContext) }
    private var tunnel: Tunnel? = null
    private var tunFd: ParcelFileDescriptor? = null

    private val tunnelImpl = object : Tunnel {
        override fun getName(): String = "gamefast"
        override fun onStateChange(newState: Tunnel.State) {
            // State changes are reported back to the UI via broadcast.
            val intent = Intent(ACTION_STATE).apply {
                putExtra(EXTRA_STATE, newState.name)
            }
            sendBroadcast(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverId = intent.getStringExtra(EXTRA_SERVER_ID)
                    ?: Servers.all.first().id
                val server = Servers.all.firstOrNull { it.id == serverId }
                    ?: Servers.all.first()
                connect(server)
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }

    private fun connect(server: VpnServer) {
        try {
            startForeground(NOTIFICATION_ID, buildNotification(server.name))

            // Device keypair: generated once and stored in app-private storage.
            // In production, generate with WireGuard's key tools and store in
            // EncryptedSharedPreferences; the placeholder below must be replaced.
            val prefs = getSharedPreferences("wg", MODE_PRIVATE)
            var privateKey = prefs.getString("private_key", null)
            var address = prefs.getString("address", null)
            if (privateKey == null || address == null) {
                // TODO: generate a real Curve25519 keypair here (KeyPair from
                // com.wireguard.crypto) and register the public key on the
                // server for this user. Never hard-code a shared private key.
                throw IllegalStateException(
                    "Device keypair not provisioned. Generate one per user " +
                        "and register its public key on the server first."
                )
            }

            val configText = Servers.wgQuickConfig(server, privateKey, address)
            val config = Config.parse(ByteArrayInputStream(configText.toByteArray()))

            // Establish the TUN interface through Android's VpnService API.
            val builder = Builder()
                .setSession("Game Fast Booster")
                .addAddress(address.substringBefore("/"), 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
            tunFd = builder.establish()
            backend.setState(tunnelImpl, Tunnel.State.UP, config)
            tunnel = tunnelImpl
        } catch (e: Exception) {
            val intent = Intent(ACTION_STATE).apply {
                putExtra(EXTRA_STATE, "ERROR")
                putExtra(EXTRA_ERROR, e.message ?: "Unknown error")
            }
            sendBroadcast(intent)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun disconnect() {
        try {
            tunnel?.let { backend.setState(it, Tunnel.State.DOWN, null) }
        } catch (_: Exception) {
        }
        tunFd?.close()
        tunFd = null
        tunnel = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(serverName: String): Notification {
        val channelId = "gamefast_vpn"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Game Fast VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Game Fast Booster is ON")
            .setContentText("Connected via $serverName")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.gamefast.booster.CONNECT"
        const val ACTION_DISCONNECT = "com.gamefast.booster.DISCONNECT"
        const val ACTION_STATE = "com.gamefast.booster.STATE"
        const val EXTRA_SERVER_ID = "server_id"
        const val EXTRA_STATE = "state"
        const val EXTRA_ERROR = "error"
        private const val NOTIFICATION_ID = 1001
    }
}
