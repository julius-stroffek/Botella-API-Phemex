package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.apis.ApiType
import com.ai4traders.botella.apis.MarketWorkerDispatcher
import com.ai4traders.botella.data.entities.TradableProduct
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import kotlin.time.Duration.Companion.seconds

@Service
class WorkerDispatcher() {
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    @Autowired
    lateinit var marketWorkerDispatcher: MarketWorkerDispatcher

    @PostConstruct
    fun init() {
        marketWorkerDispatcher.createWorkers(
            product = TradableProduct.BTCUSD, //PhemexWebSocketApi.tickerMap.keys,
            tradeApi = ApiType.WEB_SOCKETS,
            orderBookApi = ApiType.NONE,
            refreshInterval = 5.seconds,
            ApiFactoryImpl()
        )

    }
}
