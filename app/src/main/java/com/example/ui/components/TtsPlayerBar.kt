package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeData
import com.example.util.TtsPlaybackState

@Composable
fun TtsPlayerBar(
    playbackState: TtsPlaybackState,
    currentSentence: String,
    currentSentenceIndex: Int,
    currentSpeed: Float,
    theme: AppThemeData,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .border(1.dp, theme.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Row: Label, Spoken Text snippet, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (playbackState == TtsPlaybackState.PLAYING) Color(0xFF16A34A) else Color.Gray)
                    )
                    Text(
                        text = if (currentSentence.isNotBlank()) "«$currentSentence»" else "محرك القراءة والمراجعة الصوتية (TTS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (playbackState == TtsPlaybackState.PLAYING) theme.primaryColor else Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق مشغل الصوت", modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Controls Row: Speed, Rewind, Play/Pause, Fast-Forward, Sentence Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Selector Dropdown
                Box {
                    AssistChip(
                        onClick = { showSpeedMenu = true },
                        label = { Text("${currentSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = theme.primaryColor)
                    )

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        speeds.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x ${if (speed == 1.0f) "(طبيعي)" else ""}", fontSize = 12.sp) },
                                onClick = {
                                    onSpeedChange(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // Playback Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s / Previous sentence
                    IconButton(
                        onClick = onSeekBackward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "تأخير 10 ثوانٍ", tint = theme.primaryColor)
                    }

                    // Play / Pause FAB
                    FilledIconButton(
                        onClick = {
                            if (playbackState == TtsPlaybackState.PLAYING) onPause() else onPlay()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = theme.primaryColor),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState == TtsPlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState == TtsPlaybackState.PLAYING) "إيقاف مؤقت" else "تشغيل",
                            tint = Color.White
                        )
                    }

                    // Forward 10s / Next sentence
                    IconButton(
                        onClick = onSeekForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "تقديم 10 ثوانٍ", tint = theme.primaryColor)
                    }
                }

                // Sentence counter badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.05f)
                ) {
                    Text(
                        text = "جملة ${currentSentenceIndex + 1}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
