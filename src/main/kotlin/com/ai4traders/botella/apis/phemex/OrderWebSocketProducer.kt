package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.data.entities.MarketOrder
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode
import com.ai4traders.botella.data.types.Numeric
import com.ai4traders.botella.data.types.OrderSideCode
import com.ai4traders.botella.utils.HttpUtils
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.json.*
import org.ktorm.entity.Entity
import java.net.ProtocolException

class OrderWebSocketProducer(
    private val product: TradableProduct,
    private val marketCode: MarketCode
) : ActiveDataProducer<MarketOrder>() {
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    val ticker = PhemexWebSocketApi.tickerMap[product]

    var ready = false

    override fun produceData() {
        val client = HttpUtils.createWebsocketClient()
        while (true) {
            try {
                runBlocking {
                    client.webSocket(
                        urlString = "${PhemexWebSocketApi.WEBSOCKET_URL}"
                    ) {
                        val subscription =
                            """{
                              "id": 1,
                              "method": "orderbook.subscribe",
                              "params": [
                                "sBTCUSDT",
                                true
                              ]
                            }""".replace(" ", "")
                        logger.info("Sending Phemex subscription: ${subscription}")
                        send(subscription)
                        val response = (incoming.receive() as Frame.Text).readText()
                        logger.info("Received response: ${response}")
                        while (true) {
                            val frame = incoming.receive()
                            val json = (frame as Frame.Text).readText()
                            println(json)
                            val jsonStructure = JSONTokener(json).nextValue();
                            when (jsonStructure) {
                                is JSONObject -> {
                                    if (jsonStructure["event"] != "heartbeat") {
                                        println("Received response: '${json}'")
                                    }
                                }
                                is JSONArray -> {
                                    //val orders = buildOrders(jsonStructure)
                                    //for (order in orders) {
                                    //    notifyConsumers(order)
                                    //}
                                    //if (!ready) {
                                    //    ready = true
                                    //    signal(Signal.READY)
                                    //}
                                }
                            }
                        }
                    }
                }
            } catch (e: ProtocolException) {
                logger.error("Failed to process the websocket data for market '${marketCode}' and product '${product}'!", e)
            } catch (e: Exception) {
                logger.error("Failed to process the websocket data for market '${marketCode}' and product '${product}'!", e)
            }
        }
        client.close()
    }


    private fun buildOrders(json: JSONArray): List<MarketOrder> {
        val result = mutableListOf<MarketOrder>()
        var bids = (
            if ((json[1] as JSONObject).has("b"))
                (json[1] as JSONObject)["b"]
            else if ((json[1] as JSONObject).has("bs"))
                (json[1] as JSONObject)["bs"]
            else null
        ) as JSONArray?
        var asks = (
            if ((json[1] as JSONObject).has("a"))
                (json[1] as JSONObject)["a"]
            else if ((json[1] as JSONObject).has("as"))
                (json[1] as JSONObject)["as"]
            else null
        ) as JSONArray?
        if (bids != null) {
            for (it in bids) {
                val bid = it as JSONArray
                val order = Entity.create<MarketOrder>()
                order.product = product
                order.marketCode = marketCode
                order.tradeSide = OrderSideCode.BUY
                order.price = Numeric(bid[0].toString())
                order.amount = Numeric(bid[1].toString())
                order.dataStamp = Instant.fromEpochMilliseconds(Numeric(bid[2].toString()).times(1000).doubleValue().toLong())
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
                order.price = Numeric(ask[0].toString())
                order.amount = Numeric(ask[1].toString())
                order.dataStamp = Instant.fromEpochMilliseconds(Numeric(ask[2].toString()).times(1000).doubleValue().toLong())
                result.add(order)
            }
        }
        return result
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${marketCode}/${product}"
    }
}