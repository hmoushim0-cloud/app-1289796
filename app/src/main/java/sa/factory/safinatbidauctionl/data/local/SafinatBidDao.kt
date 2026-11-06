package sa.factory.safinatbidauctionl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SafinatBidDao {
    @Query("SELECT * FROM auction_live_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<SafinatBidEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SafinatBidEntity>)

    @Query("DELETE FROM auction_live_items_cache")
    suspend fun clearAll()
}
