package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.data.consumers.PassiveDataConsumer
import com.ai4traders.botella.data.producers.DataProducer
import com.ai4traders.botella.data.entities.MarketTrade
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.entities.TradeStatistic
import com.ai4traders.botella.data.types.*
import com.ai4traders.botella.data.types.OrderSideType.*
import com.ai4traders.botella.exceptions.ProductMismatch
import com.ai4traders.botella.utils.Utility
import kotlinx.datetime.Clock
import org.ktorm.entity.Entity
import kotlin.time.Duration.Companion.seconds

class TradeStatisticsProducer(
    val product: TradableProduct,
    val marketCode: MarketCode
): PassiveDataConsumer<MarketTrade>, DataProducer<TradeStatistic>() {
    val INTERVAL_LENGTH = 5.seconds
    var tradeStatistics: TradeStatistic = Entity.create()

    override fun consume(trade: MarketTrade, producer: DataProducer<MarketTrade>) {
        if (trade.product != product)
            throw ProductMismatch("The product in trade '${trade.product}' does not match the product '${product}' in statistics.")
        if (trade.marketCode != marketCode)
            throw ProductMismatch("The market in trade '${trade.marketCode}' does not match the market '${marketCode}' in statistics.")

        val useCurrentStats = belongsTo(tradeStatistics, trade)
        if (!useCurrentStats) {
            completeStatistics(tradeStatistics)
        }
        if (tradeStatistics.tradeCount == 0) with (tradeStatistics) {
            tradeCount = 1
            marketCode = trade.marketCode
            product = trade.product
            creationStamp = Clock.System.now()
            volume = trade.price * trade.amount
            amount = trade.amount
            minStamp = trade.dataStamp
            maxStamp = trade.dataStamp
            minPrice = trade.price
            maxPrice = trade.price
            lastPrice = trade.price
            avgPrice = trade.price
            when (OrderSideType.fromSideType(trade.tradeSide, trade.orderType)) {
                BM -> {
                    buyMarketAmount = amount
                    buyAmount = amount
                }
                BL -> {
                    buyLimitAmount = amount
                    buyAmount = amount
                }
                SM -> {
                    sellMarketAmount = amount
                    sellAmount = amount
                }
                SL -> {
                    sellLimitAmount = amount
                    sellAmount = amount
                }
            }
        } else with (tradeStatistics) {
            tradeCount++
            volume += trade.price * trade.amount
            amount += trade.amount
            minStamp = min(minStamp, trade.dataStamp)
            maxStamp = max(maxStamp, trade.dataStamp)
            minPrice = min(minPrice, trade.price)
            maxPrice = max(maxPrice, trade.price)
            lastPrice = if (maxStamp < trade.dataStamp) trade.price else lastPrice
            when (OrderSideType.fromSideType(trade.tradeSide, trade.orderType)) {
                BM -> {
                    buyMarketAmount += amount
                    buyAmount += amount
                }
                BL -> {
                    buyLimitAmount += amount
                    buyAmount += amount
                }
                SM -> {
                    sellMarketAmount += amount
                    sellAmount += amount
                }
                SL -> {
                    sellLimitAmount += amount
                    sellAmount += amount
                }
            }
        }
    }

    private fun belongsTo(tradeStatistics: TradeStatistic, trade: MarketTrade): Boolean {
        if (tradeStatistics.tradeCount == 0)
            return true
        return tradeStatistics.minStamp <= trade.dataStamp &&
                trade.dataStamp < (tradeStatistics.minStamp + INTERVAL_LENGTH)
    }

    private fun completeStatistics(tradeStatistics: TradeStatistic) {
        with (tradeStatistics) {
            val millis = maxStamp.toEpochMilliseconds() - minStamp.toEpochMilliseconds()
            dataStamp = maxStamp
            if (!amount.isZero()) {
                avgPrice = volume / amount
            }
            if (millis > 1000) {
                speedInst1 = amount / (millis/1000)
                speedInst2 = volume / (millis/1000)
            }
            buyAmount = buyAmount ?: Numeric.ZERO
            buyMarketAmount = buyMarketAmount ?: Numeric.ZERO
            buyLimitAmount = buyLimitAmount ?: Numeric.ZERO
            sellAmount = sellAmount ?: Numeric.ZERO
            sellMarketAmount = sellMarketAmount ?: Numeric.ZERO
            sellLimitAmount = sellLimitAmount ?: Numeric.ZERO
        }
        notifyConsumers(tradeStatistics)
        this.tradeStatistics = Entity.create()
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${marketCode}/${product}"
    }
}