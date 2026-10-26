package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppUpdateInfo
import com.example.model.DownloadState
import com.example.model.UpdateFlowStep
import com.example.updater.APKDownloader
import kotlinx.coroutines.launch

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
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedQuestionAnswer by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111216)) // Google Play Dark Mode background
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top App Header matching reference photo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Squircle Logo from screenshot
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_whatsapp_logo),
                        contentDescription = "WhatsApp Messenger Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "WhatsApp\nMessenger",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        modifier = Modifier.semantics {
                            contentDescription = "WhatsApp Messenger"
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = updateInfo.developer,
                        color = Color(0xFF7CD4FD), // Light blue link style
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = updateInfo.inAppPurchases,
                        color = Color(0xFF9AA0A6),
                        fontSize = 12.sp
                    )
                }
            }

            // Buttons Row: Uninstall & Update / Installing / Progress
            when (flowStep) {
                UpdateFlowStep.DOWNLOADING -> {
                    // Downloading State Card in place of action buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E2024))
                            .border(1.dp, Color(0xFF2E3138), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Downloading...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (downloadState is DownloadState.Downloading) {
                                    val downloadedStr = APKDownloader.formatBytes(downloadState.downloadedBytes)
                                    val totalStr = if (downloadState.totalBytes > 0) {
                                        APKDownloader.formatBytes(downloadState.totalBytes)
                                    } else {
                                        updateInfo.formattedSize
                                    }
                                    Text(
                                        text = "$downloadedStr / $totalStr • ${downloadState.speed}",
                                        color = Color(0xFF9AA0A6),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (downloadState is DownloadState.Downloading) {
                                    Text(
                                        text = "${downloadState.percentage}%",
                                        color = Color(0xFFA8C7FA),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("txt_download_percentage")
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = onCancelDownload,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_cancel_update")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel download",
                                        tint = Color(0xFFE2E8F0),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (downloadState is DownloadState.Downloading) {
                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .testTag("progress_indicator_bar"),
                                color = Color(0xFFA8C7FA),
                                trackColor = Color(0xFF303134)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = Color(0xFFA8C7FA),
                                trackColor = Color(0xFF303134)
                            )
                        }
                    }
                }

                UpdateFlowStep.READY_TO_INSTALL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cannot uninstall system package")
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1E2024),
                                contentColor = Color(0xFFE2E8F0)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4B505A))
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_uninstall")
                        ) {
                            Text(
                                text = "Uninstall",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onInstallClick,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669) // Emerald Green for Install
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_action_install")
                        ) {
                            Text(
                                text = "Install",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                else -> {
                    // IDLE state: Exactly matches Screenshot buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("WhatsApp Messenger cannot be uninstalled")
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1E2024),
                                contentColor = Color(0xFFE2E8F0)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4B505A))
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_uninstall")
                        ) {
                            Text(
                                text = "Uninstall",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onUpdateClick,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA8C7FA) // Signature Google Play light blue
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_action_update")
                        ) {
                            Text(
                                text = "Update",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF041E49)
                            )
                        }
                    }
                }
            }

            // Error Display Banner (Shown if download fails)
            AnimatedVisibility(visible = downloadState is DownloadState.Error) {
                if (downloadState is DownloadState.Error) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF3E1212))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update download error",
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = downloadState.message,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRetryDownload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            ),
                            shape = CircleShape,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "Ask Play about this app" Card matching screenshot
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E2024))
                    .border(1.dp, Color(0xFF2E3138), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Ask Play",
                        tint = Color(0xFFA8C7FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask Play about this app",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // "New!" Pill Badge from Screenshot
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF133826))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "New!",
                            color = Color(0xFF81C995),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Box Pill from Screenshot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color(0xFF131418))
                        .clickable {
                            scope.launch {
                                snackbarHostState.showSnackbar("Play AI Search: Ask anything about WhatsApp features")
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Ask a question about this app",
                        color = Color(0xFF8E9196),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Suggestion Questions Row 1 (from screenshot)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF004A77))
                            .clickable {
                                selectedQuestionAnswer = "Go to Settings > Storage and Data > Media upload quality to select Standard or HD."
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "How to change media upload quality?",
                            color = Color(0xFFC2E7FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF004A77))
                            .clickable {
                                selectedQuestionAnswer = "Open WhatsApp on web or secondary device and link via QR code in Linked Devices."
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "How to use companion mode?",
                            color = Color(0xFFC2E7FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Suggestion Questions Row 2 (from screenshot)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF004A77))
                            .clickable {
                                selectedQuestionAnswer = "Navigate to Settings > Privacy to adjust Last Seen, Profile Photo, Groups, and App lock."
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "How to manage chat privacy settings?",
                            color = Color(0xFFC2E7FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF004A77))
                            .clickable {
                                selectedQuestionAnswer = "Channels allow you to follow updates from organizations and people directly in Updates tab."
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "What are WhatsApp Channels?",
                            color = Color(0xFFC2E7FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Answer bubble if user tapped a question
                selectedQuestionAnswer?.let { answer ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF131418))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💡 $answer",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // "What's new" Section matching screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSecurityGuide() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "What's new",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7CD4FD))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF282A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View full changelog",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Last updated ${updateInfo.releaseDate}",
                color = Color(0xFF9AA0A6),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bullet points matching screenshot
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "• You can now choose to share recent group history with new members when you add them to a group",
                    color = Color(0xFFC4C7C5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "• Group calls now include the ability to transfer call...",
                    color = Color(0xFFC4C7C5),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // "Available on more devices" Section matching screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            snackbarHostState.showSnackbar("Available on Phone, Tablet, and Wear OS")
                        }
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available on more devices",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Expand devices",
                    tint = Color(0xFF9AA0A6),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
