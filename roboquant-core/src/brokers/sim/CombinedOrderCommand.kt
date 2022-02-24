package org.roboquant.brokers.sim

import org.roboquant.common.iszero
import org.roboquant.orders.*
import java.time.Instant

internal class OCOOrderCommand(order: OneCancelsOtherOrder) : OrderCommand<OneCancelsOtherOrder>(order) {

    private val first = ExecutionEngine.getOrderCommand(order.first)
    private val second = ExecutionEngine.getOrderCommand(order.second)
    private var active = 0

    override fun execute(pricing: Pricing, time: Instant): List<Execution> {
        update(time)

        if (active == 0 || active == 1) {
            val result = first.execute(pricing, time)
            if (result.isNotEmpty()) {
                active = 1
                status = first.status
                return result
            }

        }

        if (active == 0 || active == 2) {
            val result = second.execute(pricing, time)
            if (result.isNotEmpty()) {
                active = 2
                status = first.status
                return result
            }
        }

        return emptyList()
    }
}


internal class OTOOrderCommand(order: OneTriggersOtherOrder) : OrderCommand<OneTriggersOtherOrder>(order) {

    private val first = ExecutionEngine.getOrderCommand(order.first)
    private val second = ExecutionEngine.getOrderCommand(order.second)

    override fun execute(pricing: Pricing, time: Instant): List<Execution> {
        update(time)
        val result = mutableListOf<Execution>()

        if (first.status.open) {
            result.addAll(first.execute(pricing, time))
            if (first.status.aborted) status = first.status
        }

        if (first.status == OrderStatus.COMPLETED) {
            result.addAll(second.execute(pricing, time))
            status = second.status
        }

        return result
    }
}


internal class BracketOrderCommand(order: BracketOrder) : OrderCommand<BracketOrder>(order) {

    private val main = ExecutionEngine.getOrderCommand(order.entry) as SingleOrderCommand<*>
    private val profit = ExecutionEngine.getOrderCommand(order.takeProfit) as SingleOrderCommand<*>
    private val loss = ExecutionEngine.getOrderCommand(order.stopLoss) as SingleOrderCommand<*>

    override fun execute(pricing: Pricing, time: Instant): List<Execution> {
        update(time)
        if (main.status.open) return main.execute(pricing, time)

        val executions = mutableListOf<Execution>()

        if (loss.fill.iszero) executions.addAll(profit.execute(pricing, time))
        if (profit.fill.iszero) executions.addAll(loss.execute(pricing, time))

        val remaining = main.qty + loss.fill + profit.fill
        if (remaining.iszero) close(OrderStatus.COMPLETED, time)
        return executions
    }

}