package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.apis.MarketWorkerDispatcher
import com.ai4traders.botella.apis.MarketWorkerDispatcher.ApiType
import com.ai4traders.botella.apis.MarketWorkerDispatcher.ProductConfiguration
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.types.MarketCode
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class WorkerDispatcher() {
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    /** The list of products this worker creates downloaders for. */
    val products = listOf(
        ProductConfiguration(
            product = TradableProduct.BTCUSD,
            tradeApi = ApiType.REST,
            orderBookApi = ApiType.REST_STATS
        ),
        ProductConfiguration(
            product = TradableProduct.ETHUSD,
            tradeApi = ApiType.REST,
            orderBookApi = ApiType.REST_STATS
        ),
        ProductConfiguration(
            product = TradableProduct.ETHBTC,
            tradeApi = ApiType.REST,
            orderBookApi = ApiType.REST_STATS
        ),
        ProductConfiguration(
            product = TradableProduct.XRPUSD,
            tradeApi = ApiType.REST,
            orderBookApi = ApiType.REST_STATS
        ),
    )

    @Autowired
    lateinit var marketWorkerDispatcher: MarketWorkerDispatcher

    @PostConstruct
    fun init() {
        marketWorkerDispatcher.createWorkers(products, ApiFactoryImpl())
    }
}
