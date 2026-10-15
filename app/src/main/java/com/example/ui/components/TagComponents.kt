package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DocumentEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScanFlowViewModel

object TagUtils {
    // Return background color and accent text color for a given tag
    fun getTagColors(tag: String): Pair<Color, Color> {
        val lower = tag.trim().lowercase()
        return when {
            lower == "work" || lower == "iş" || lower.contains("work") || lower.contains("iş") ->
                Pair(Color(0xFF1E3A8A).copy(alpha = 0.45f), Color(0xFF60A5FA))

            lower == "personal" || lower == "kişisel" || lower.contains("personal") || lower.contains("kisisel") ->
                Pair(Color(0xFF065F46).copy(alpha = 0.45f), Color(0xFF34D399))

            lower == "receipts" || lower == "receipt" || lower == "fiş" || lower == "fatura" || lower.contains("fiş") || lower.contains("fatura") ->
                Pair(Color(0xFF78350F).copy(alpha = 0.45f), Color(0xFFFBBF24))

            lower == "important" || lower == "önemli" || lower == "acil" || lower == "urgent" ->
                Pair(Color(0xFF881337).copy(alpha = 0.45f), Color(0xFFFB7185))

            lower == "official" || lower == "resmi" || lower == "devlet" || lower.contains("resmi") ->
                Pair(Color(0xFF4C1D95).copy(alpha = 0.45f), Color(0xFFA78BFA))

            lower == "tax" || lower == "vergi" ->
                Pair(Color(0xFF312E81).copy(alpha = 0.45f), Color(0xFF818CF8))

            lower == "contract" || lower == "sözleşme" || lower == "sozlesme" ->
                Pair(Color(0xFF134E4A).copy(alpha = 0.45f), Color(0xFF2DD4BF))

            lower == "medical" || lower == "sağlık" || lower == "saglik" || lower == "rapor" ->
                Pair(Color(0xFF831843).copy(alpha = 0.45f), Color(0xFFF472B6))

            lower == "travel" || lower == "seyahat" || lower == "pasaport" ->
                Pair(Color(0xFF0E7490).copy(alpha = 0.45f), Color(0xFF38BDF8))

            else -> {
                // Dynamic deterministic color generation based on hash
                val hash = kotlin.math.abs(lower.hashCode())
                val palettes = listOf(
                    Pair(Color(0xFF1E3A8A).copy(alpha = 0.45f), Color(0xFF93C5FD)),
                    Pair(Color(0xFF065F46).copy(alpha = 0.45f), Color(0xFF6EE7B7)),
                    Pair(Color(0xFF581C87).copy(alpha = 0.45f), Color(0xFFC084FC)),
                    Pair(Color(0xFF701A75).copy(alpha = 0.45f), Color(0xFFF472B6)),
                    Pair(Color(0xFF164E63).copy(alpha = 0.45f), Color(0xFF38BDF8)),
                    Pair(Color(0xFF713F12).copy(alpha = 0.45f), Color(0xFFFCD34D))
                )
                palettes[hash % palettes.size]
            }
        }
    }

    fun getTagIcon(tag: String): ImageVector {
        val lower = tag.trim().lowercase()
        return when {
            lower == "work" || lower == "iş" || lower.contains("iş") -> Icons.Default.Work
            lower == "personal" || lower == "kişisel" || lower.contains("kisi") -> Icons.Default.Person
            lower == "receipts" || lower == "fiş" || lower == "fatura" || lower.contains("fiş") -> Icons.Default.Receipt
            lower == "important" || lower == "önemli" -> Icons.Default.Star
            lower == "official" || lower == "resmi" -> Icons.Default.VerifiedUser
            lower == "tax" || lower == "vergi" -> Icons.Default.AccountBalance
            lower == "contract" || lower == "sözleşme" -> Icons.Default.Description
            lower == "medical" || lower == "sağlık" -> Icons.Default.LocalHospital
            lower == "travel" || lower == "seyahat" -> Icons.Default.Flight
            else -> Icons.Default.LocalOffer
        }
    }

    val PRESET_TAG_SUGGESTIONS = listOf(
        "Work",
        "Personal",
        "Receipts",
        "Important",
        "Official",
        "Tax",
        "Contract",
        "Medical"
    )
}

@Composable
fun DocumentTagBadge(
    tag: String,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, textColor) = TagUtils.getTagColors(tag)
    val icon = TagUtils.getTagIcon(tag)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("tag_badge_$tag")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = tag.trim(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onRemove != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Etiketi Kaldır",
                    tint = textColor.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { onRemove() }
                )
            }
        }
    }
}

