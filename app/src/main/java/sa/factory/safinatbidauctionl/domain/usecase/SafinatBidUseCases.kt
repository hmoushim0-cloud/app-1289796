package sa.factory.safinatbidauctionl.domain.usecase

import sa.factory.safinatbidauctionl.domain.repository.SafinatBidRepository
import sa.factory.safinatbidauctionl.model.SafinatBidItem
import sa.factory.safinatbidauctionl.model.SafinatBidOrder
import sa.factory.safinatbidauctionl.model.SafinatBidTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: SafinatBidRepository) {
    operator fun invoke(): Flow<List<SafinatBidItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: SafinatBidRepository) {
    val cart = repository.activeCart
    fun add(item: SafinatBidItem) = repository.addToCart(item)
    fun remove(item: SafinatBidItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: SafinatBidRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): SafinatBidOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: SafinatBidRepository) {
    operator fun invoke(): Flow<SafinatBidTelemetry> = repository.telemetryFlow
}
