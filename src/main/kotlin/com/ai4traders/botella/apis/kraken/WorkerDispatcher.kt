package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.apis.ApiType
import com.ai4traders.botella.apis.MarketWorkerDispatcher
import com.ai4traders.botella.data.types.MarketCode
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
            products = KrakenRestApi.tickerMap.keys,
            tradeApi = ApiType.REST,
            orderBookApi = ApiType.REST_STATS,
            refreshInterval = 5.seconds,
            ApiFactoryImpl(MarketCode.BINANCE_CHECK)
        )

    }
}
