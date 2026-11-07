package sa.factory.safinatbidauctionl

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.safinatbidauctionl.data.repository.SafinatBidRepositoryImpl
import sa.factory.safinatbidauctionl.engine.*
import sa.factory.safinatbidauctionl.hardware.HardwareSensorDriver
import sa.factory.safinatbidauctionl.model.SafinatBidItem

class SafinatBidRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = SafinatBidRepositoryImpl(sensorDriver)

        val item1 = SafinatBidItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = SafinatBidItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testBleBiddingPaddlePacketProcessing() {
        val bleEngine = BleBiddingPaddleGattEngine()
        val packet = bleEngine.createHardwareTriggerPacket("PADDLE-SA-042", "BDR-9901")
        val result = bleEngine.processIncomingPacket(packet)

        assertTrue("Packet should be successfully processed", result.isProcessed)
        assertEquals("PADDLE-SA-042", result.paddleId)
        assertTrue("BLE latency should be within sub-100ms tolerance", result.latencyMs in 1..100)
        assertEquals(PaddleHapticFeedback.BID_ACKNOWLEDGED, result.feedback)
        assertEquals("SHA-256 digest length must be 64 characters", 64, result.packetDigest.length)
    }

    @Test
    fun testBleBiddingPaddleReplayRejection() {
        val bleEngine = BleBiddingPaddleGattEngine()
        val pkt1 = bleEngine.createHardwareTriggerPacket("PADDLE-SA-042", "BDR-9901")
        val res1 = bleEngine.processIncomingPacket(pkt1)
        assertTrue(res1.isProcessed)

        // Resend identical packet with same sequence number
        val res2 = bleEngine.processIncomingPacket(pkt1)
        assertFalse("Replay packet must be rejected", res2.isProcessed)
        assertEquals(PaddleHapticFeedback.REJECTED_INVALID, res2.feedback)
    }

    @Test
    fun testAuctionIncrementLadderCalculation() {
        val auctionEngine = DynamicAuctionBiddingEngine()

        assertEquals(500.0, auctionEngine.calculateMinimumIncrementSar(5000.0), 0.01)
        assertEquals(1000.0, auctionEngine.calculateMinimumIncrementSar(25000.0), 0.01)
        assertEquals(5000.0, auctionEngine.calculateMinimumIncrementSar(150000.0), 0.01)
        assertEquals(10000.0, auctionEngine.calculateMinimumIncrementSar(500000.0), 0.01)
    }

    @Test
    fun testBidPlacementAndAntiSnipeExtension() {
        val auctionEngine = DynamicAuctionBiddingEngine()
        val initialLot = AuctionLotState(
            lotId = "LOT-101",
            title = "Arabian Mare 'Abaq'",
            openingPriceSar = 100000.0,
            currentPriceSar = 100000.0,
            reservePriceSar = 150000.0,
            highestBidderId = "BDR-1",
            highestPaddleId = "PADDLE-1",
            countdownRemainingSec = 10, // In anti-sniping window (<= 15s)
            totalBidsPlaced = 1,
            status = AuctionLotStatus.ACTIVE_BIDDING
        )

        val result = auctionEngine.placeBid(initialLot, "BDR-2", "PADDLE-2")
        assertTrue("Bid should be accepted", result.isAccepted)
        assertEquals(105000.0, result.updatedLotState.currentPriceSar, 0.01)
        assertEquals("BDR-2", result.updatedLotState.highestBidderId)
        assertTrue("Anti-snipe flag should be active", result.updatedLotState.isAntiSnipeExtended)
        assertEquals("Countdown should extend by 30s (10 + 30 = 40s)", 40, result.updatedLotState.countdownRemainingSec)
    }

    @Test
    fun testHammerCallProgressionAndReserveCheck() {
        val auctionEngine = DynamicAuctionBiddingEngine()
        val lot = AuctionLotState(
            lotId = "LOT-102",
            title = "Purebred Filly",
            openingPriceSar = 50000.0,
            currentPriceSar = 80000.0,
            reservePriceSar = 70000.0,
            highestBidderId = "BDR-WIN",
            highestPaddleId = "PADDLE-WIN",
            countdownRemainingSec = 12,
            totalBidsPlaced = 5,
            status = AuctionLotStatus.ACTIVE_BIDDING
        )

        // Advance to 10s: Going once
        val once = auctionEngine.advanceCountdown(lot, 2)
        assertEquals(10, once.countdownRemainingSec)
        assertEquals(AuctionLotStatus.GOING_ONCE, once.status)

        // Advance to 5s: Going twice
        val twice = auctionEngine.advanceCountdown(once, 5)
        assertEquals(5, twice.countdownRemainingSec)
        assertEquals(AuctionLotStatus.GOING_TWICE, twice.status)

        // Advance to 0s: Hammer fall (reserve met: 80k >= 70k)
        val sold = auctionEngine.advanceCountdown(twice, 5)
        assertEquals(0, sold.countdownRemainingSec)
        assertEquals(AuctionLotStatus.HAMMER_FALL_SOLD, sold.status)
    }

    @Test
    fun testHardwareSensorDriverEmitsValidTelemetry() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals(2, frames.size)
        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
        assertTrue(frames[1].livePayload.contains("ONLINE"))
        assertTrue(frames[1].livePayload.contains("Lot:"))
        assertTrue(frames[1].livePayload.contains("Paddle:"))
    }
}
