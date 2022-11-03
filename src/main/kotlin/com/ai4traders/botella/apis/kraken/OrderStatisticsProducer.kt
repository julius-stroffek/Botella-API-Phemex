package com.ai4traders.botella.apis.kraken

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

    /** The since where to start fetch of the trade history. */
    var since: String? = null

    /** The order statistic. */
    var orderStatistic = Entity.create<OrderStatistic>()

    lateinit var askPriceRatio: Numeric
    lateinit var bidPriceRatio: Numeric
    lateinit var dataStamp: Instant
    var maxAskPrice: Numeric? = null
    var minBidPrice: Numeric? = null

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
        askPriceRatio = Numeric.ONE + depthSpread
        bidPriceRatio = Numeric.ONE - depthSpread
        maxAskPrice = null
        minBidPrice = null
        with(orderStatistic) {
            marketCode = this@OrderStatisticsProducer.marketCode
            product = this@OrderStatisticsProducer.product
            spread = depthSpread
            dataStamp = this@OrderStatisticsProducer.dataStamp
            askOrderCount = 0
            bidOrderCount = 0
            askVolume = Numeric.ZERO
            bidVolume = Numeric.ZERO
            askAmount = Numeric.ZERO
            bidAmount = Numeric.ZERO
            bidMinStamp = Instant.fromEpochMilliseconds(Long.MAX_VALUE)
            bidMaxStamp = Instant.fromEpochMilliseconds(Long.MIN_VALUE)
            askMaxStamp = Instant.fromEpochMilliseconds(Long.MIN_VALUE)
            askMinStamp = Instant.fromEpochMilliseconds(Long.MAX_VALUE)
        }
        for (i in asks.indices) {
            processOrder(OrderSideCode.SELL, asks.getJsonArray(i))
        }
        for (i in bids.indices) {
            processOrder(OrderSideCode.BUY, bids.getJsonArray(i))
        }
        with(orderStatistic) {
            creationStamp = Clock.System.now()
            priceGap = askMinPrice!! - bidMaxPrice!!
        }
        notifyConsumers(orderStatistic)
        this.orderStatistic = Entity.create()
    }

    private fun processOrder(orderSideCode: OrderSideCode, json: JsonArray): Boolean {
        val order = Entity.create<MarketOrder>()
        with (order) {
            orderType = OrderTypeCode.LIMIT
            tradeSide = orderSideCode
            price = Numeric(json.getString(0))
            amount = Numeric(json.getString(1))
            dataStamp = Instant.fromEpochMilliseconds(json.getJsonNumber(2).bigDecimalValue().multiply(BigDecimal(1000)).toLong())
        }
        with (orderStatistic) {
            when (orderSideCode) {
                OrderSideCode.SELL -> {
                    if (maxAskPrice != null && order.price > maxAskPrice!!)
                        return false
                    askOrderCount++
                    if (askMinPrice == null || askMinPrice!! > order.price) {
                        askMinPrice = order.price
                        maxAskPrice = askMinPrice!! * askPriceRatio
                    }
                    if (askMaxPrice == null || askMaxPrice!! < order.price)
                        askMaxPrice = order.price
                    if (askMinStamp == null || order.dataStamp < askMinStamp)
                        askMinStamp = order.dataStamp
                    if (askMaxStamp == null || order.dataStamp > askMaxStamp)
                        askMaxStamp = order.dataStamp
                    askAmount += order.amount
                    askVolume += order.amount * order.price
                }
                OrderSideCode.BUY -> {
                    if (minBidPrice != null && order.price < minBidPrice!!) {
                        return false
                    }
                    bidOrderCount++
                    if (bidMinPrice == null || bidMinPrice!! > order.price)
                        bidMinPrice = order.price
                    if (bidMaxPrice == null || bidMaxPrice!! < order.price) {
                        bidMaxPrice = order.price
                        minBidPrice = bidMaxPrice!! * bidPriceRatio
                    }
                    if (bidMinStamp == null || order.dataStamp < bidMinStamp)
                        bidMinStamp = order.dataStamp
                    if (bidMaxStamp == null || order.dataStamp > bidMaxStamp)
                        bidMaxStamp = order.dataStamp
                    bidAmount += order.amount
                    bidVolume += order.amount * order.price
                }
            }
        }

        return true
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${MarketCode.KRAKEN}/${product}"
    }
}
