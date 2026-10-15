package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null,
    val colorHex: String = "#2563EB",
    val iconName: String = "folder",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "İmzam",
    val imagePath: String,
    val colorHex: String = "#000000",
    val createdAt: Long = System.currentTimeMillis()
)
