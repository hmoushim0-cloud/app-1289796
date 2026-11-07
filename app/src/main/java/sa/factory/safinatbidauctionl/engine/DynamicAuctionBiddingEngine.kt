package sa.factory.safinatbidauctionl.engine

import java.security.MessageDigest
import kotlin.math.max

/**
 * Stage of the live auction lot.
 */
enum class AuctionLotStatus {
    WAITING_OPENING,
    ACTIVE_BIDDING,
    GOING_ONCE,
    GOING_TWICE,
    HAMMER_FALL_SOLD,
    RESERVE_NOT_MET_PASSED
}

/**
 * Complete state representation of an active auction lot.
 */
data class AuctionLotState(
    val lotId: String,
    val title: String,
    val openingPriceSar: Double,
    val currentPriceSar: Double,
    val reservePriceSar: Double,
    val highestBidderId: String,
    val highestPaddleId: String,
    val countdownRemainingSec: Int,
    val totalBidsPlaced: Int,
    val status: AuctionLotStatus,
    val isAntiSnipeExtended: Boolean = false,
    val auditDigest: String = ""
)

/**
 * Result of submitting a bid into the live auction.
 */
data class BidSubmissionResult(
    val isAccepted: Boolean,
    val updatedLotState: AuctionLotState,
    val rejectedReason: String? = null
)

/**
 * Real-time Auction Bidding, Escalation Ladder, and Anti-Sniping Engine.
 */
class DynamicAuctionBiddingEngine {

    companion object {
        const val ANTI_SNIPE_WINDOW_SEC = 15
        const val ANTI_SNIPE_EXTENSION_SEC = 30
    }

    /**
     * Determines minimum bid increment in SAR conforming to Saudi auction ladders.
     */
    fun calculateMinimumIncrementSar(currentPriceSar: Double): Double {
        return when {
            currentPriceSar < 10000.0 -> 500.0
            currentPriceSar < 50000.0 -> 1000.0
            currentPriceSar < 250000.0 -> 5000.0
            else -> 10000.0
        }
    }

    /**
     * Processes an incoming bid from a paddle or digital client.
     */
    fun placeBid(
        currentState: AuctionLotState,
        bidderId: String,
        paddleId: String,
        offeredAmountSar: Double? = null
    ): BidSubmissionResult {
        if (currentState.status == AuctionLotStatus.HAMMER_FALL_SOLD ||
            currentState.status == AuctionLotStatus.RESERVE_NOT_MET_PASSED
        ) {
            return BidSubmissionResult(
                isAccepted = false,
                updatedLotState = currentState,
                rejectedReason = "Bidding closed for this lot"
            )
        }

        val minIncrement = calculateMinimumIncrementSar(currentState.currentPriceSar)
        val minimumNextBid = currentState.currentPriceSar + minIncrement
        val proposedAmount = offeredAmountSar ?: minimumNextBid

        if (proposedAmount < minimumNextBid) {
            return BidSubmissionResult(
                isAccepted = false,
                updatedLotState = currentState,
                rejectedReason = "Bid amount below minimum allowed increment"
            )
        }

        // Anti-sniping dynamic extension: if bid occurs in final 15 seconds, add 30 seconds
        val isSnipeTriggered = currentState.countdownRemainingSec <= ANTI_SNIPE_WINDOW_SEC
        val newCountdown = if (isSnipeTriggered) {
            currentState.countdownRemainingSec + ANTI_SNIPE_EXTENSION_SEC
        } else {
            currentState.countdownRemainingSec
        }

        val updatedBidsCount = currentState.totalBidsPlaced + 1
        val digest = generateBidAuditDigest(currentState.lotId, proposedAmount, bidderId, paddleId, updatedBidsCount)

        val updatedState = currentState.copy(
            currentPriceSar = proposedAmount,
            highestBidderId = bidderId,
            highestPaddleId = paddleId,
            countdownRemainingSec = newCountdown,
            totalBidsPlaced = updatedBidsCount,
            status = AuctionLotStatus.ACTIVE_BIDDING,
            isAntiSnipeExtended = isSnipeTriggered,
            auditDigest = digest
        )

        return BidSubmissionResult(
            isAccepted = true,
            updatedLotState = updatedState,
            rejectedReason = null
        )
    }

    /**
     * Ticks auction countdown and advances hammer status stages.
     */
    fun advanceCountdown(state: AuctionLotState, elapsedSeconds: Int = 1): AuctionLotState {
        val remaining = max(0, state.countdownRemainingSec - elapsedSeconds)

        val newStatus = when {
            remaining == 0 -> {
                if (state.currentPriceSar >= state.reservePriceSar) {
                    AuctionLotStatus.HAMMER_FALL_SOLD
                } else {
                    AuctionLotStatus.RESERVE_NOT_MET_PASSED
                }
            }
            remaining <= 5 -> AuctionLotStatus.GOING_TWICE
            remaining <= 10 -> AuctionLotStatus.GOING_ONCE
            else -> AuctionLotStatus.ACTIVE_BIDDING
        }

        return state.copy(
            countdownRemainingSec = remaining,
            status = newStatus
        )
    }

    private fun generateBidAuditDigest(
        lotId: String,
        amount: Double,
        bidderId: String,
        paddleId: String,
        bidSeq: Int
    ): String {
        val amtFmt = String.format("%.2f", amount)
        val input = "AUCTION-BID::$lotId::$amtFmt::$bidderId::$paddleId::SEQ-$bidSeq"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
