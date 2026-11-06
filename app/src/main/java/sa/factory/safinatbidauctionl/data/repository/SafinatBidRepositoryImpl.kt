package sa.factory.safinatbidauctionl.data.repository

import sa.factory.safinatbidauctionl.domain.repository.SafinatBidRepository
import sa.factory.safinatbidauctionl.hardware.HardwareSensorDriver
import sa.factory.safinatbidauctionl.model.SafinatBidItem
import sa.factory.safinatbidauctionl.model.SafinatBidOrder
import sa.factory.safinatbidauctionl.model.SafinatBidTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SafinatBidRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : SafinatBidRepository {

    private val _catalog = MutableStateFlow<List<SafinatBidItem>>(listOf(
        SafinatBidItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع BLE Hardware Bidding Paddle GATT + Sub-100ms WebRTC Audio", 180.0, "معتمد", "الخدمات الرئيسية"),
        SafinatBidItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        SafinatBidItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<SafinatBidItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<SafinatBidItem>>(emptyList())
    override val activeCart: StateFlow<List<SafinatBidItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<SafinatBidTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            SafinatBidTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: SafinatBidItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: SafinatBidItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): SafinatBidOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = SafinatBidOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
