package fe.lnf.repository

import fe.lnf.model.ItemStatus
import fe.lnf.model.LostItem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface LostItemRepository : CrudRepository<LostItem, Long> {

    fun findByStatus(status: ItemStatus): List<LostItem>

    @Query("SELECT * FROM lost_items ORDER BY created_at DESC")
    fun findAllOrderByCreatedAtDesc(): List<LostItem>
}
