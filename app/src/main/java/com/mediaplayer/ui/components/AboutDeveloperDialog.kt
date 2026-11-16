package com.mediaplayer.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurfaceVariant

const val DEVELOPER_NAME = "Bijoy"
const val DEVELOPER_UPI_ID = "bijoymistri367@okaxis"

@Composable
fun AboutDeveloperDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isUpiCopied by remember { mutableStateOf(false) }

    fun copyUpiId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Developer UPI ID", DEVELOPER_UPI_ID)
        clipboard.setPrimaryClip(clip)
        isUpiCopied = true
        Toast.makeText(context, "UPI ID copied: $DEVELOPER_UPI_ID", Toast.LENGTH_SHORT).show()
    }

    fun openUpiPayment() {
        try {
            val uri = Uri.parse("upi://pay?pa=$DEVELOPER_UPI_ID&pn=$DEVELOPER_NAME&tn=Support%20Media%20Player%20Development&cu=INR")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (e: Exception) {
            copyUpiId()
            Toast.makeText(context, "No UPI app detected. UPI ID copied to clipboard!", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceVariant,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = AudiophileGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About & Support",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9E9EAE)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Developer Profile Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AudiophileCyan, AudiophileGold)
                            )
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F1017)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = AudiophileCyan,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = DEVELOPER_NAME,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Lead Developer & System Architect",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AudiophileCyan,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Media Player • v1.0.0 (Audiophile Edition)",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E9E),
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Support & Donation Card (UPI)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x24FFD54F)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x66FFD54F))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payment,
                                contentDescription = null,
                                tint = AudiophileGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Support & Donation ☕",
                                fontWeight = FontWeight.Bold,
                                color = AudiophileGold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "If you love the bit-perfect audio and cinema playback experience, support future development with a small donation via UPI!",
                            fontSize = 12.sp,
                            color = Color(0xFFE0E0E0),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // UPI ID Badge & Copy Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33000000))
                                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "UPI ID",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA0A0B0)
                                    )
                                    Text(
                                        text = DEVELOPER_UPI_ID,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                IconButton(
                                    onClick = { copyUpiId() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isUpiCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy UPI ID",
                                        tint = if (isUpiCopied) AudiophileGreen else AudiophileGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pay via UPI Button
                        Button(
                            onClick = { openUpiPayment() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AudiophileGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pay via UPI App (GPay / PhonePe / Paytm)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Features & Tech Architecture Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F12131A)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CORE SYSTEM CAPABILITIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AudiophileCyan,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(8.dp))

                        FeatureItem("Bit-Perfect USB-C Audio", "Bypasses Android 14+ mixer and DSP for pure bitstream.")
                        FeatureItem("Universal Jellyfin FFmpeg", "Native decoding for MKV, FLAC, DSD, DTS, TrueHD & AC3.")
                        FeatureItem("Cinematic Video Engine", "Auto-rotation sensor lock, PiP, gestures & subtitle tracks.")
                        FeatureItem("100% Jetpack Compose & M3", "OLED true-dark canvas with dynamic palette accents.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DeveloperSupportCard(
    onOpenFullDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isUpiCopied by remember { mutableStateOf(false) }

    fun copyUpiId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Developer UPI ID", DEVELOPER_UPI_ID)
        clipboard.setPrimaryClip(clip)
        isUpiCopied = true
        Toast.makeText(context, "UPI ID copied: $DEVELOPER_UPI_ID", Toast.LENGTH_SHORT).show()
    }

    fun openUpiPayment() {
        try {
            val uri = Uri.parse("upi://pay?pa=$DEVELOPER_UPI_ID&pn=$DEVELOPER_NAME&tn=Support%20Media%20Player%20Development&cu=INR")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyUpiId()
            Toast.makeText(context, "No UPI app detected. UPI ID copied to clipboard!", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AudiophileCyan, AudiophileGold)
                                )
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F1017)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = AudiophileCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Developer: $DEVELOPER_NAME",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Lead Developer & System Architect",
                            fontSize = 11.sp,
                            color = AudiophileCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onOpenFullDialog) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "About",
                        tint = AudiophileGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0x22FFFFFF))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Support Ongoing Development ☕",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AudiophileGold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Media Player is ad-free and open for audiophile & cinema lovers. If you enjoy using it, support me with a donation via UPI!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0C0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // UPI ID Pill Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, Color(0x33FFD54F), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPI ID",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA0A0B0)
                        )
                        Text(
                            text = DEVELOPER_UPI_ID,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { copyUpiId() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isUpiCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            tint = if (isUpiCopied) AudiophileGreen else AudiophileGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { openUpiPayment() },
                    colors = ButtonDefaults.buttonColors(containerColor = AudiophileGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payment,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Donate via UPI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenFullDialog,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AudiophileCyan),
                    border = BorderStroke(1.dp, AudiophileCyan)
                ) {
                    Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(AudiophileCyan)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF9E9EAE), lineHeight = 14.sp)
        }
    }
}
