package fe.lnf.controller

import fe.lnf.model.ItemStatus
import fe.lnf.model.LostItem
import fe.lnf.repository.LostItemRepository
import fe.lnf.service.ImageService
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = ["*"])
class ItemsRestController(
    private val repository: LostItemRepository,
    private val imageService: ImageService
) {

    @GetMapping
    fun getAllItems(
        @RequestParam(required = false) status: List<String>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<LostItem> {
        // Get all items (already sorted by created_at DESC)
        val allItems = if (status.isNullOrEmpty()) {
            repository.findAllOrderByCreatedAtDesc()
        } else {
            repository.findByStatusInOrderByCreatedAtDesc(status)
        }

        // Manual pagination
        val start = page * size
        val end = minOf(start + size, allItems.size)
        val pageContent = if (start < allItems.size) {
            allItems.subList(start, end)
        } else {
            emptyList()
        }

        val pageable = PageRequest.of(page, size)
        return PageImpl(pageContent, pageable, allItems.size.toLong())
    }

    @GetMapping("/{id}")
    fun getItemById(@PathVariable id: Long): LostItem {
        return repository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found") }
    }

    @GetMapping("/status/{status}")
    fun getItemsByStatus(@PathVariable status: ItemStatus): List<LostItem> {
        return repository.findByStatus(status)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(
        @RequestParam name: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) status: ItemStatus?,
        @RequestParam(required = false) image: MultipartFile?
    ): LostItem {
        // Save image if provided
        val imagePath = image?.let {
            try {
                imageService.saveImage(it)
            } catch (e: IllegalArgumentException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
            }
        }

        val item = LostItem(
            name = name,
            description = description,
            location = location,
            status = status ?: ItemStatus.FOUND,
            imagePath = imagePath
        )
        return repository.save(item)
    }

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateItem(
        @PathVariable id: Long,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) status: ItemStatus?,
        @RequestParam(required = false) image: MultipartFile?
    ): LostItem {
        val existing = repository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found") }

        // Handle image update
        var newImagePath = existing.imagePath
        if (image != null) {
            // Delete old image
            imageService.deleteImage(existing.imagePath)
            // Save new image
            try {
                newImagePath = imageService.saveImage(image)
            } catch (e: IllegalArgumentException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
            }
        }

        val updated = existing.copy(
            name = name ?: existing.name,
            description = description ?: existing.description,
            location = location ?: existing.location,
            status = status ?: existing.status,
            imagePath = newImagePath,
            updatedAt = java.time.LocalDateTime.now()
        )
        return repository.save(updated)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@PathVariable id: Long) {
        val item = repository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found") }

        // Delete image if exists
        imageService.deleteImage(item.imagePath)

        repository.deleteById(id)
    }

    @GetMapping("/images/{filename}")
    fun getImage(@PathVariable filename: String): ResponseEntity<Resource> {
        val imagePath = imageService.getImagePath(filename)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found")

        val resource = FileSystemResource(imagePath)
        if (!resource.exists()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found")
        }

        // Determine content type
        val contentType = when (filename.substringAfterLast('.').lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource)
    }
}

data class CreateItemRequest(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val status: ItemStatus? = null
)

data class UpdateItemRequest(
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val status: ItemStatus? = null
)
