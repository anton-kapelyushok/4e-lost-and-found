package fe.lnf.repository

import fe.lnf.model.ItemStatus
import fe.lnf.model.LostItem
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface LostItemRepository : CrudRepository<LostItem, Long>, PagingAndSortingRepository<LostItem, Long> {

    fun findByStatus(status: ItemStatus): List<LostItem>

    @Query("SELECT * FROM lost_items ORDER BY created_at DESC")
    fun findAllOrderByCreatedAtDesc(): List<LostItem>

    @Query("SELECT * FROM lost_items WHERE status IN (:statuses) ORDER BY created_at DESC")
    fun findByStatusInOrderByCreatedAtDesc(statuses: List<String>): List<LostItem>
}
