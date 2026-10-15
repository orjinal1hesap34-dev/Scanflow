package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.DocCategory

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: DocCategory = DocCategory.DOCUMENTS,
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val isLocked: Boolean = false,
    val lockPin: String? = null,
    val tags: String = "", // Comma separated
    val pageCount: Int = 1,
    val thumbnailPath: String = "",
    val fileSize: Long = 0L,
    val ocrFullText: String = "",
    val notes: String = ""
)
