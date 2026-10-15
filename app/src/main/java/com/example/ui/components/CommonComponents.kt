package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.DocumentEntity
import com.example.domain.model.DocCategory
import com.example.domain.model.ExportFormat
import com.example.domain.model.ExportQuality
import com.example.domain.model.ImageFilter
import com.example.ui.theme.ScanBlue
import com.example.ui.theme.ScanBlueDark
import com.example.ui.theme.ScanCyan
import com.example.ui.theme.ScanGold
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CategoryIcon(category: DocCategory, tint: Color = MaterialTheme.colorScheme.primary, size: Int = 20) {
    val icon: ImageVector = when (category) {
        DocCategory.ALL -> Icons.Default.Folder
        DocCategory.DOCUMENTS -> Icons.Default.Description
        DocCategory.ID_CARD -> Icons.Default.Badge
        DocCategory.PASSPORT -> Icons.Default.Public
        DocCategory.DRIVER_LICENSE -> Icons.Default.DirectionsCar
        DocCategory.BUSINESS_CARD -> Icons.Default.ContactPage
        DocCategory.RECEIPT -> Icons.Default.Receipt
        DocCategory.INVOICE -> Icons.Default.ReceiptLong
        DocCategory.BOOK -> Icons.Default.MenuBook
        DocCategory.OTHER -> Icons.Default.MoreHoriz
    }
    Icon(
        imageVector = icon,
        contentDescription = category.displayName,
        tint = tint,
        modifier = Modifier.size(size.dp)
    )
}

@Composable
fun CategoryChipsCarousel(
    selectedCategory: DocCategory,
    onCategorySelected: (DocCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DocCategory.values()) { category ->
            val isSelected = category == selectedCategory
            val bgBrush = if (isSelected) {
                Brush.horizontalGradient(listOf(ScanBlue, ScanCyan))
            } else {
                Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCategorySelected(category) }
                    .testTag("category_chip_${category.name}"),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .background(bgBrush)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryIcon(
                        category = category,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18
                    )
                    Text(
                        text = category.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentGridCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(document.updatedAt))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("doc_grid_card_${document.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (document.thumbnailPath.isNotBlank() && File(document.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(document.thumbnailPath),
                        contentDescription = document.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = ScanBlue.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Page count badge (top-left)
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "${document.pageCount} Sayfa",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Lock indicator if locked
                if (document.isLocked) {
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        shape = CircleShape,
                        color = ScanGold
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilitli",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp)
                        )
                    }
                } else {
                    // Favorite button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = if (document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (document.isFavorite) Color(0xFFEF4444) else Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Info row
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Seçenekler",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = document.category.displayName,
                        fontSize = 11.sp,
                        color = ScanBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (document.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    DocumentTagRow(tags = document.tags, maxVisible = 2)
                }
            }
        }
    }
}

@Composable
fun DocumentListCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(document.updatedAt))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("doc_list_card_${document.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (document.thumbnailPath.isNotBlank() && File(document.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(document.thumbnailPath),
                        contentDescription = document.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = ScanBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = document.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (document.isLocked) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilitli",
                            tint = ScanGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${document.pageCount} Sayfa",
                        fontSize = 12.sp,
                        color = ScanBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (document.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DocumentTagRow(tags = document.tags, maxVisible = 3)
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (document.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onOptionsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Seçenekler",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentOptionsBottomSheet(
    document: DocumentEntity,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onLockToggle: () -> Unit,
    onMoveFolder: () -> Unit,
    onManageTags: () -> Unit,
    onOcr: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = document.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${document.pageCount} Sayfa • ${document.category.displayName}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OptionRowItem(icon = Icons.Default.LocalOffer, title = "Etiketleri Yönet (Labels)", onClick = onManageTags)
            OptionRowItem(icon = Icons.Default.Share, title = "Paylaş", onClick = onShare)
            OptionRowItem(icon = Icons.Default.PictureAsPdf, title = "PDF / Resim Olarak Dışa Aktar", onClick = onExport)
            OptionRowItem(icon = Icons.Default.TextFields, title = "Metin Tanıma (OCR)", onClick = onOcr)
            OptionRowItem(icon = Icons.Default.Edit, title = "Yeniden Adlandır", onClick = onRename)
            OptionRowItem(
                icon = if (document.isLocked) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                title = if (document.isLocked) "Biyometrik / PIN Kilidini Kaldır" else "Biyometrik & PIN ile Kilitle",
                onClick = onLockToggle
            )
            OptionRowItem(icon = Icons.Default.DriveFileMove, title = "Klasöre Taşı", onClick = onMoveFolder)
            OptionRowItem(
                icon = Icons.Default.Delete,
                title = "Çöp Kutusuna Taşı",
                tint = Color(0xFFEF4444),
                onClick = onDelete
            )
        }
    }
}

