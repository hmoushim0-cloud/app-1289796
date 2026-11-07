package sa.factory.safinatbidauctionl.engine

import java.security.MessageDigest
import kotlin.math.max

/**
 * BLE Bidding Paddle Button Press Type.
 */
enum class PaddleButtonAction {
    SINGLE_TAP_INCREMENT,
    LONG_PRESS_MAX_BID,
    CANCEL_RETRACT
}

/**
 * Hardware Feedback Mode sent to the physical bidding paddle.
 */
enum class PaddleHapticFeedback {
    BID_ACKNOWLEDGED,
    WINNING_BIDDER,
    OUTBID_WARNING,
    HAMMER_FALL_SOLD,
    REJECTED_INVALID
}

/**
 * Incoming GATT packet payload from BLE hardware paddle.
 */
data class BlePaddlePacket(
    val paddleId: String,
    val bidderId: String,
    val sequenceNumber: Long,
    val action: PaddleButtonAction,
    val txTimestampMs: Long,
    val rxTimestampMs: Long = System.currentTimeMillis(),
    val batteryPct: Int = 96,
    val rssiDbm: Int = -52
) {
    val latencyMs: Long get() = max(1L, rxTimestampMs - txTimestampMs)
}

/**
 * Evaluated result of a hardware paddle bid trigger.
 */
data class PaddleTriggerResult(
    val isProcessed: Boolean,
    val paddleId: String,
    val latencyMs: Long,
    val feedback: PaddleHapticFeedback,
    val packetDigest: String
)

/**
 * Bluetooth Low Energy GATT Bidding Paddle Driver.
 * Emulates GATT Service 0xFF10 with sub-100ms hardware response.
 */
class BleBiddingPaddleGattEngine {

    companion object {
        const val GATT_SERVICE_UUID = "0000FF10-0000-1000-8000-00805F9B34FB"
        const val GATT_CHAR_BID_EVENT = "0000FF11-0000-1000-8000-00805F9B34FB"
        const val GATT_CHAR_HAPTIC_OUT = "0000FF12-0000-1000-8000-00805F9B34FB"
        const val MAX_TOLERATED_LATENCY_MS = 150L
    }

    private var lastSequenceNumber: Long = 0L

    /**
     * Ingests and processes a BLE GATT notification packet.
     */
    fun processIncomingPacket(packet: BlePaddlePacket): PaddleTriggerResult {
        // Enforce sequence progression to prevent replay attacks
        if (packet.sequenceNumber <= lastSequenceNumber) {
            val digest = generatePacketDigest(packet, "REPLAY_REJECTED")
            return PaddleTriggerResult(
                isProcessed = false,
                paddleId = packet.paddleId,
                latencyMs = packet.latencyMs,
                feedback = PaddleHapticFeedback.REJECTED_INVALID,
                packetDigest = digest
            )
        }
        lastSequenceNumber = packet.sequenceNumber

        val feedback = when (packet.action) {
            PaddleButtonAction.SINGLE_TAP_INCREMENT -> PaddleHapticFeedback.BID_ACKNOWLEDGED
            PaddleButtonAction.LONG_PRESS_MAX_BID -> PaddleHapticFeedback.WINNING_BIDDER
            PaddleButtonAction.CANCEL_RETRACT -> PaddleHapticFeedback.OUTBID_WARNING
        }

        val digest = generatePacketDigest(packet, feedback.name)
        return PaddleTriggerResult(
            isProcessed = true,
            paddleId = packet.paddleId,
            latencyMs = packet.latencyMs,
            feedback = feedback,
            packetDigest = digest
        )
    }

    /**
     * Generates a realistic BLE packet from a physical paddle button press.
     */
    fun createHardwareTriggerPacket(
        paddleId: String = "PADDLE-SA-042",
        bidderId: String = "BDR-9901",
        action: PaddleButtonAction = PaddleButtonAction.SINGLE_TAP_INCREMENT
    ): BlePaddlePacket {
        val now = System.currentTimeMillis()
        val txTime = now - 18L // 18ms BLE transmission delay
        val seq = lastSequenceNumber + 1
        return BlePaddlePacket(
            paddleId = paddleId,
            bidderId = bidderId,
            sequenceNumber = seq,
            action = action,
            txTimestampMs = txTime,
            rxTimestampMs = now
        )
    }

    private fun generatePacketDigest(packet: BlePaddlePacket, status: String): String {
        val input = "BLE-PADDLE::$status::" + packet.paddleId + "::SEQ-" + packet.sequenceNumber + "::" + packet.rxTimestampMs
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
