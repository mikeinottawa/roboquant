package org.roboquant.orders

import org.roboquant.common.Asset
import org.roboquant.orders.OrderStatus.*
import java.time.Instant
import org.roboquant.brokers.Account

/**
 * Order State keeps track of the execution state of an order. After an order is placed at a broker, the OrderState
 * informs you what is happening with that order. The most common way to access this information is through
 * [Account.openOrders] and [Account.closedOrders]
 *
 * This is a open class and can be extended by more advanced implementations.
 *
 * @property order The underying order
 * @property status The latest status
 * @property openedAt When was the order execution opened
 * @property closedAt When was the order execution closed
 * @constructor Create new Order state
 */
open class OrderState(
    val order: Order,
    val status: OrderStatus = INITIAL,
    val openedAt: Instant = Instant.MIN,
    val closedAt: Instant = Instant.MAX
)  {

    /**
     * Is the order still open
     */
    val open: Boolean
        get() = status.open

    /**
     * Is the order closed. If this returns true, the order will be no further processed.
     */
    val closed: Boolean
        get() = status.closed

    /**
     * The underlying asset
     */
    val asset: Asset
        get() = order.asset

    /**
     * The underlying order id
     */
    val id: Int
        get() = order.id

    /**
     * Update the order state and return the new order state (if applicable)
     *
     * @param time
     * @param newStatus
     * @return
     */
    fun copy(time: Instant, newStatus: OrderStatus = ACCEPTED) : OrderState {
        return if (newStatus === ACCEPTED && status === INITIAL) {
            OrderState(order, newStatus, time)
        } else if (newStatus.closed && status.open) {
            val openTime = if (openedAt === Instant.MIN) time else openedAt
            OrderState(order, newStatus, openTime, time)
        } else {
            this
        }
    }



}



/**
 * The status an order can be in. The  flow is straight forward:
 *
 *  - [INITIAL] -> [ACCEPTED] -> [COMPLETED] | [CANCELLED] | [EXPIRED]
 *  - [INITIAL] -> [REJECTED]
 *
 *  At any given time an [OrderState] is either [open] or [closed] state. Once an order reaches a [closed] state,
 *  it cannot be opened again and will not be further processed.
 */
enum class OrderStatus {

    /**
     * State of an order that has just been created. It will remain in this state until it is either
     * rejected or accepted.
     */
    INITIAL,

    /**
     * The order has been received, validated and accepted.
     */
    ACCEPTED,

    /**
     * The order has been successfully completed. This is an end state
     */
    COMPLETED,

    /**
     * The order was cancelled, normally by a cancellation order. This is an end state
     */
    CANCELLED,

    /**
     *  The order has expired, normally triggered by a time-in-force policy. This is an end state
     */
    EXPIRED,

    /**
     *  The order has been rejected. This is an end state and typically occurs when:
     *  - The order is not valid, for example you try to short an asset while that is not allowed
     *  - You don't have enough buyingPower
     *  - The provided asset is recognised or cannot be traded on your account
     */
    REJECTED;

    /**
     * Has the order been aborted. That implies it is in one of the following three "error" end states:
     * [CANCELLED], [EXPIRED], [REJECTED]
     */
    val aborted: Boolean
        get() = this === CANCELLED || this === EXPIRED || this === REJECTED

    /**
     * Is the order closed. This means it has reached an end-state that doesn't allow for any more trading. This implies
     * it is in one of these four possible end-states: [COMPLETED], [CANCELLED], [EXPIRED] or [REJECTED].
     */
    val closed: Boolean
        get() = this === COMPLETED || this === CANCELLED || this === EXPIRED || this === REJECTED


    /**
     * Is the order in an open state, so [INITIAL] or [ACCEPTED]
     */
    val open: Boolean
        get() = this === INITIAL || this === ACCEPTED


}