@Composable
fun OptionRowItem(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    document: DocumentEntity,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onExportConfirmed: (ExportFormat, ExportQuality, Boolean, com.example.domain.model.ShareTarget) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedQuality by remember { mutableStateOf(ExportQuality.HIGH) }
    var selectedShareTarget by remember { mutableStateOf(com.example.domain.model.ShareTarget.SYSTEM_CHOOSER) }
    var includeWatermark by remember { mutableStateOf(!isPremium) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dışa Aktar & Paylaş",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${document.title} • ${document.pageCount} Sayfa",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ScanBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = selectedFormat.name,
                        color = ScanBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format Selection
            Text(text = "Dosya Formatı", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportFormat.values().forEach { fmt ->
                    val isSel = fmt == selectedFormat
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFormat = fmt },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) ScanBlue else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = fmt.name,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Intent Target Selection (Sharing Destination)
            Text(text = "Aktarım & Paylaşım Yöntemi (Android Intent)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.example.domain.model.ShareTarget.values().forEach { target ->
                    val isTargetSel = target == selectedShareTarget
                    val targetIcon = when (target) {
                        com.example.domain.model.ShareTarget.SYSTEM_CHOOSER -> Icons.Default.Share
                        com.example.domain.model.ShareTarget.EMAIL -> Icons.Default.Email
                        com.example.domain.model.ShareTarget.GOOGLE_DRIVE -> Icons.Default.CloudUpload
                        com.example.domain.model.ShareTarget.EXTERNAL_VIEWER -> Icons.Default.Visibility
                        com.example.domain.model.ShareTarget.PRINT -> Icons.Default.Print
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedShareTarget = target },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTargetSel) ScanBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTargetSel) ScanBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isTargetSel) ScanBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = targetIcon,
                                        contentDescription = target.displayName,
                                        tint = if (isTargetSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isTargetSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTargetSel) ScanBlue else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = target.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }

                            RadioButton(
                                selected = isTargetSel,
                                onClick = { selectedShareTarget = target }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quality Selection
            Text(text = "Kalite ve Boyut Optimizasyonu", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            ExportQuality.values().forEach { q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedQuality = q }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedQuality == q, onClick = { selectedQuality = q })
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(text = q.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(text = q.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Watermark toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "ScanFlow Filigranı", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isPremium) "VIP: Filigran kapalı" else "Filigransız çıktı için VIP",
                        fontSize = 11.sp,
                        color = if (isPremium) ScanCyan else ScanGold
                    )
                }
                Switch(
                    checked = includeWatermark,
                    onCheckedChange = { if (isPremium) includeWatermark = it },
                    enabled = isPremium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Action Button based on target
            val actionButtonText = when (selectedShareTarget) {
                com.example.domain.model.ShareTarget.SYSTEM_CHOOSER -> "Tüm Uygulamalarla Paylaş"
                com.example.domain.model.ShareTarget.EMAIL -> "E-Posta (Gmail) ile Gönder"
                com.example.domain.model.ShareTarget.GOOGLE_DRIVE -> "Google Drive / Bulut'a Aktar"
                com.example.domain.model.ShareTarget.EXTERNAL_VIEWER -> "PDF Okuyucu ile Aç"
                com.example.domain.model.ShareTarget.PRINT -> "Yazıcıya Gönder (Yazdır)"
            }

            Button(
                onClick = { onExportConfirmed(selectedFormat, selectedQuality, includeWatermark, selectedShareTarget) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
            ) {
                Icon(
                    imageVector = when (selectedShareTarget) {
                        com.example.domain.model.ShareTarget.SYSTEM_CHOOSER -> Icons.Default.Share
                        com.example.domain.model.ShareTarget.EMAIL -> Icons.Default.Email
                        com.example.domain.model.ShareTarget.GOOGLE_DRIVE -> Icons.Default.CloudUpload
                        com.example.domain.model.ShareTarget.EXTERNAL_VIEWER -> Icons.Default.Visibility
                        com.example.domain.model.ShareTarget.PRINT -> Icons.Default.Print
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = actionButtonText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun FilterSelectorRow(
    selectedFilter: ImageFilter,
    onFilterSelected: (ImageFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ImageFilter.values()) { filter ->
            val isSel = filter == selectedFilter
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSel) ScanBlue else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = filter.displayName,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ColorMatrixFilterSelectorRow(
    selectedFilter: com.example.domain.engine.ColorMatrixFilterType,
    onFilterSelected: (com.example.domain.engine.ColorMatrixFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(com.example.domain.engine.ColorMatrixFilterType.values()) { filter ->
            val isSel = filter == selectedFilter
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSel) ScanBlue else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = filter.displayName,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
