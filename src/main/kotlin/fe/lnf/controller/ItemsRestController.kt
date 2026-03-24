package fe.lnf.controller

import fe.lnf.model.ItemStatus
import fe.lnf.model.LostItem
import fe.lnf.repository.LostItemRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = ["*"])
class ItemsRestController(
    private val repository: LostItemRepository
) {

    @GetMapping
    fun getAllItems(): List<LostItem> {
        return repository.findAllOrderByCreatedAtDesc()
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(@RequestBody request: CreateItemRequest): LostItem {
        val item = LostItem(
            name = request.name,
            description = request.description,
            location = request.location,
            status = request.status ?: ItemStatus.LOST
        )
        return repository.save(item)
    }

    @PutMapping("/{id}")
    fun updateItem(@PathVariable id: Long, @RequestBody request: UpdateItemRequest): LostItem {
        val existing = repository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found") }

        val updated = existing.copy(
            name = request.name ?: existing.name,
            description = request.description ?: existing.description,
            location = request.location ?: existing.location,
            status = request.status ?: existing.status,
            updatedAt = java.time.LocalDateTime.now()
        )
        return repository.save(updated)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@PathVariable id: Long) {
        if (!repository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")
        }
        repository.deleteById(id)
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
