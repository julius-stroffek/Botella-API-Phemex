package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.apis.ApiFactory
import com.ai4traders.botella.data.entities.*
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode

class ApiFactoryImpl(
    override val marketCode: MarketCode = MarketCode.KRAKEN
): ApiFactory {

    override fun createOrderStatsRestProducer(product: TradableProduct): ActiveDataProducer<OrderStatistic> {
        return OrderStatisticsProducer(product, marketCode)
    }

    override fun createOrderStatsWebSocketProducer(product: TradableProduct): ActiveDataProducer<OrderStatistic> {
        TODO("Not yet implemented")
    }

    override fun createOrderWebSocketProducer(product: TradableProduct): ActiveDataProducer<MarketOrder> {
        TODO("Not yet implemented")
    }

    override fun createTradeRestProducer(product: TradableProduct): ActiveDataProducer<MarketTrade> {
        return MarketTradeProducer(product, marketCode)
    }

    override fun createTradeStatsRestProducer(product: TradableProduct): ActiveDataProducer<TradeStatistic> {
        TODO("Not yet implemented")
    }

    override fun createTradeStatsWebSocketProducer(product: TradableProduct): ActiveDataProducer<TradeStatistic> {
        TODO("Not yet implemented")
    }

    override fun createTradeWebSocketProducer(product: TradableProduct): ActiveDataProducer<MarketTrade> {
        TODO("Not yet implemented")
    }
}