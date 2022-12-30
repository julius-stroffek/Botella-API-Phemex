package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.apis.*
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.types.MarketCode
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import kotlin.time.Duration.Companion.seconds

@Service
class WorkerDispatcher() {
    private val marketCode = MarketCode.PHEMEX
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    @Autowired
    lateinit var marketWorkerDispatcher: MarketWorkerDispatcher

    @PostConstruct
    fun init() {
        val products = ProductConfiguration.configureProducts(
            products = PhemexWebSocketApi.tickerMap.keys,
            marketCode = MarketCode.PHEMEX,
            tradeApi = ApiType.WEB_SOCKETS,
            orderBookApi = ApiType.WEB_SOCKETS,
            refreshInterval = 5.seconds,
            //flags = ProductConfigurationFlags.of(ProductConfigurationFlag.VALIDATE_ORDER_BOOK_PRICE_GAP)
        )
        val factory = ApiFactoryImpl()
        marketWorkerDispatcher.createTradeWorkers(products, MarketTradeWebSocketProducer(PhemexWebSocketApi.tickerMap.keys, marketCode), factory)
        marketWorkerDispatcher.createOrderWorkers(products, OrderWebSocketProducer(PhemexWebSocketApi.tickerMap.keys, marketCode), factory)
    }
}
