package fe.lnf.service

import net.coobird.thumbnailator.Thumbnails
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Service
class ImageService(
    @Value("\${app.upload.dir:/uploads}") private val uploadDir: String,
    @Value("\${app.upload.max-size:10485760}") private val maxFileSize: Long, // 10MB default
    @Value("\${app.upload.target-size:512000}") private val targetSize: Long // 500KB default
) {

    init {
        // Create upload directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(uploadDir))
        } catch (e: Exception) {
            // In test environments or when directory can't be created, log warning
            // Directory will be created lazily on first upload if needed
            println("Warning: Could not create upload directory $uploadDir: ${e.message}")
        }
    }

    fun saveImage(file: MultipartFile): String {
        // Validate file
        validateImage(file)

        // Ensure upload directory exists
        Files.createDirectories(Paths.get(uploadDir))

        // Generate unique filename
        val extension = file.originalFilename?.substringAfterLast('.', "jpg") ?: "jpg"
        val filename = "${UUID.randomUUID()}.$extension"
        val filePath = Paths.get(uploadDir, filename)

        // Save and resize image
        val tempFile = File.createTempFile("upload", extension)
        file.transferTo(tempFile)

        try {
            // Check if resize is needed
            if (tempFile.length() > targetSize) {
                // Resize to target size (approximately)
                val quality = calculateQuality(tempFile.length())
                Thumbnails.of(tempFile)
                    .scale(1.0)
                    .outputQuality(quality)
                    .toFile(filePath.toFile())
            } else {
                // Just copy if already under target size
                Files.copy(tempFile.toPath(), filePath)
            }
        } finally {
            tempFile.delete()
        }

        return filename
    }

    fun deleteImage(filename: String?) {
        if (filename.isNullOrBlank()) return
        try {
            val filePath = Paths.get(uploadDir, filename)
            Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            // Log error but don't throw - file might already be deleted
            println("Error deleting image $filename: ${e.message}")
        }
    }

    fun getImagePath(filename: String?): String? {
        if (filename.isNullOrBlank()) return null
        val filePath = Paths.get(uploadDir, filename)
        return if (Files.exists(filePath)) filePath.toString() else null
    }

    private fun validateImage(file: MultipartFile) {
        // Check if file is empty
        if (file.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }

        // Check file size (5MB max)
        if (file.size > maxFileSize) {
            throw IllegalArgumentException("File size exceeds maximum of ${maxFileSize / 1024 / 1024}MB")
        }

        // Check content type
        val contentType = file.contentType
        if (contentType == null || !contentType.startsWith("image/")) {
            throw IllegalArgumentException("File must be an image")
        }

        // Validate allowed image types
        val allowedTypes = listOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp")
        if (contentType !in allowedTypes) {
            throw IllegalArgumentException("Unsupported image type. Allowed: JPEG, PNG, GIF, WebP")
        }
    }

    private fun calculateQuality(fileSize: Long): Double {
        // Calculate compression quality based on file size
        // Larger files get more compression
        return when {
            fileSize < targetSize -> 0.9 // Already small, minimal compression
            fileSize < targetSize * 2 -> 0.7 // Medium compression
            fileSize < targetSize * 3 -> 0.5 // More compression
            else -> 0.3 // Heavy compression for very large files
        }
    }
}
