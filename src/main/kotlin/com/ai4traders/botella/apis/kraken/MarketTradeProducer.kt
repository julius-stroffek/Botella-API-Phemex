package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.ExecutionConfiguration
import com.ai4traders.botella.data.entities.MarketTrade
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.entities.TradeStatistics
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode
import com.ai4traders.botella.data.types.Numeric
import com.ai4traders.botella.data.types.toEpochNanoseconds
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import java.math.BigDecimal
import javax.json.JsonArray

class MarketTradeProducer(
    private val product: TradableProduct,
    private val marketCode: MarketCode
): ActiveDataProducer<MarketTrade>() {

    /** The Kraken ticker symbol for the product. */
    private val ticker = KrakenRestApi.tickerMap[product]!!

    /** The since where to start fetch of the trade history.  */
    var since: String? = null

    override fun produceData() {
        if (since == null) {
            val maxStampQuery = ExecutionConfiguration.Companion.INSTANCE.database
                .from(TradeStatistics)
                .select(max(TradeStatistics.dataStamp))
                .where((TradeStatistics.product eq product) and (TradeStatistics.marketCode eq marketCode))
            since = if (maxStampQuery.rowSet.next()) {
                maxStampQuery.rowSet.getInstant(1)
                    ?.toKotlinInstant()
                    ?.toEpochNanoseconds(1)
                    ?.toString()
                    ?: KrakenRestApi.KRAKEN_DEFAULT_START_NANOS
            } else {
                KrakenRestApi.KRAKEN_DEFAULT_START_NANOS
            }
        }
        var result = KrakenRestApi.fetchTradeData(ticker, since)
        if (result == null) {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return
            }
            result = KrakenRestApi.fetchTradeData(ticker, since)
        }
        if (result != null) {
            val contentArray = result.getJsonArray(ticker)
            if (contentArray == null || contentArray.size < 1) {
                return
            }
            val last = result.getString("last")

            // Iterate over the trades
            for (jsonTrade in contentArray) {
                val trade = buildTrade(jsonTrade as JsonArray);
                notifyConsumers(trade)
            }

            // Update the value of since
            since = last
        }
    }

    private fun buildTrade(jsonTrade: JsonArray): MarketTrade {
        val result = Entity.create<MarketTrade>()
        result.marketCode = MarketCode.KRAKEN
        result.product = product
        result.price = Numeric(jsonTrade.getString(0))
        result.amount = Numeric(jsonTrade.getString(1))
        result.dataStamp = Instant.fromEpochMilliseconds(jsonTrade.getJsonNumber(2).bigDecimalValue().multiply(BigDecimal(1000)).toLong())
        result.tradeSide = KrakenRestApi.parseOrderSide(jsonTrade.getString(3))
        result.orderType = KrakenRestApi.parseOrderType(jsonTrade.getString(4))

        return result
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${MarketCode.KRAKEN}/${product}"
    }
}
