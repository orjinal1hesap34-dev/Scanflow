package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ImageFilter

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val imagePath: String,
    val originalImagePath: String = "",
    val filterType: ImageFilter = ImageFilter.DOCUMENT,
    val rotationDegrees: Int = 0,
    val cropCornersJson: String = "", // e.g. "x1,y1,x2,y2,x3,y3,x4,y4"
    val ocrText: String = "",
    val ocrLanguage: String = "tr",
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
