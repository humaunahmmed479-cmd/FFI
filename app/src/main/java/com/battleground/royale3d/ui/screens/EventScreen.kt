package com.battleground.royale3d.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.battleground.royale3d.data.L10n
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.ui.theme.BlazeAmber
import com.battleground.royale3d.ui.theme.CyberCyan
import com.battleground.royale3d.ui.theme.GunmetalElevated
import com.battleground.royale3d.ui.theme.GunmetalSurface
import com.battleground.royale3d.ui.theme.ObsidianBg
import com.battleground.royale3d.ui.theme.SafeEmerald
import com.battleground.royale3d.ui.theme.TacticalBorder
import com.battleground.royale3d.viewmodel.GameViewModel

private data class EventReward(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
    val tag: String,
    val armoryId: String
)

private val eventRewards = listOf(
    EventReward(
        id = "awm",
        titleEn = "AWM High-Texture Sniper",
        titleBn = "AWM স্নাইপার রাইফেল",
        descEn = "Claim & save to Vault • 110 DMG • 8x Scope",
        descBn = "ক্লেইম করে ভল্টে সেভ করুন • ১১০ ক্ষতি",
        tag = "WEAPON EVENT",
        armoryId = "awm"
    ),
    EventReward(
        id = "mp40",
        titleEn = "MP40 Cobra Evo",
        titleBn = "MP40 কোবরা ইভো",
        descEn = "Claim & save to Vault • Rapid Close-Combat SMG",
        descBn = "ক্লেইম করে ভল্টে সেভ করুন • দ্রুত ফায়ার",
        tag = "EVO EVENT",
        armoryId = "mp40"
    ),
    EventReward(
        id = "ff_gun",
        titleEn = "FF Tactical Rifle",
        titleBn = "এফএফ অ্যাসল্ট রাইফেল",
        descEn = "Standard issue Battle Royale Assault Rifle",
        descBn = "মাঝারি দূরত্বের ব্যালেন্সড রাইফেল",
        tag = "STARTER",
        armoryId = "ff_gun"
    ),
    EventReward(
        id = "skin_0",
        titleEn = "Championship Jersey #1",
        titleBn = "চ্যাম্পিয়নশিপ জার্সি #১",
        descEn = "Main Player Skin • Saved in Vault",
        descBn = "আসল প্লেয়ার স্কিন • ভল্টে সেভ থাকবে",
        tag = "PLAYER SKIN",
        armoryId = "jersey"
    ),
    EventReward(
        id = "skin_1",
        titleEn = "Rare Striker Jersey #2",
        titleBn = "রেয়ার স্ট্রাইকার জার্সি #২",
        descEn = "Unlockable Rare Jersey variant for Vault",
        descBn = "ভল্টে সেভ করার জন্য রেয়ার জার্সি স্কিন",
        tag = "RARE JERSEY",
        armoryId = "jersey"
    ),
    EventReward(
        id = "skin_2",
        titleEn = "Bengal Legend Jersey #3",
        titleBn = "বেঙ্গল লিজেন্ড জার্সি #৩",
        descEn = "Unlockable Rare Jersey variant for Vault",
        descBn = "ভল্টে সেভ করার জন্য রেয়ার জার্সি স্কিন",
        tag = "RARE JERSEY",
        armoryId = "jersey"
    )
)

@Composable
fun EventScreen(
    viewModel: GameViewModel,
    settings: GameSettings,
    onOpenVaultItem: (String) -> Unit,
    onBack: () -> Unit
) {
    val lang = settings.language

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GunmetalSurface)
                        .testTag("event_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = L10n.pick(lang, "WEAPON & SKIN EVENTS", "গান ও স্কিন ইভেন্ট"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = BlazeAmber
                    )
                    Text(
                        text = L10n.pick(
                            lang,
                            "Claim weapons & skins here — all unlocked items save directly to your Vault.",
                            "এখান থেকে অস্ত্র ও স্কিন ক্লেইম করুন — সব আইটেম সরাসরি ভল্টে সেভ হবে।"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 260.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(eventRewards, key = { it.id }) { reward ->
                val isUnlocked = reward.id in settings.vaultUnlockedIds || reward.id == "skin_0"
                val isEquipped = (reward.id.startsWith("skin_") &&
                    reward.id.removePrefix("skin_").toIntOrNull() == settings.equippedJerseyVariant) ||
                    reward.id == settings.equippedWeaponId

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            if (isUnlocked) SafeEmerald else BlazeAmber,
                            RoundedCornerShape(14.dp)
                        ),
                    color = GunmetalSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = reward.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GunmetalElevated)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            if (isUnlocked) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "In Vault",
                                        tint = SafeEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = L10n.pick(lang, "SAVED IN VAULT", "ভল্টে সেভ আছে"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafeEmerald
                                    )
                                }
                            }
                        }

                        Text(
                            text = L10n.pick(lang, reward.titleEn, reward.titleBn),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = L10n.pick(lang, reward.descEn, reward.descBn),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isUnlocked) {
                                Button(
                                    onClick = { viewModel.claimToVault(reward.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlazeAmber,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("claim_${reward.id}")
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(L10n.pick(lang, "CLAIM TO VAULT", "ভল্টে নিন"), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.equipFromVault(reward.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEquipped) GunmetalElevated else SafeEmerald,
                                        contentColor = if (isEquipped) SafeEmerald else ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("equip_${reward.id}")
                                ) {
                                    Text(
                                        text = if (isEquipped) {
                                            L10n.pick(lang, "EQUIPPED", "ইকুইপ করা")
                                        } else {
                                            L10n.pick(lang, "EQUIP", "ইকুইপ করুন")
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onOpenVaultItem(reward.armoryId) },
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(L10n.pick(lang, "3D VIEW", "৩ডি দেখুন"))
                            }
                        }
                    }
                }
            }
        }
    }
}
