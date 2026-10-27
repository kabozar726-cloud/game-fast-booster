package com.gamefast.booster

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Game Fast Booster — main screen.
 *
 * What this screen honestly does and does not do:
 * - Connect really turns the phone's VPN ON (Android system VPN dialog
 *   appears on first connect) once a real server is configured.
 * - "Network test" measures HTTP latency in this app — it is labelled
 *   as a network test, NOT as PUBG's in-game ms.
 * - Before/After ms numbers come only from the player typing what
 *   PUBG's own lobby shows. The app never invents them.
 */
class MainActivity : ComponentActivity() {

    private var vpnState by mutableStateOf("OFF")
    private var vpnError by mutableStateOf("")
    private var selectedServer by mutableStateOf(Servers.all.first())

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startVpn(selectedServer)
            }
        }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GameVpnService.ACTION_STATE) {
                vpnState = intent.getStringExtra(GameVpnService.EXTRA_STATE) ?: "OFF"
                vpnError = intent.getStringExtra(GameVpnService.EXTRA_ERROR) ?: ""
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(GameVpnService.ACTION_STATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BoosterScreen(
                        vpnState = vpnState,
                        vpnError = vpnError,
                        selectedServer = selectedServer,
                        onServerSelected = { selectedServer = it },
                        onConnect = { requestConnect() },
                        onDisconnect = { stopVpn() },
                        context = this
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(stateReceiver)
        super.onDestroy()
    }

    private fun requestConnect() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            startVpn(selectedServer)
        }
    }

    private fun startVpn(server: VpnServer) {
        val intent = Intent(this, GameVpnService::class.java).apply {
            action = GameVpnService.ACTION_CONNECT
            putExtra(GameVpnService.EXTRA_SERVER_ID, server.id)
        }
        startService(intent)
    }

    private fun stopVpn() {
        val intent = Intent(this, GameVpnService::class.java).apply {
            action = GameVpnService.ACTION_DISCONNECT
        }
        startService(intent)
    }
}

@Composable
private fun BoosterScreen(
    vpnState: String,
    vpnError: String,
    selectedServer: VpnServer,
    onServerSelected: (VpnServer) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    context: Context,
) {
    var netTest by remember { mutableStateOf("Not tested yet") }
    var beforeMs by remember { mutableStateOf("") }
    var afterMs by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(BoostLogStore.load(context)) }

    fun refreshEntries() {
        entries = BoostLogStore.load(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Game Fast Booster", style = MaterialTheme.typography.headlineMedium)
            Text(
                "VPN: $vpnState  •  Server: ${selectedServer.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (vpnError.isNotEmpty()) {
                Text(
                    "Problem: $vpnError",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Honest note: no app can guarantee 20 ms. Real ping depends " +
                    "on your internet, your distance to the game server, and " +
                    "the network route. This app removes phone-side slowdowns " +
                    "and routes you through the fastest server it can.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        item {
            Text("1. Pick a server region", style = MaterialTheme.typography.titleMedium)
        }
        items(Servers.all) { server ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(server.name, style = MaterialTheme.typography.titleSmall)
                    Text(server.region, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { onServerSelected(server) }) {
                        Text(if (server.id == selectedServer.id) "Selected" else "Use this server")
                    }
                }
            }
        }

        item {
            Text("2. Turn the VPN ON", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect) { Text("Connect (ON)") }
                OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
            }
            Text(
                "First connect shows Android's system VPN permission dialog. " +
                    "That dialog comes from Android itself — no app can skip it.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        item {
            Text("3. Network test (not in-game ms)", style = MaterialTheme.typography.titleMedium)
            Text(netTest, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                netTest = "Testing…"
                thread {
                    val ms = measureHttpLatency()
                    netTest = if (ms >= 0) {
                        "Network latency: $ms ms (HTTP test in this app — " +
                            "PUBG's lobby ms is shown inside the game)"
                    } else {
                        "Test failed — no network. No number is shown " +
                            "rather than a guessed one."
                    }
                }
            }) { Text("Run network test") }
        }

        item {
            Text("4. Your real PUBG ms — before / after", style = MaterialTheme.typography.titleMedium)
            Text(
                "Type exactly what PUBG's lobby shows, before boosting and " +
                    "after boosting. Only your own numbers are saved.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = beforeMs,
                onValueChange = { beforeMs = it.filter(Char::isDigit) },
                label = { Text("Before ms (from PUBG lobby)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = afterMs,
                onValueChange = { afterMs = it.filter(Char::isDigit) },
                label = { Text("After ms (from PUBG lobby)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val b = beforeMs.toIntOrNull()
                val a = afterMs.toIntOrNull()
                if (b != null && a != null) {
                    BoostLogStore.add(
                        context,
                        BoostEntry(
                            id = System.currentTimeMillis(),
                            beforeMs = b,
                            afterMs = a,
                            region = selectedServer.name,
                            note = "",
                            time = System.currentTimeMillis()
                        )
                    )
                    beforeMs = ""
                    afterMs = ""
                    refreshEntries()
                }
            }) { Text("Save entry") }
        }

        items(entries) { e ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${e.beforeMs} → ${e.afterMs} ms  (${e.afterMs - e.beforeMs} ms change)")
                    Text(e.region, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        BoostLogStore.delete(context, e.id)
                        refreshEntries()
                    }) { Text("Delete") }
                }
            }
        }
    }
}

/** Simple HTTP latency probe. Returns -1 on failure (never a guessed number). */
private fun measureHttpLatency(): Long {
    return try {
        val start = System.currentTimeMillis()
        val conn = URL("https://www.google.com/generate_204")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"
        conn.responseCode
        conn.disconnect()
        System.currentTimeMillis() - start
    } catch (_: Exception) {
        -1
    }
}
