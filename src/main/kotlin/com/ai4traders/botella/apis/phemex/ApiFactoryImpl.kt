package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.apis.ApiFactory
import com.ai4traders.botella.data.entities.*
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode

class ApiFactoryImpl(
    override val marketCode: MarketCode = MarketCode.PHEMEX
): ApiFactory {

    override fun createOrderStatsRestProducer(product: TradableProduct): ActiveDataProducer<OrderStatistic> {
        TODO("Not yet implemented")
    }

    override fun createOrderStatsWebSocketProducer(product: TradableProduct): ActiveDataProducer<OrderStatistic> {
        TODO("Not yet implemented")
    }

    override fun createOrderWebSocketProducer(product: TradableProduct): ActiveDataProducer<List<MarketOrder>> {
        return OrderWebSocketProducer(listOf(product), marketCode)
    }

    override fun createTradeRestProducer(product: TradableProduct): ActiveDataProducer<List<MarketTrade>> {
        TODO("Not yet implemented")
    }

    override fun createTradeStatsRestProducer(product: TradableProduct): ActiveDataProducer<TradeStatistic> {
        TODO("Not yet implemented")
    }

    override fun createTradeStatsWebSocketProducer(product: TradableProduct): ActiveDataProducer<TradeStatistic> {
        TODO("Not yet implemented")
    }

    override fun createTradeWebSocketProducer(product: TradableProduct): ActiveDataProducer<List<MarketTrade>> {
        return MarketTradeWebSocketProducer(listOf(product), marketCode)
    }
}