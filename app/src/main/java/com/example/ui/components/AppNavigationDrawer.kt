package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DocCategory
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanBlueLight
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanCyanGlow
import com.example.ui.theme.ScanEmerald
import com.example.ui.theme.ScanGold
import com.example.ui.theme.ScanGoldLight
import com.example.ui.theme.ScanPurple
import com.example.ui.viewmodel.ScanFlowViewModel

sealed class DrawerNavDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badge: String? = null
) {
    object AllDocuments : DrawerNavDestination(
        title = "Tüm Belgeler",
        subtitle = "Ana belge kütüphanesi",
        icon = Icons.Default.Description,
        accentColor = ScanBlueLight
    )
    object CameraScan : DrawerNavDestination(
        title = "Akıllı Tarama",
        subtitle = "Otomatik kenar & HD tarama",
        icon = Icons.Default.CameraAlt,
        accentColor = ScanCyanGlow,
        badge = "HD"
    )
    object PhotoStorage : DrawerNavDestination(
        title = "Fotoğraf Sakla & Galeri",
        subtitle = "Fotoğraf yükle ve güvenle sakla",
        icon = Icons.Default.Image,
        accentColor = ScanCyanGlow,
        badge = "FOTO"
    )
    object SignatureStudio : DrawerNavDestination(
        title = "Dijital İmza Stüdyosu",
        subtitle = "Çiz, yükle ve PDF'e damgala",
        icon = Icons.Default.Draw,
        accentColor = ScanEmerald,
        badge = "PRO"
    )
    object OcrStudio : DrawerNavDestination(
        title = "AI Metin Tanıma (OCR)",
        subtitle = "Görselden metne çevirici",
        icon = Icons.Default.TextFields,
        accentColor = ScanPurple,
        badge = "AI"
    )
    object PdfTools : DrawerNavDestination(
        title = "PDF Araçları & Birleştir",
        subtitle = "Birleştir, şifrele, sıkıştır",
        icon = Icons.Default.PictureAsPdf,
        accentColor = ScanGoldLight
    )
    object Folders : DrawerNavDestination(
        title = "Akıllı Klasörler",
        subtitle = "Özel etiket ve kategoriler",
        icon = Icons.Default.Folder,
        accentColor = ScanBlue
    )
    object CloudSync : DrawerNavDestination(
        title = "Bulut Yedekleme",
        subtitle = "Google Drive & Şifreli Bulut",
        icon = Icons.Default.CloudSync,
        accentColor = ScanCyan
    )
    object SecurityVault : DrawerNavDestination(
        title = "Güvenlik & Gizli Kasa",
        subtitle = "PIN ve Biyometrik Kilit",
        icon = Icons.Default.Lock,
        accentColor = ScanGold
    )
    object Trash : DrawerNavDestination(
        title = "Çöp Kutusu",
        subtitle = "Silinen belgeleri geri yükle",
        icon = Icons.Default.Delete,
        accentColor = Color(0xFFEF4444)
    )
    object Premium : DrawerNavDestination(
        title = "Ultra VIP / PRO Paket",
        subtitle = "Tüm premium özellikler açık",
        icon = Icons.Default.WorkspacePremium,
        accentColor = ScanGold,
        badge = "VIP"
    )
    object Settings : DrawerNavDestination(
        title = "Ayarlar & Tercihler",
        subtitle = "Tarama kalitesi, dil ve tema",
        icon = Icons.Default.Settings,
        accentColor = Color(0xFF94A3B8)
    )
}

@Composable
fun AppNavigationDrawer(
    viewModel: ScanFlowViewModel,
    onCloseDrawer: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPhotoStorage: () -> Unit = {},
    onNavigateToSignature: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToPdfTools: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenAuth: () -> Unit = {},
    onSelectCategory: (DocCategory) -> Unit
) {
    val documents by viewModel.allActiveDocuments.collectAsStateWithLifecycle()
    val signatures by viewModel.allSignatures.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("app_navigation_drawer"),
        drawerContainerColor = ObsidianBlack,
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBlack,
                            Color(0xFF070B14),
                            Color(0xFF0D1322)
                        )
                    )
                )
        ) {
            // Scrollable Menu List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Section: Core Scanner Tools
                item {
                    Text(
                        text = "TARAYICI & STÜDYO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 6.dp)
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.AllDocuments,
                        badgeText = "${documents.size}",
                        onClick = {
                            onSelectCategory(DocCategory.ALL)
                            onCloseDrawer()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.CameraScan,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToCamera()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.PhotoStorage,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToPhotoStorage()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.SignatureStudio,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToSignature()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.OcrStudio,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToOcr()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.PdfTools,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToPdfTools()
                        }
                    )
                }

                // Section: Organization & Cloud
                item {
                    Text(
                        text = "KÜTÜPHANE & GÜVENLİK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 16.dp, bottom = 6.dp)
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.Folders,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToFolders()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.CloudSync,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToCloudSync()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.SecurityVault,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToSettings()
                        }
                    )
                }

                item {
                    DrawerItemCard(
                        destination = DrawerNavDestination.Trash,
                        onClick = {
                            onCloseDrawer()
                            onNavigateToTrash()
                        }
                    )
                }

                // Section: Category Quick Switches inside Drawer
                item {
                    Text(
                        text = "KATEGORİLER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 16.dp, bottom = 6.dp)
                    )
                }

                items(DocCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    val count = if (category == DocCategory.ALL) documents.size else documents.count { it.category == category }
                    val catIcon = when (category) {
                        DocCategory.ALL -> Icons.Default.Folder
                        DocCategory.DOCUMENTS -> Icons.Default.Description
                        DocCategory.ID_CARD -> Icons.Default.CreditCard
                        DocCategory.PASSPORT -> Icons.Default.Description
                        DocCategory.DRIVER_LICENSE -> Icons.Default.CreditCard
                        DocCategory.BUSINESS_CARD -> Icons.Default.CreditCard
                        DocCategory.RECEIPT -> Icons.Default.Receipt
                        DocCategory.INVOICE -> Icons.Default.Receipt
                        DocCategory.BOOK -> Icons.Default.MenuBook
                        DocCategory.OTHER -> Icons.Default.Folder
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ScanBlue.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                onSelectCategory(category)
                                onCloseDrawer()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = null,
                                tint = if (isSelected) ScanCyanGlow else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = category.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) ScanBlue else Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = Color(0xFF1E293B),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Ultra Dark Bottom Bar with Theme Toggle & Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AMOLED Black Theme Switcher Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkMode) ScanCyanGlow else ScanGoldLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isDarkMode) "Siyah Tema (AMOLED)" else "Açık Tema",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ScanBlue,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Settings Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onCloseDrawer()
                            onNavigateToSettings()
                        },
                    color = Color(0xFF1E293B)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ayarlar",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun DrawerItemCard(
    destination: DrawerNavDestination,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("drawer_item_${destination.title.lowercase().replace(" ", "_")}"),
        color = Color(0xFF0F172A).copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = destination.accentColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, destination.accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            tint = destination.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = destination.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = destination.subtitle,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val badge = badgeText ?: destination.badge
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = destination.accentColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, destination.accentColor)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = destination.accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