@Composable
fun DocumentTagRow(
    tags: String,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    onTagClick: ((String) -> Unit)? = null,
    onAddTagClick: (() -> Unit)? = null
) {
    val tagList = remember(tags) {
        tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    if (tagList.isEmpty() && onAddTagClick == null) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tagList.take(maxVisible).forEach { tag ->
            DocumentTagBadge(
                tag = tag,
                onClick = onTagClick?.let { { it(tag) } }
            )
        }

        if (tagList.size > maxVisible) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF334155).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Text(
                    text = "+${tagList.size - maxVisible}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        if (onAddTagClick != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ScanCyan.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, ScanCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onAddTagClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Etiket Ekle",
                        tint = ScanCyan,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Etiket",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScanCyan
                    )
                }
            }
        }
    }
}

@Composable
fun TagFilterBar(
    availableTags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Tümü" (All) Chip
        item {
            val isAllSelected = selectedTag == null
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTagSelected(null) }
                    .testTag("tag_filter_all"),
                shape = RoundedCornerShape(16.dp),
                color = if (isAllSelected) ScanBlue else DarkSurfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (isAllSelected) ScanCyanGlow else Color(0xFF334155)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = if (isAllSelected) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Tüm Etiketler",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSelected) Color.White else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        items(availableTags) { tag ->
            val isSelected = selectedTag != null && selectedTag.equals(tag, ignoreCase = true)
            val (bgColor, textColor) = TagUtils.getTagColors(tag)
            val icon = TagUtils.getTagIcon(tag)

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (isSelected) onTagSelected(null) else onTagSelected(tag)
                    }
                    .testTag("tag_filter_$tag"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) textColor.copy(alpha = 0.25f) else DarkSurfaceElevated,
                border = BorderStroke(
                    1.5.dp,
                    if (isSelected) textColor else Color(0xFF334155)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) textColor else Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) textColor else Color(0xFFCBD5E1)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seçili",
                            tint = textColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementSheet(
    document: DocumentEntity,
    viewModel: ScanFlowViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newTagInput by remember { mutableStateOf("") }
    
    // Parse current tags as list
    val currentTags = remember(document.tags) {
        document.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableStateList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = ScanCyanGlow,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Belge Etiketleri",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = document.title,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Assigned Tags Section
            Text(
                text = "Atanan Etiketler (${currentTags.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentTags.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Henüz etiket atanmamış. Aşağıdaki önerilerden seçin veya yeni ekleyin.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                // Flow-like row of current tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentTags.forEach { tag ->
                        DocumentTagBadge(
                            tag = tag,
                            onRemove = {
                                currentTags.remove(tag)
                                viewModel.updateDocumentTagsList(document.id, currentTags.toList())
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Preset Tags (e.g. Work, Personal, Receipts)
            Text(
                text = "Önerilen Etiketler (Hızlı Ekle)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Preset grid / chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TagUtils.PRESET_TAG_SUGGESTIONS.chunked(3).forEach { rowTags ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTags.forEach { presetTag ->
                            val isAssigned = currentTags.any { it.equals(presetTag, ignoreCase = true) }
                            val (bgColor, textColor) = TagUtils.getTagColors(presetTag)
                            val icon = TagUtils.getTagIcon(presetTag)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAssigned) bgColor else Color(0xFF0F172A),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAssigned) textColor else Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isAssigned) {
                                            currentTags.removeAll { it.equals(presetTag, ignoreCase = true) }
                                        } else {
                                            currentTags.add(presetTag)
                                        }
                                        viewModel.updateDocumentTagsList(document.id, currentTags.toList())
                                    }
                                    .testTag("preset_tag_$presetTag")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAssigned) Icons.Default.CheckCircle else icon,
                                        contentDescription = null,
                                        tint = if (isAssigned) textColor else Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = presetTag,
                                        fontSize = 12.sp,
                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAssigned) textColor else Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        // Pad if chunk has fewer items
                        if (rowTags.size < 3) {
                            repeat(3 - rowTags.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Tag Input Box
            Text(
                text = "Özel Etiket Oluştur",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("Yeni etiket adı yazın...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_tag_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmed = newTagInput.trim()
                            if (trimmed.isNotBlank() && !currentTags.any { it.equals(trimmed, ignoreCase = true) }) {
                                currentTags.add(trimmed)
                                viewModel.updateDocumentTagsList(document.id, currentTags.toList())
                                newTagInput = ""
                            }
                        }
                    )
                )

                Button(
                    onClick = {
                        val trimmed = newTagInput.trim()
                        if (trimmed.isNotBlank() && !currentTags.any { it.equals(trimmed, ignoreCase = true) }) {
                            currentTags.add(trimmed)
                            viewModel.updateDocumentTagsList(document.id, currentTags.toList())
                            newTagInput = ""
                        }
                    },
                    enabled = newTagInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScanBlue),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekle", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScanBlue)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tamamla", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
