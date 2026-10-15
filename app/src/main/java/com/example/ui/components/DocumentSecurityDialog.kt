package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.local.entity.DocumentEntity
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanGold
import com.example.ui.viewmodel.ScanFlowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUnlockModal(
    document: DocumentEntity,
    viewModel: ScanFlowViewModel,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isBiometricEnabled = viewModel.securityEngine.isBiometricEnabled()
    val isBiometricAvailable = viewModel.biometricManager.isBiometricSupported()

    // Auto-prompt biometric prompt on opening if enabled
    LaunchedEffect(document.id) {
        if (isBiometricEnabled && activity != null && isBiometricAvailable) {
            viewModel.authenticateDocumentWithBiometrics(
                activity = activity,
                doc = document,
                onSuccess = {
                    onUnlocked()
                },
                onPinFallback = {
                    // User selected PIN fallback on BiometricPrompt
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Surface(
                shape = CircleShape,
                color = ScanGold.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Kilitli Belge",
                        tint = ScanGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Korumalı Hassas Belge",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "\"${document.title}\"",
                    fontSize = 14.sp,
                    color = ScanGold,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isError) "Hatalı PIN Kodu!" else errorMessage ?: "Açmak için biyometrik doğrulama yapın veya PIN girin",
                    fontSize = 12.sp,
                    color = if (isError) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Biometric Trigger Button
            if (activity != null && isBiometricEnabled && isBiometricAvailable) {
                Button(
                    onClick = {
                        errorMessage = null
                        isError = false
                        viewModel.authenticateDocumentWithBiometrics(
                            activity = activity,
                            doc = document,
                            onSuccess = {
                                onUnlocked()
                            },
                            onError = { err ->
                                errorMessage = err
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Parmak İzi / Biyometrik ile Aç",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Text("veya PIN girin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }

            // PIN Field
            OutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    if (it.length <= 4) {
                        enteredPin = it
                        isError = false
                        if (it.length == 4) {
                            if (viewModel.verifyAndUnlockDocument(document, it)) {
                                onUnlocked()
                            } else {
                                isError = true
                            }
                        }
                    }
                },
                label = { Text("4 Haneli Güvenlik PIN") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = ScanGold) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Submit Button
            Button(
                onClick = {
                    if (viewModel.verifyAndUnlockDocument(document, enteredPin)) {
                        onUnlocked()
                    } else {
                        isError = true
                        Toast.makeText(context, "Hatalı PIN kodu!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PIN ile Doğrula ve Aç", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DocumentLockSetupDialog(
    document: DocumentEntity,
    viewModel: ScanFlowViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf(document.lockPin ?: "") }
    var useMasterPin by remember { mutableStateOf(document.lockPin.isNullOrBlank()) }
    val isBiometricAvailable = viewModel.biometricManager.isBiometricSupported()
    val isBiometricEnabled = viewModel.securityEngine.isBiometricEnabled()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (document.isLocked) Icons.Default.LockOpen else Icons.Default.Shield,
                    contentDescription = null,
                    tint = ScanGold
                )
                Text(
                    text = if (document.isLocked) "Belge Korumasını Kaldır" else "Hassas Belge Koruması",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (document.isLocked) {
                    Text("Bu belgenin Biyometrik (Parmak İzi/FaceID) ve PIN korumasını kaldırmak istiyor musunuz? Belge doğrudan açılabilir hale gelecektir.")
                } else {
                    Text("Bu belgeyi korumalı kasaya ekleyin. Açılışta Android Biyometrik Doğrulama (Parmak İzi / FaceID) veya PIN istenecektir.")

                    if (isBiometricAvailable) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ScanCyan.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, ScanCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = ScanCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isBiometricEnabled) "Android Biyometrik Doğrulama (BiometricPrompt) Aktif" else "Biyometrik Kilit Ayarlardan Etkinleştirilebilir",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ana Kasa PIN'ini Kullan", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = useMasterPin,
                                    onCheckedChange = { useMasterPin = it }
                                )
                            }

                            if (!useMasterPin) {
                                OutlinedTextField(
                                    value = pinText,
                                    onValueChange = { if (it.length <= 4) pinText = it },
                                    label = { Text("Özel 4 Haneli PIN") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (document.isLocked) {
                        viewModel.toggleDocumentLock(document.id, null)
                        viewModel.lockDocumentSession(document.id)
                        Toast.makeText(context, "Belge koruması kaldırıldı", Toast.LENGTH_SHORT).show()
                    } else {
                        val pinToSet = if (useMasterPin) null else pinText.ifBlank { null }
                        viewModel.toggleDocumentLock(document.id, pinToSet)
                        Toast.makeText(context, "Hassas belge biyometrik ve PIN korumasına alındı", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (document.isLocked) Color(0xFFEF4444) else ScanBlue)
            ) {
                Text(if (document.isLocked) "Korumayı Kaldır" else "Koru ve Kilitle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
