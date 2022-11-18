package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.data.consumers.Signal
import com.ai4traders.botella.data.entities.MarketOrder
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode
import com.ai4traders.botella.data.types.Numeric
import com.ai4traders.botella.data.types.OrderSideCode
import com.ai4traders.botella.utils.HttpUtils
import com.ai4traders.botella.utils.WebSocketClientWrapper
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.json.*
import org.ktorm.dsl.div
import org.ktorm.entity.Entity
import java.net.ProtocolException

class OrderWebSocketProducer(
    private val products: Collection<TradableProduct>,
    private val marketCode: MarketCode
) : ActiveDataProducer<MarketOrder>() {
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    val tickers = products.map { PhemexWebSocketApi.tickerMap[it] }
    val productMap = PhemexWebSocketApi.tickerMap.entries.map {it.value to it.key}.toMap()

    var ready = false

    override fun produceData() {
        while (true) {
            val clientWrapper = WebSocketClientWrapper()
            try {
                runBlocking {
                    clientWrapper.client.webSocket(
                        urlString = "${PhemexWebSocketApi.WEBSOCKET_URL}"
                    ) {
                        for (ticker in tickers) {
                            val subscription =
                                """{
                                  "id": 1,
                                  "method": "orderbook.subscribe",
                                  "params": [
                                    "${ticker}",
                                    true
                                  ]
                                }""".replace(" ", "")
                            logger.info("Sending Phemex subscription: ${subscription}")
                            send(subscription)
                        }
                        while (true) {
                            val frame = incoming.receive()
                            clientWrapper.activity()
                            val json = (frame as Frame.Text).readText()
                            val jsonStructure = JSONTokener(json).nextValue();
                            when (jsonStructure) {
                                is JSONObject -> {
                                    if (jsonStructure.has("result")) {
                                        logger.info("Received response: ${json}")
                                    } else {
                                        val orders = buildOrders(jsonStructure)
                                        for (order in orders) {
                                            notifyConsumers(order)
                                        }
                                        if (!ready) {
                                            ready = true
                                            signal(Signal(Signal.SignalType.READY, orders[0].product, orders[0].marketCode))
                                        }
                                    }
                                }
                                is JSONArray -> {
                                    println("Received response: '${json}'")
                                }
                            }
                        }
                    }
                }
            } catch (e: ProtocolException) {
                logger.error("Failed to process the websocket data for market '${marketCode}' and product '${products.joinToString(", ")}'!", e)
            } catch (e: Exception) {
                logger.error("Failed to process the websocket data for market '${marketCode}' and product '${products.joinToString(", ")}'!", e)
            }
        }
    }


    private fun buildOrders(json: JSONObject): List<MarketOrder> {
        val result = mutableListOf<MarketOrder>()
        val timestamp = Instant.fromEpochMilliseconds(json.getNumber("timestamp").toLong().div(1_000_000))
        val book = json.getJSONObject("book") as JSONObject
        val product = productMap[json.getString("symbol")]!!
        var bids = (
            if (book.has("bids"))
                book["bids"]
            else null
        ) as JSONArray?
        var asks = (
            if (book.has("asks"))
                book["asks"]
            else null
        ) as JSONArray?
        if (bids != null) {
            for (it in bids) {
                val bid = it as JSONArray
                val order = Entity.create<MarketOrder>()
                order.product = product
                order.marketCode = marketCode
                order.tradeSide = OrderSideCode.BUY
                order.price = Numeric(bid[0].toString()) / Numeric(100_000_000)
                order.amount = Numeric(bid[1].toString()) / Numeric(100_000_000)
                order.dataStamp = timestamp
                result.add(order)
            }
        }

        if (asks != null) {
            for (it in asks) {
                val ask = it as JSONArray
                val order = Entity.create<MarketOrder>()
                order.product = product
                order.marketCode = marketCode
                order.tradeSide = OrderSideCode.SELL
                order.price = Numeric(ask[0].toString()) / Numeric(100_000_000)
                order.amount = Numeric(ask[1].toString()) / Numeric(100_000_000)
                order.dataStamp = timestamp
                result.add(order)
            }
        }
        return result
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${marketCode}/${products.joinToString("|")}"
    }
}