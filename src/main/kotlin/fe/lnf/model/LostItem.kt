package fe.lnf.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("lost_items")
data class LostItem(
    @Id
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val status: ItemStatus = ItemStatus.LOST,
    val imagePath: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ItemStatus {
    LOST, FOUND, CLAIMED
}
