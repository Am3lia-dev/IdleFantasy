package com.fantasyidler.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fantasyidler.R
import com.fantasyidler.data.model.Skills
import com.fantasyidler.simulator.XpTable
import com.fantasyidler.util.GameStrings

@Composable
fun LampSkillDialog(
    skillLevels: Map<String, Int>,
    skillXp: Map<String, Long>,
    sessionXpGain: Long,
    onSkillSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.slayer_lamp_pick_skill)) },
        text = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Skills.ALL.forEach { skillKey ->
                    val level = skillLevels[skillKey] ?: 1
                    val xp    = skillXp[skillKey] ?: 0L
                    val name  = GameStrings.skillName(context, skillKey)
                    Surface(
                        onClick  = { onSkillSelected(skillKey) },
                        shape    = RoundedCornerShape(8.dp),
                        color    = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier            = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = stringResource(R.string.slayer_level_label, level),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val endXp = xp + sessionXpGain
                                val isOverMax = sessionXpGain > 0L && endXp > XpTable.xpForLevel(99)
                                val xpLineText = remember(skillKey, skillXp, sessionXpGain) {
                                    if (sessionXpGain <= 0L) null
                                    else {
                                        val levelBefore = XpTable.levelForXp(xp)
                                        val levelAfter = XpTable.levelForXp(endXp)
                                        val levelGain = levelAfter - levelBefore
                                        val pct = (XpTable.progressFraction(endXp) * 100).toInt()
                                        buildString {
                                            append("→  Lv $levelAfter")
                                            if (levelGain > 0) append(" (+$levelGain, $pct%)")
                                            else append(" ($pct%)")
                                        }
                                    }
                                }
                                if (isOverMax) {
                                    Text(
                                        text = stringResource(R.string.slayer_lamp_max_level_warning),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                } else {
                                    Spacer(Modifier)
                                }

                                if (xpLineText != null) {
                                    Text(
                                        text = xpLineText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}
