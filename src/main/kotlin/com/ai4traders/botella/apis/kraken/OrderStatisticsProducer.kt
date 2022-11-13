package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.data.calculation.OrderStatisticCalculator
import com.ai4traders.botella.data.entities.MarketOrder
import com.ai4traders.botella.data.entities.OrderStatistic
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode
import com.ai4traders.botella.data.types.Numeric
import com.ai4traders.botella.data.types.OrderSideCode
import com.ai4traders.botella.data.types.OrderTypeCode
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.ktorm.entity.Entity
import java.math.BigDecimal
import javax.json.JsonArray

class OrderStatisticsProducer(
    private val product: TradableProduct,
    private val marketCode: MarketCode
): ActiveDataProducer<OrderStatistic>() {

    /** The Kraken ticker symbol for the product. */
    private val ticker = KrakenRestApi.tickerMap[product]!!

    /** The order statistic. */
    lateinit var orderStatisticCalculator: OrderStatisticCalculator

    lateinit var dataStamp: Instant


    override fun produceData() {
        var result = KrakenRestApi.fetchOrderBookData(ticker)
        if (result == null) {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return
            }
            result = KrakenRestApi.fetchOrderBookData(ticker)
        }
        if (result != null) {
            dataStamp = Clock.System.now()
            val contentArray = result.getJsonObject(ticker)
            val asksArray: JsonArray = contentArray.getJsonArray("asks")
            val bidsArray: JsonArray = contentArray.getJsonArray("bids")
            for (spread in KrakenRestApi.ORDER_BOOK_DEPTH_SPREADS) {
                calculateStatistics(asksArray, bidsArray, Numeric(spread))
            }
        }
    }

    fun calculateStatistics(asks: JsonArray, bids: JsonArray, depthSpread: Numeric) {
        orderStatisticCalculator = OrderStatisticCalculator(marketCode, product, depthSpread, dataStamp)
        val sortedAsks = asks.toList().sortedBy { Numeric((it as JsonArray).getString(0)) }
        for (i in sortedAsks.indices) {
            val ask = sortedAsks[i].asJsonArray()
            orderStatisticCalculator.processOrder(
                OrderSideCode.SELL, Numeric(ask.getString(0)), Numeric(ask.getString(1)),
                Instant.fromEpochMilliseconds(ask.getJsonNumber(2).bigDecimalValue().multiply(BigDecimal(1000)).toLong()))
        }
        val sortedBids = bids.toList().sortedBy { -Numeric((it as JsonArray).getString(0)) }
        for (i in sortedBids.indices) {
            val bid = sortedBids[i].asJsonArray()
            orderStatisticCalculator.processOrder(
                OrderSideCode.BUY, Numeric(bid.getString(0)), Numeric(bid.getString(1)),
                Instant.fromEpochMilliseconds(bid.getJsonNumber(2).bigDecimalValue().multiply(BigDecimal(1000)).toLong()))
        }
        notifyConsumers(orderStatisticCalculator.calculate())
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${MarketCode.KRAKEN}/${product}"
    }
}
