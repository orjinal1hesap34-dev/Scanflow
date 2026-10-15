package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AuthModalBottomSheet
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanEmerald
import com.example.ui.theme.ScanGold
import com.example.ui.viewmodel.ScanFlowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScanFlowViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    var showAuthSheet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showLangMenu by remember { mutableStateOf(false) }
    var showAutoLockMenu by remember { mutableStateOf(false) }

    var biometricEnabled by remember { mutableStateOf(viewModel.securityEngine.isBiometricEnabled()) }
    var sensitiveDocEnabled by remember { mutableStateOf(viewModel.securityEngine.isSensitiveDocProtectionEnabled()) }
    var autoLockMinutes by remember { mutableStateOf(viewModel.securityEngine.getAutoLockMinutes()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar & Güvenlik", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Firebase Auth Account Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (authState.isLoggedIn) ScanEmerald.copy(alpha = 0.3f) else ScanBlue.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (authState.isLoggedIn) ScanEmerald.copy(alpha = 0.15f) else ScanBlue.copy(alpha = 0.15f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (authState.isLoggedIn) {
                                            Text(
                                                text = (authState.displayName?.take(1) ?: "U").uppercase(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ScanEmerald
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = ScanBlue,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (authState.isLoggedIn) (authState.displayName ?: "ScanFlow Kullanıcısı") else "Firebase Hesabı",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (authState.isLoggedIn) (authState.email ?: "Giriş yapıldı") else "Giriş yaparak belgeleri eşitleyin",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (authState.isLoggedIn) {
                                IconButton(onClick = { viewModel.signOut() }) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Çıkış Yap", tint = Color(0xFFEF4444))
                                }
                            }
                        }

                        if (!authState.isLoggedIn) {
                            Button(
                                onClick = { showAuthSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                            ) {
                                Icon(imageVector = Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Giriş Yap / Hesap Oluştur", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = ScanEmerald, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Firebase Auth ile senkronize edildi",
                                    fontSize = 11.sp,
                                    color = ScanEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // VIP Upgrade Row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToPremium() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ScanGold.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, ScanGold.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null, tint = ScanGold, modifier = Modifier.size(28.dp))
                            Column {
                                Text(text = "ScanFlow VIP PRO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = if (isPremium) "Aboneliğiniz Aktif (Tüm özellikler açık)" else "Filigranı kaldırın & Sınırsız tarayın",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = ScanGold)
                    }
                }
            }

            // Security & Privacy Section
            item {
                Text(
                    text = "Biyometrik & Kasa Güvenliği",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // App PIN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPinDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ScanBlue)
                                Column {
                                    Text("Ana Kasa PIN Kodu", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (viewModel.securityEngine.isPinSet()) "Aktif (Uygulama açılışı ve hassas belgeler)" else "Ayarlanmadı",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (viewModel.securityEngine.isPinSet()) "Değiştir / Kaldır" else "Ayarla",
                                color = ScanBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Biometrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = ScanCyan)
                                Column {
                                    Text("Biyometrik Kilit (FaceID / Parmak İzi)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "Dokunmatik ve yüz tanıma ile anında kilit açma",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = {
                                    biometricEnabled = it
                                    viewModel.securityEngine.setBiometricEnabled(it)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sensitive Document Protection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = ScanGold)
                                Column {
                                    Text("Hassas Belge Koruması", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "Korumalı belgeler açılırken kimlik doğrulama zorunlu",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = sensitiveDocEnabled,
                                onCheckedChange = {
                                    sensitiveDocEnabled = it
                                    viewModel.securityEngine.setSensitiveDocProtectionEnabled(it)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto Lock Timeout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAutoLockMenu = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = ScanBlue)
                                Text("Otomatik Kilitleme", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Box {
                                Text(
                                    text = when (autoLockMinutes) {
                                        0 -> "Hemen"
                                        1 -> "1 Dakika"
                                        5 -> "5 Dakika"
                                        15 -> "15 Dakika"
                                        else -> "Hemen"
                                    },
                                    color = ScanBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                DropdownMenu(expanded = showAutoLockMenu, onDismissRequest = { showAutoLockMenu = false }) {
                                    DropdownMenuItem(text = { Text("Hemen") }, onClick = {
                                        autoLockMinutes = 0
                                        viewModel.securityEngine.setAutoLockMinutes(0)
                                        showAutoLockMenu = false
                                    })
                                    DropdownMenuItem(text = { Text("1 Dakika") }, onClick = {
                                        autoLockMinutes = 1
                                        viewModel.securityEngine.setAutoLockMinutes(1)
                                        showAutoLockMenu = false
                                    })
                                    DropdownMenuItem(text = { Text("5 Dakika") }, onClick = {
                                        autoLockMinutes = 5
                                        viewModel.securityEngine.setAutoLockMinutes(5)
                                        showAutoLockMenu = false
                                    })
                                    DropdownMenuItem(text = { Text("15 Dakika") }, onClick = {
                                        autoLockMinutes = 15
                                        viewModel.securityEngine.setAutoLockMinutes(15)
                                        showAutoLockMenu = false
                                    })
                                }
                            }
                        }
                    }
                }
            }

            // General Preferences
            item {
                Text(
                    text = "Genel Ayarlar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Dark Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(imageVector = Icons.Default.Brightness4, contentDescription = null, tint = ScanBlue)
                                Text("Karanlık Mod (Dark Theme)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode() }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Language Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLangMenu = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = ScanBlue)
                                Text("Uygulama Dili", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Box {
                                Text(
                                    text = when (selectedLanguage) {
                                        "tr" -> "Türkçe"
                                        "en" -> "English"
                                        "de" -> "Deutsch"
                                        "fr" -> "Français"
                                        "es" -> "Español"
                                        "ar" -> "العربية"
                                        else -> "Türkçe"
                                    },
                                    color = ScanBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                                    DropdownMenuItem(text = { Text("Türkçe") }, onClick = { viewModel.setLanguage("tr"); showLangMenu = false })
                                    DropdownMenuItem(text = { Text("English") }, onClick = { viewModel.setLanguage("en"); showLangMenu = false })
                                    DropdownMenuItem(text = { Text("Deutsch") }, onClick = { viewModel.setLanguage("de"); showLangMenu = false })
                                    DropdownMenuItem(text = { Text("Français") }, onClick = { viewModel.setLanguage("fr"); showLangMenu = false })
                                    DropdownMenuItem(text = { Text("Español") }, onClick = { viewModel.setLanguage("es"); showLangMenu = false })
                                    DropdownMenuItem(text = { Text("العربية") }, onClick = { viewModel.setLanguage("ar"); showLangMenu = false })
                                }
                            }
                        }
                    }
                }
            }

            // About ScanFlow
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "ScanFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScanBlue)
                        Text(text = "\"Scan Anything. Secure Everything.\"", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Sürüm 2.4.0 (Secure Release)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Firebase Auth • Biometric FaceID/Fingerprint • AES-256 Vault • ML Kit OCR", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // App PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Ana Kasa PIN Kodu") },
            text = {
                Column {
                    Text("Uygulama açılışında ve korumalı belgelerde istenecek 4 haneli PIN kodu:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("4 Haneli PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setAppPin(pinInput)
                            showPinDialog = false
                            Toast.makeText(context, "Ana PIN kodu kaydedildi", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Lütfen 4 haneli bir PIN girin", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                if (viewModel.securityEngine.isPinSet()) {
                    TextButton(
                        onClick = {
                            viewModel.removeAppPin()
                            showPinDialog = false
                            Toast.makeText(context, "PIN kodu kaldırıldı", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Kaldır", color = Color.Red)
                    }
                } else {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("İptal")
                    }
                }
            }
        )
    }

    // Auth Sheet
    if (showAuthSheet) {
        AuthModalBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showAuthSheet = false }
        )
    }
}
