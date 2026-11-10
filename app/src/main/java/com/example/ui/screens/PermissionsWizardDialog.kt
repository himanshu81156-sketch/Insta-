package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.InstaCoral

data class HindiPermissionStep(
    val title: String,
    val reason: String,
    val fallbackTip: String,
    val manifestPermission: String? = null,
    val settingsAction: String? = null
)

@Composable
fun PermissionsWizardDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val steps = remember {
        listOf(
            HindiPermissionStep(
                title = "Camera Permission",
                reason = "Photo aur Video banane ke liye",
                fallbackTip = "Koi baat nahi! Aap Gallery (Photo Picker) se photo/video upload kar sakte ho.",
                manifestPermission = Manifest.permission.CAMERA
            ),
            HindiPermissionStep(
                title = "Microphone Permission",
                reason = "Reels me awaaz aur Voice Message ke liye",
                fallbackTip = "Koi baat nahi! Aap bina awaaz ke Reels aur Text/Photo Chat bhej sakte ho.",
                manifestPermission = Manifest.permission.RECORD_AUDIO
            ),
            HindiPermissionStep(
                title = "Storage / Photos & Videos Permission",
                reason = "Photo Video save aur upload karne ke liye",
                fallbackTip = "Koi baat nahi! Android ka surakshit Photo Picker bina permission ke kaam karta hai."
            ),
            HindiPermissionStep(
                title = "Notifications Permission",
                reason = "Like, Comment, Follow, Message ka batane ke liye",
                fallbackTip = "Koi baat nahi! Aap app ke andar Notifications icon par saare updates dekh sakte ho.",
                manifestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null
            ),
            HindiPermissionStep(
                title = "Location Permission",
                reason = "Post pe Location lagane aur aas-paas ke dost dhoondhne ke liye",
                fallbackTip = "Koi baat nahi! Aap Post banate waqt apna shehar khud type kar sakte ho.",
                manifestPermission = Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            HindiPermissionStep(
                title = "Contacts Permission",
                reason = "Apne Contacts se dost dhoondhne aur Follow karne ke liye",
                fallbackTip = "Koi baat nahi! Aap Search Tab me username likh kar kisi ko bhi Follow kar sakte ho.",
                manifestPermission = Manifest.permission.READ_CONTACTS
            ),
            HindiPermissionStep(
                title = "Accessibility Permission",
                reason = "App ko aasani se chalane aur dusre app se Post Share karne ke liye",
                fallbackTip = "Koi baat nahi! App ke saare buttons bade aur saaf hain, bina iske bhi aasani se chalega.",
                settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
            ),
            HindiPermissionStep(
                title = "Display over other apps Permission",
                reason = "Kisi bhi app ke upar Notification aur Chat Bubble dikhane ke liye",
                fallbackTip = "Koi baat nahi! Aapko saare messages Chat Tab me turant milenge.",
                settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            ),
            HindiPermissionStep(
                title = "Internet / Online Permission",
                reason = "Online Post, Follow, Chat ke liye",
                fallbackTip = "Internet pehle se on hai — aapka data surakshit tarike se online save hoga."
            ),
            HindiPermissionStep(
                title = "Phone State Permission",
                reason = "Call aane pe App ko rokne ke liye",
                fallbackTip = "Koi baat nahi! Call aane par aap khud Reel pause kar sakte ho.",
                manifestPermission = Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var declinedTip by remember { mutableStateOf<String?>(null) }
    val step = steps[currentIndex]

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            declinedTip = null
            if (currentIndex < steps.lastIndex) currentIndex++ else onDismiss()
        } else {
            declinedTip = step.fallbackTip
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${currentIndex + 1}/${steps.size} • ${step.title}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / steps.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = InstaCoral
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(step.reason, style = MaterialTheme.typography.titleMedium)
                Text("Du permission? Haan / Nahi", style = MaterialTheme.typography.labelLarge, color = InstaCoral)
                if (declinedTip != null) {
                    Text(
                        text = declinedTip!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (declinedTip == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        declinedTip = step.fallbackTip
                    }) {
                        Text("Nahi")
                    }
                    Button(
                        onClick = {
                            when {
                                step.manifestPermission != null -> {
                                    runtimeLauncher.launch(step.manifestPermission)
                                }
                                step.settingsAction != null -> {
                                    try {
                                        val intent = Intent(step.settingsAction).apply {
                                            if (step.settingsAction == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                    if (currentIndex < steps.lastIndex) currentIndex++ else onDismiss()
                                }
                                else -> {
                                    if (currentIndex < steps.lastIndex) currentIndex++ else onDismiss()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InstaCoral)
                    ) {
                        Text("Haan")
                    }
                }
            } else {
                Button(
                    onClick = {
                        declinedTip = null
                        if (currentIndex < steps.lastIndex) currentIndex++ else onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InstaCoral)
                ) {
                    Text(if (currentIndex < steps.lastIndex) "Aage Badhein" else "Samapt Karein")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Baad me")
            }
        }
    )
}
