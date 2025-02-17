/*
 * Copyright 2020-2022 Neural Layer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("SpreadOperator")

package org.roboquant.iexcloud

import org.roboquant.common.Asset
import org.roboquant.common.Logging
import org.roboquant.feeds.*
import pl.zankowski.iextrading4j.api.marketdata.Trade
import pl.zankowski.iextrading4j.api.stocks.Quote
import pl.zankowski.iextrading4j.client.IEXCloudClient
import pl.zankowski.iextrading4j.client.socket.request.marketdata.deep.DeepAsyncResponse
import pl.zankowski.iextrading4j.client.sse.request.marketdata.TradesSseRequestBuilder
import pl.zankowski.iextrading4j.client.sse.request.stocks.QuoteInterval
import pl.zankowski.iextrading4j.client.sse.request.stocks.QuoteSseRequestBuilder
import java.time.Instant

/**
 * @see QuoteInterval
 */
typealias Interval = QuoteInterval

/**
 * Live feed of trades and/or quotes from IEXCloud Cloud for the subscribed assets.
 *
 * @property useMachineTime use machine time to tag events
 * @param configure additional configuration
 */
class IEXCloudLiveFeed(
    private val useMachineTime: Boolean = true,
    configure: IEXCloudConfig.() -> Unit = {}
) : LiveFeed(), AssetFeed {

    private val logger = Logging.getLogger(IEXCloudLiveFeed::class)
    private val config = IEXCloudConfig()
    private val client: IEXCloudClient
    private val assetMap = mutableMapOf<String, Asset>()

    /**
     * @see AssetFeed.assets
     */
    override val assets
        get() = assetMap.values.toSortedSet()

    init {
        config.configure()
        client = IEXCloud.getClient(config)
    }

    /**
     * Subscribe to one or more [assets]. If the symbol of the asset is found by IEXCloud, [PriceQuote] will be provided
     * as part of the feed.
     */
    fun subscribeQuotes(vararg symbols: String, interval: Interval = Interval.ONE_MINUTE) {
        require(symbols.isNotEmpty()) { "Subscribe to at least one symbol"}
        symbols.forEach { assetMap[it] = Asset(it) }
        logger.info { "Subscribing to assets $assets" }

        val request = QuoteSseRequestBuilder() // TopsSseRequestBuilder()
            .withSymbols(*symbols)
            .withQuoteInterval(interval)
            .build()

        client.subscribe(request, ::handleQuotes)
    }

    /**
     * Subscribe to the Trades for one or more [symbols]. If the symbol of the asset is found by IEXCloud,
     * [TradePrice] will be provided as part of the feed.
     */
    fun subscribeTrades(vararg symbols: String) {
        require(symbols.isNotEmpty()) { "Subscribe to at least one symbol"}
        symbols.forEach { assetMap[it] = Asset(it) }

        logger.info { "Subscribing to asset $assets" }

        val request = TradesSseRequestBuilder()
            .withSymbols(*symbols)
            .build()

        client.subscribe(request, ::handleTrades)
    }

    private fun handleTrades(trades: List<DeepAsyncResponse<Trade>>) {
        var now = Instant.now()
        var lastTime = Instant.MIN
        var actions = mutableListOf<Action>()
        for (trade in trades) {
            val asset = assetMap[trade.symbol]!!
            val data = trade.data
            if (!useMachineTime) now = Instant.ofEpochMilli(trade.data.timestamp)
            val action = TradePrice(asset, data.price.toDouble(), data.size.toDouble())
            if (now > lastTime && actions.isNotEmpty()) {
                val event = Event(actions, now)
                actions = mutableListOf()
                send(event)
            }
            lastTime = now
            actions.add(action)
        }

        if (actions.isNotEmpty()) {
            val event = Event(actions, now)
            send(event)
        }
    }

    // TODO correct for timezone
    private fun handleQuotes(quotes: List<Quote>) {
        logger.info { "Received callback with ${quotes.size} quotes" }
        var now = Instant.now()
        quotes.forEach {
            val asset = assetMap[it.symbol]!!
            val action = PriceQuote(
                asset,
                it.askPrice.toDouble(),
                it.askSize.toDouble(),
                it.bidPrice.toDouble(),
                it.askSize.toDouble()
            )
            if (!useMachineTime) now = Instant.ofEpochMilli(it.iexLastUpdated)
            val event = Event(listOf(action), now)
            send(event)
        }

    }

}
