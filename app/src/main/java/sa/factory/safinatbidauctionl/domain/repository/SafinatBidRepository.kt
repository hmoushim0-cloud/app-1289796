package sa.factory.safinatbidauctionl.domain.repository

import sa.factory.safinatbidauctionl.model.SafinatBidItem
import sa.factory.safinatbidauctionl.model.SafinatBidOrder
import sa.factory.safinatbidauctionl.model.SafinatBidTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SafinatBidRepository {
    val catalogFlow: Flow<List<SafinatBidItem>>
    val activeCart: StateFlow<List<SafinatBidItem>>
    val telemetryFlow: Flow<SafinatBidTelemetry>
    
    fun addToCart(item: SafinatBidItem)
    fun removeFromCart(item: SafinatBidItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): SafinatBidOrder
    suspend fun refreshCatalog()
}
