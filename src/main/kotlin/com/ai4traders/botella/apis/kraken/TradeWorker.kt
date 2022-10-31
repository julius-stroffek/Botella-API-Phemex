package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.data.ProducerConsumerDispatcher
import com.ai4traders.botella.data.consumers.PersistenceConsumer
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.types.MarketCode
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class TradeWorker() {
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    @Autowired
    lateinit var producerConsumerDispatcher: ProducerConsumerDispatcher

    @Autowired
    lateinit var persistentConsumer: PersistenceConsumer

    var tradeProducerBTCUSD = MarketTradeProducer(TradableProduct.BTCUSD, MarketCode.KRAKEN)
    var statsProducerBTCUSD = TradeStatisticsProducer(TradableProduct.BTCUSD, MarketCode.KRAKEN)

    var tradeProducerETHUSD = MarketTradeProducer(TradableProduct.ETHUSD, MarketCode.KRAKEN)
    var statsProducerETHUSD = TradeStatisticsProducer(TradableProduct.ETHUSD, MarketCode.KRAKEN)

    var tradeProducerETHBTC = MarketTradeProducer(TradableProduct.ETHBTC, MarketCode.KRAKEN)
    var statsProducerETHBTC = TradeStatisticsProducer(TradableProduct.ETHBTC, MarketCode.KRAKEN)

    @PostConstruct
    fun init() {
        producerConsumerDispatcher.register(statsProducerBTCUSD, tradeProducerBTCUSD)
        producerConsumerDispatcher.register(persistentConsumer, statsProducerBTCUSD)

        producerConsumerDispatcher.register(statsProducerETHUSD, tradeProducerETHUSD)
        producerConsumerDispatcher.register(persistentConsumer, statsProducerETHUSD)

        producerConsumerDispatcher.register(statsProducerETHBTC, tradeProducerETHBTC)
        producerConsumerDispatcher.register(persistentConsumer, statsProducerETHBTC)
    }


    @Scheduled(fixedRate = 5000)
    fun fetchTradesBTCUSD() {
        try {
            tradeProducerBTCUSD.fetchTrades()
        } catch (t: Throwable) {
            logger.error("Failed to execute the scheduled job!", t)
        }
    }

    @Scheduled(fixedRate = 5000)
    fun fetchTradesETHUSD() {
        try {
            tradeProducerETHUSD.fetchTrades()
        } catch (t: Throwable) {
            logger.error("Failed to execute the scheduled job!", t)
        }
    }

    @Scheduled(fixedRate = 5000)
    fun fetchTradesETHBTC() {
        try {
            tradeProducerETHBTC.fetchTrades()
        } catch (t: Throwable) {
            logger.error("Failed to execute the scheduled job!", t)
        }
    }
}