package sa.factory.safinatbidauctionl.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.safinatbidauctionl.model.SafinatBidItem

@Entity(tableName = "auction_live_items_cache")
data class SafinatBidEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): SafinatBidItem = SafinatBidItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
