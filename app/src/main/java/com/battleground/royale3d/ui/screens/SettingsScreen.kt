package com.battleground.royale3d.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.battleground.royale3d.data.L10n
import com.battleground.royale3d.model.AppLanguage
import com.battleground.royale3d.model.CameraPerspective
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.model.GraphicsQuality
import com.battleground.royale3d.model.WsConnectionState
import com.battleground.royale3d.ui.theme.BlazeAmber
import com.battleground.royale3d.ui.theme.CrimsonPulse
import com.battleground.royale3d.ui.theme.CyberCyan
import com.battleground.royale3d.ui.theme.GunmetalElevated
import com.battleground.royale3d.ui.theme.GunmetalSurface
import com.battleground.royale3d.ui.theme.ObsidianBg
import com.battleground.royale3d.ui.theme.SafeEmerald
import com.battleground.royale3d.ui.theme.TacticalBorder
import com.battleground.royale3d.viewmodel.GameViewModel

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    settings: GameSettings,
    onBack: () -> Unit
) {
    val lang = settings.language
    val wsState by viewModel.wsConnectionState.collectAsStateWithLifecycle()
    val wsPing by viewModel.wsPingMs.collectAsStateWithLifecycle()
    var wsInput by remember(settings.wsServerUrl) { mutableStateOf(settings.wsServerUrl) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GunmetalSurface)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = L10n.pick(lang, "SETTINGS", "সেটিংস"),
                style = MaterialTheme.typography.headlineMedium,
                color = BlazeAmber
            )
        }

        // Language Section
        SettingsCard(title = L10n.pick(lang, "Language / ভাষা", "ভাষা / Language")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { option ->
                    val selected = settings.language == option
                    Button(
                        onClick = { viewModel.setLanguage(option) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) BlazeAmber else GunmetalElevated,
                            contentColor = if (selected) ObsidianBg else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(if (option == AppLanguage.BENGALI) "বাংলা" else "English")
                    }
                }
            }
        }

        // Controls & Camera
        SettingsCard(title = L10n.pick(lang, "Camera & Look Sensitivity", "ক্যামেরা ও সেনসিটিভিটি")) {
            Text(
                text = L10n.pick(
                    lang,
                    "Look Sensitivity: ${"%.1f".format(settings.lookSensitivity)}x",
                    "লুক সেনসিটিভিটি: ${"%.1f".format(settings.lookSensitivity)}x"
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = settings.lookSensitivity,
                onValueChange = { viewModel.setSensitivity(it) },
                valueRange = 0.4f..2.2f,
                colors = SliderDefaults.colors(
                    thumbColor = BlazeAmber,
                    activeTrackColor = BlazeAmber
                ),
                modifier = Modifier.testTag("sensitivity_slider")
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraPerspective.entries.forEach { p ->
                    val selected = settings.defaultPerspective == p
                    Button(
                        onClick = { viewModel.setDefaultPerspective(p) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) CyberCyan else GunmetalElevated,
                            contentColor = if (selected) ObsidianBg else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(if (p == CameraPerspective.THIRD_PERSON) "TPP" else "FPP")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.pick(lang, "Tactile Haptics", "হ্যাপটিক ফিডব্যাক"),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { viewModel.setHaptics(it) }
                )
            }
        }

        // Graphics LOD
        SettingsCard(title = L10n.pick(lang, "3D Graphics & LOD Culling", "গ্রাফিক্স ও লেভেল-অফ-ডিটেইল (LOD)")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphicsQuality.entries.forEach { q ->
                    val selected = settings.graphicsQuality == q
                    Button(
                        onClick = { viewModel.setGraphicsQuality(q) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) SafeEmerald else GunmetalElevated,
                            contentColor = if (selected) ObsidianBg else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(q.name)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = L10n.pick(
                    lang,
                    "LOD culls distant GLB models automatically for steady FPS on mobile.",
                    "দূরের মডেলগুলো অটো-অপ্টিমাইজ করে মোবাইলে স্মুথ FPS ধরে রাখে।"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // WebSocket Multiplayer Config
        SettingsCard(title = L10n.pick(lang, "WebSocket Multiplayer Server", "ওয়েবসকেট মাল্টিপ্লেয়ার সার্ভার")) {
            OutlinedTextField(
                value = wsInput,
                onValueChange = {
                    wsInput = it
                    viewModel.setWsServerUrl(it)
                },
                label = { Text("ws:// or wss:// Server URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ws_url_input")
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (wsState) {
                    WsConnectionState.CONNECTED -> SafeEmerald
                    WsConnectionState.CONNECTING -> BlazeAmber
                    WsConnectionState.DISCONNECTED_FALLBACK -> CrimsonPulse
                    WsConnectionState.LOCAL_SIMULATION -> CyberCyan
                }
                val statusLabel = when (wsState) {
                    WsConnectionState.CONNECTED -> "CONNECTED (${wsPing ?: "--"}ms)"
                    WsConnectionState.CONNECTING -> "CONNECTING..."
                    WsConnectionState.DISCONNECTED_FALLBACK -> "FALLBACK: 100-BOT ARENA"
                    WsConnectionState.LOCAL_SIMULATION -> "LOCAL 100-BOT ARENA"
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
                Button(
                    onClick = { viewModel.testWebSocketConnection(wsInput) },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("ws_test_button")
                ) {
                    Text(L10n.pick(lang, "Connect", "কানেক্ট"))
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp)),
        color = GunmetalSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BlazeAmber)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
