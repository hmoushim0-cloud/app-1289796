package sa.factory.safinatbidauctionl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SafinatBidEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun auction_liveDao(): SafinatBidDao
}
