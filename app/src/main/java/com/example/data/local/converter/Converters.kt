package com.example.data.local.converter

import androidx.room.TypeConverter
import com.example.domain.model.DocCategory
import com.example.domain.model.ImageFilter

class Converters {
    @TypeConverter
    fun fromCategory(category: DocCategory?): String {
        return category?.name ?: DocCategory.DOCUMENTS.name
    }

    @TypeConverter
    fun toCategory(value: String?): DocCategory {
        return try {
            if (value != null) DocCategory.valueOf(value) else DocCategory.DOCUMENTS
        } catch (e: Exception) {
            DocCategory.DOCUMENTS
        }
    }

    @TypeConverter
    fun fromFilter(filter: ImageFilter?): String {
        return filter?.name ?: ImageFilter.DOCUMENT.name
    }

    @TypeConverter
    fun toFilter(value: String?): ImageFilter {
        return try {
            if (value != null) ImageFilter.valueOf(value) else ImageFilter.DOCUMENT
        } catch (e: Exception) {
            ImageFilter.DOCUMENT
        }
    }
}
