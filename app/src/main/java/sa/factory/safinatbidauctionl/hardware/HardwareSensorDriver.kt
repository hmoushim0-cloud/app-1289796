package sa.factory.safinatbidauctionl.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.safinatbidauctionl.engine.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    val peripheral = "BLE Hardware Bidding Paddle GATT + Sub-100ms WebRTC Audio"
    val bleEngine = BleBiddingPaddleGattEngine()
    val auctionEngine = DynamicAuctionBiddingEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "BLE GATT Service 0xFF10 & WebRTC OPUS Audio initializing..."))
        delay(200)

        // Seed Arabian thoroughbred auction lot
        var currentLot = AuctionLotState(
            lotId = "LOT-SAF-08",
            title = "Arabian Stallion 'Kahil Al Shaqab'",
            openingPriceSar = 150000.0,
            currentPriceSar = 150000.0,
            reservePriceSar = 200000.0,
            highestBidderId = "BDR-9901",
            highestPaddleId = "PADDLE-SA-042",
            countdownRemainingSec = 45,
            totalBidsPlaced = 0,
            status = AuctionLotStatus.ACTIVE_BIDDING
        )

        val initPacket = bleEngine.createHardwareTriggerPacket("PADDLE-SA-042", "BDR-9901")
        val initRes = bleEngine.processIncomingPacket(initPacket)
        val initialBidRes = auctionEngine.placeBid(currentLot, "BDR-9901", "PADDLE-SA-042")
        currentLot = initialBidRes.updatedLotState

        val priceStr = String.format("%.0f", currentLot.currentPriceSar)
        val initPayload = "[ONLINE] Lot: " + currentLot.lotId + " | Current: " + priceStr + " SAR | " +
                "Paddle: " + currentLot.highestPaddleId + " (" + initRes.latencyMs + "ms) | " +
                "Stage: " + currentLot.status.name + " (" + currentLot.countdownRemainingSec + "s)"
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", initPayload))

        var frameCounter = 1

        while (true) {
            delay(1500)
            frameCounter++
            val jitter = Random.nextInt(1, 8)

            // Simulate hardware paddle bids periodically
            if (frameCounter % 3 == 0 && currentLot.status == AuctionLotStatus.ACTIVE_BIDDING) {
                val bidder = if (frameCounter % 2 == 0) "BDR-1088" else "BDR-9901"
                val paddle = if (frameCounter % 2 == 0) "PADDLE-SA-019" else "PADDLE-SA-042"
                val pkt = bleEngine.createHardwareTriggerPacket(paddle, bidder)
                bleEngine.processIncomingPacket(pkt)
                val bidRes = auctionEngine.placeBid(currentLot, bidder, paddle)
                currentLot = bidRes.updatedLotState
            } else {
                currentLot = auctionEngine.advanceCountdown(currentLot, elapsedSeconds = 1)
            }

            val priceFmt = String.format("%.0f", currentLot.currentPriceSar)
            val liveStatus = "[ACTIVE_STREAMING] Frame #" + frameCounter + " (+" + jitter + "ms) | " +
                    "Lot: " + currentLot.lotId + " | High Bid: " + priceFmt + " SAR | " +
                    "Paddle: " + currentLot.highestPaddleId + " | Clock: " + currentLot.countdownRemainingSec + "s | " +
                    "Hammer: " + currentLot.status.name

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", liveStatus))
        }
    }
}
