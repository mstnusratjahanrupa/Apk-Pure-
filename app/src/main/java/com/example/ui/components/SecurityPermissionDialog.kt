package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPermissionDialog(
    appName: String,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_security_unknown_apps")
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White, // Exact clean white dialog from Screenshot 3
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header with Mascot Squircle & App Name (Screenshot 3)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_whatsapp_logo),
                            contentDescription = appName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = appName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Exact text from Screenshot 3
                Text(
                    text = "For your security, your phone currently isn\'t allowed to install unknown apps from this source. You can change this in Settings.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Cancel | Settings (Screenshot 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_dialog_cancel")
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = onGoToSettings,
                        modifier = Modifier.testTag("btn_dialog_settings")
                    ) {
                        Text(
                            text = "Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669) // Green accent matching screenshot 3
                        )
                    }
                }
            }
        }
    }
}
