package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppUpdateInfo
import com.example.model.DownloadState
import com.example.model.UpdateFlowStep
import com.example.updater.APKDownloader

@Composable
fun TimeForUpdatesScreen(
    updateInfo: AppUpdateInfo,
    flowStep: UpdateFlowStep,
    downloadState: DownloadState,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownload: () -> Unit = {},
    onOpenSecurityGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D13))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Luminous Purple Squircle Mascot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (flowStep == UpdateFlowStep.DOWNLOADING) pulseScale else 1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFC026D3),
                                Color(0xFF8B5CF6),
                                Color(0xFF6366F1)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_octopus_mascot),
                    contentDescription = "App Mascot",
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Main Title
            Text(
                text = updateInfo.changelogTitle,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.4.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle Description
            Text(
                text = updateInfo.changelogDescription,
                color = Color(0xFF94A3B8),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Download Progress Bar & Speed Section (Shown when downloading via OkHttp)
            AnimatedVisibility(visible = flowStep == UpdateFlowStep.DOWNLOADING && downloadState is DownloadState.Downloading) {
                if (downloadState is DownloadState.Downloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF161922))
                            .border(1.dp, Color(0xFF282E3E), RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading update package...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${downloadState.percentage}%",
                                color = Color(0xFFA855F7),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("txt_download_percentage")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .testTag("progress_indicator_bar"),
                            color = Color(0xFFA855F7),
                            trackColor = Color(0xFF242A38)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Speed: ${downloadState.speed}",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val downloadedStr = APKDownloader.formatBytes(downloadState.downloadedBytes)
                            val totalStr = if (downloadState.totalBytes > 0) {
                                APKDownloader.formatBytes(downloadState.totalBytes)
                            } else {
                                updateInfo.formattedSize
                            }
                            Text(
                                text = "$downloadedStr / $totalStr",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "URL: ${updateInfo.downloadUrl}",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Error Display Banner (Shown if download fails)
            AnimatedVisibility(visible = downloadState is DownloadState.Error) {
                if (downloadState is DownloadState.Error) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF450A0A).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFFDC2626), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " Download Failed",
                                color = Color(0xFFF87171),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = downloadState.message,
                            color = Color(0xFFFECACA),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRetryDownload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("btn_retry_download")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = " Retry Download",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Download Complete Notice (Shown when ready to install)
            AnimatedVisibility(visible = flowStep == UpdateFlowStep.READY_TO_INSTALL) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF064E3B).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFF059669), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Update downloaded successfully (100%)! Tap INSTALL to complete.",
                            color = Color(0xFF34D399),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onOpenSecurityGuide,
                        modifier = Modifier.testTag("btn_security_tips_ready")
                    ) {
                        Text(
                            text = "🛡️ Play Protect বা রেস্ট্রিক্টেড সেটিং সমাধান গাইড",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Top-right Security Help Button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            TextButton(
                onClick = onOpenSecurityGuide,
                modifier = Modifier.testTag("btn_top_security_help")
            ) {
                Text(
                    text = "🛡️ নিরাপত্তা সহায়তা",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Action Button (UPDATE / INSTALL / CANCEL)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 38.dp)
        ) {
            when (flowStep) {
                UpdateFlowStep.DOWNLOADING -> {
                    OutlinedButton(
                        onClick = onCancelDownload,
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_cancel_update")
                    ) {
                        Text(
                            text = "CANCEL",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                UpdateFlowStep.READY_TO_INSTALL -> {
                    Button(
                        onClick = onInstallClick,
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669) // Emerald Green
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_action_install")
                    ) {
                        Text(
                            text = "INSTALL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                else -> {
                    Button(
                        onClick = onUpdateClick,
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A4658)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .testTag("btn_action_update")
                    ) {
                        Text(
                            text = "UPDATE",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
