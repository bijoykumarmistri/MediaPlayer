package com.mediaplayer.vault

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultPinDialog(
    securityManager: VaultSecurityManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isPinAlreadySet = remember { securityManager.isPinSet() }

    // 0: Unlock, 1: Setup PIN, 2: Confirm PIN, 3: Security Question Setup, 4: Forgot PIN Recovery, 5: Reset New PIN
    var stage by remember { mutableIntStateOf(if (isPinAlreadySet) 0 else 1) }

    var enteredPin by remember { mutableStateOf("") }
    var firstEnteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(value = false) }

    // Security Question state
    var selectedQuestion by remember { mutableStateOf(VaultSecurityManager.DEFAULT_SECURITY_QUESTIONS.first()) }
    var questionExpanded by remember { mutableStateOf(value = false) }
    var securityAnswerInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf(value = false) }

    fun onNumberClick(num: String) {
        if (enteredPin.length < 4) {
            enteredPin += num
            pinError = false

            if (enteredPin.length == 4) {
                when (stage) {
                    0 -> {
                        // Unlocking existing vault
                        if (securityManager.verifyPin(enteredPin)) {
                            onSuccess()
                        } else {
                            pinError = true
                            enteredPin = ""
                            Toast.makeText(context, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        // Setup step 1: Save first PIN & proceed to confirm
                        firstEnteredPin = enteredPin
                        enteredPin = ""
                        stage = 2
                    }
                    2 -> {
                        // Setup step 2: Verify confirmation matches
                        if (enteredPin == firstEnteredPin) {
                            enteredPin = ""
                            stage = 3 // Move to security question setup
                        } else {
                            pinError = true
                            enteredPin = ""
                            Toast.makeText(context, "PINs did not match. Enter again.", Toast.LENGTH_SHORT).show()
                            stage = 1
                        }
                    }
                    5 -> {
                        // Resetting PIN after recovery
                        firstEnteredPin = enteredPin
                        enteredPin = ""
                        stage = 2 // reuse confirmation step
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            pinError = false
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AudiophileGold.copy(alpha = 0.25f), AudiophileCyan.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, AudiophileGold.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (stage == 0) Icons.Rounded.Lock else Icons.Rounded.Security,
                            contentDescription = null,
                            tint = AudiophileGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when (stage) {
                                0 -> "Privacy Vault"
                                1 -> "Create Vault PIN"
                                2 -> "Confirm Vault PIN"
                                3 -> "Security Question"
                                4 -> "PIN Recovery"
                                5 -> "New Vault PIN"
                                else -> "Privacy Vault"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = when (stage) {
                                0 -> "Enter 4-digit PIN to unlock"
                                1 -> "Set a 4-digit security PIN"
                                2 -> "Re-enter PIN to confirm"
                                3 -> "Required for password recovery"
                                4 -> "Answer your security question"
                                5 -> "Enter your new 4-digit PIN"
                                else -> "Secure local storage"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF9E9EAE)
                        )
                    }
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
            AnimatedContent(
                targetState = stage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "PinStageAnimation"
            ) { currentStage ->
                when (currentStage) {
                    3 -> {
                    // Security Question Setup
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Choose a recovery question in case you forget your PIN:",
                            fontSize = 12.sp,
                            color = Color(0xFFD0D0E0),
                            lineHeight = 16.sp
                        )

                        ExposedDropdownMenuBox(
                            expanded = questionExpanded,
                            onExpandedChange = { questionExpanded = !questionExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedQuestion,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questionExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AudiophileGold,
                                    unfocusedBorderColor = OledBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = questionExpanded,
                                onDismissRequest = { questionExpanded = false },
                                modifier = Modifier.background(Color(0xFF141722))
                            ) {
                                VaultSecurityManager.DEFAULT_SECURITY_QUESTIONS.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q, color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            selectedQuestion = q
                                            questionExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = securityAnswerInput,
                            onValueChange = { securityAnswerInput = it },
                            label = { Text("Your Secret Answer") },
                            placeholder = { Text("e.g. Blue, Rover, etc.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AudiophileCyan,
                                unfocusedBorderColor = OledBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Button(
                            onClick = {
                                if (securityAnswerInput.trim().isNotEmpty()) {
                                    val success = securityManager.setupPin(
                                        pin = firstEnteredPin,
                                        question = selectedQuestion,
                                        answer = securityAnswerInput.trim()
                                    )
                                    if (success) {
                                        Toast.makeText(context, "Privacy Vault configured successfully!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter an answer", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AudiophileGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save & Open Vault", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                4 -> {
                    // Forgot PIN Recovery
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Security Question:\n${securityManager.getSecurityQuestion()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AudiophileGold
                        )

                        OutlinedTextField(
                            value = securityAnswerInput,
                            onValueChange = {
                                securityAnswerInput = it
                                recoveryError = false
                            },
                            label = { Text("Your Answer") },
                            isError = recoveryError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AudiophileCyan,
                                unfocusedBorderColor = OledBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (recoveryError) {
                            Text(
                                text = "Answer does not match. Try again.",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (securityManager.verifySecurityAnswer(securityAnswerInput.trim())) {
                                    enteredPin = ""
                                    securityAnswerInput = ""
                                    stage = 5 // Go to enter new PIN
                                } else {
                                    recoveryError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify Answer", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                stage = 0
                                enteredPin = ""
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Back to PIN entry", color = Color(0xFFA0A0B0), fontSize = 12.sp)
                        }
                    }
                }
                else -> {
                    // Keypad PIN Entry (Stage 0, 1, 2, 5)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 4 PIN Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 14.dp)
                        ) {
                            for (i in 0 until 4) {
                                val filled = i < enteredPin.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pinError) Color(0xFFFF5252)
                                            else if (filled) AudiophileCyan
                                            else Color(0x33FFFFFF)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (pinError) Color(0xFFFF5252)
                                            else if (filled) AudiophileCyan
                                            else Color(0x55FFFFFF),
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Numeric Keypad
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("", "0", "DEL")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (row in keys) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (key in row) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (key.isEmpty()) Color.Transparent
                                                    else Color(0x18FFFFFF)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (key.isEmpty()) Color.Transparent else Color(0x15FFFFFF),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable(enabled = key.isNotEmpty()) {
                                                    if (key == "DEL") onBackspace()
                                                    else onNumberClick(key)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (key == "DEL") {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                                    contentDescription = "Backspace",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else if (key.isNotEmpty()) {
                                                Text(
                                                    text = key,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFamily = FontFamily.SansSerif
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (currentStage == 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    stage = 4
                                    enteredPin = ""
                                }
                            ) {
                                Text(
                                    text = "Forgot PIN?",
                                    color = AudiophileGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    },
    confirmButton = {}
)
}
