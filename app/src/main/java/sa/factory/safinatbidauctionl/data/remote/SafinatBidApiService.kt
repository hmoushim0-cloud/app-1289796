package sa.factory.safinatbidauctionl.data.remote

import sa.factory.safinatbidauctionl.model.SafinatBidItem
import sa.factory.safinatbidauctionl.model.SafinatBidOrder

interface SafinatBidApiService {
    suspend fun fetchCatalog(): List<SafinatBidItem>
    suspend fun submitOrder(order: SafinatBidOrder): Boolean
}
