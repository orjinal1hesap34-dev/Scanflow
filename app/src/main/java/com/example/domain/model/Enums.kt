package com.example.domain.model

enum class DocCategory(val displayName: String, val iconName: String) {
    ALL("Tüm Belgeler", "Folder"),
    DOCUMENTS("Belgeler", "Description"),
    ID_CARD("Kimlik Kartı", "Badge"),
    PASSPORT("Pasaport", "Public"),
    DRIVER_LICENSE("Ehliyet", "DirectionsCar"),
    BUSINESS_CARD("Kartvizit", "ContactPage"),
    RECEIPT("Makbuz", "Receipt"),
    INVOICE("Fatura", "ReceiptLong"),
    BOOK("Kitap / Not", "MenuBook"),
    OTHER("Diğer", "MoreHoriz")
}

enum class ImageFilter(val displayName: String) {
    ORIGINAL("Orijinal"),
    DOCUMENT("Belge"),
    MAGIC_COLOR("Sihirli Renk"),
    BLACK_WHITE("Siyah & Beyaz"),
    GRAYSCALE("Gri Tonlama"),
    ENHANCED("Geliştirilmiş (Enhanced)"),
    HIGH_CONTRAST("Yüksek Kontrast")
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    ZIP("zip", "application/zip"),
    TXT("txt", "text/plain")
}

enum class ExportQuality(val title: String, val compressionQuality: Int, val description: String) {
    LOW("Düşük Boyut", 40, "E-posta ve hızlı paylaşım için"),
    MEDIUM("Dengeli Kalite", 70, "Önerilen standart kalite"),
    HIGH("Yüksek Kalite (HD)", 90, "Net baskı ve arşiv için"),
    ORIGINAL("Orijinal (Kayıpsız)", 100, "Maksimum çözünürlük")
}

enum class SortOption(val title: String) {
    DATE_DESC("En Yeniye Göre"),
    DATE_ASC("En Eskiye Göre"),
    NAME_ASC("İsme Göre (A-Z)"),
    NAME_DESC("İsme Göre (Z-A)"),
    SIZE_DESC("Boyuta Göre")
}

enum class CloudProvider(val displayName: String, val iconColorHex: String) {
    GOOGLE_DRIVE("Google Drive", "#4285F4"),
    DROPBOX("Dropbox", "#0061FF"),
    ONE_DRIVE("OneDrive", "#0078D4")
}

enum class ShareTarget(val displayName: String, val description: String) {
    SYSTEM_CHOOSER("Tüm Uygulamalar", "WhatsApp, Telegram, Bluetooth vb. ile paylaş"),
    EMAIL("E-Posta (Gmail / Mail)", "Belgeyi doğrudan e-posta eki olarak gönder"),
    GOOGLE_DRIVE("Google Drive / Bulut", "Google Drive veya bulut uygulamasına yükle"),
    EXTERNAL_VIEWER("Harici PDF Okuyucu", "Adobe Acrobat, Drive PDF vb. ile aç"),
    PRINT("Doğrudan Yazdır", "Android Print framework ile kablosuz çıktı al")
}

