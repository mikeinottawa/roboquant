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

@file:Suppress("KotlinConstantConditions")

package org.roboquant.samples

import org.roboquant.Roboquant
import org.roboquant.common.*
import org.roboquant.iexcloud.Range
import org.roboquant.loggers.MemoryLogger
import org.roboquant.metrics.AccountMetric
import org.roboquant.metrics.PositionMetric
import org.roboquant.metrics.ProgressMetric
import org.roboquant.polygon.PolygonHistoricFeed
import org.roboquant.strategies.EMAStrategy
import org.roboquant.yahoo.YahooHistoricFeed

fun feedIEX() {
    val feed = org.roboquant.iexcloud.IEXCloudHistoricFeed()
    feed.retrieve("AAPL", "GOOGL", "FB", range = Range.TWO_YEARS)

    val strategy = EMAStrategy(10, 30)
    val roboquant = Roboquant(strategy, AccountMetric())
    roboquant.run(feed)
    println(roboquant.broker.account.summary())
}

fun feedIEXLive() {
    val feed = org.roboquant.iexcloud.IEXCloudLiveFeed()
    feed.subscribeTrades("AAPL", "GOOG")
    val strategy = EMAStrategy()
    val roboquant = Roboquant(strategy, AccountMetric(), ProgressMetric())
    roboquant.run(feed, Timeframe.next(5.minutes))
    println(roboquant.broker.account.summary())
}

fun feedYahoo() {
    val feed = YahooHistoricFeed()
    val last300Days = Timeframe.past(300.days)
    feed.retrieve("AAPL", "GOOG", timeframe = last300Days)
    val strategy = EMAStrategy()
    val logger = MemoryLogger()
    val roboquant = Roboquant(strategy, AccountMetric(), PositionMetric(), logger = logger)
    roboquant.run(feed)
    logger.summary(10)
}

fun feedPolygon() {

    // Get the feed
    val feed = PolygonHistoricFeed()
    println(feed.availableAssets.summary())
    val tf = Timeframe.fromYears(2021, 2022)
    feed.retrieve("IBM", "AAPL", timeframe = tf)
    println(feed.assets)
    println(feed.timeline.size)

    // Use the feed
    val strategy = EMAStrategy()
    val roboquant = Roboquant(strategy)
    roboquant.run(feed)
    val account = roboquant.broker.account
    println(account.fullSummary())
}

fun main() {
    when ("POLYGON") {
        "IEXCloud" -> feedIEX()
        "IEX_LIVE" -> feedIEXLive()
        "YAHOO" -> feedYahoo()
        "POLYGON" -> feedPolygon()
    }
}