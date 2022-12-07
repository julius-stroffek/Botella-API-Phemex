package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.data.entities.MarketTrade
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode
import com.ai4traders.botella.data.types.Numeric
import com.ai4traders.botella.data.types.OrderSideCode
import com.ai4traders.botella.data.types.OrderTypeCode
import com.ai4traders.botella.exceptions.DataInconsistent
import com.ai4traders.botella.utils.HttpUtils
import com.ai4traders.botella.utils.WebSocketClientWrapper
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.json.*
import org.ktorm.entity.Entity
import java.net.ProtocolException

class MarketTradeWebSocketProducer(
    private val products: Collection<TradableProduct>,
    private val marketCode: MarketCode
) : ActiveDataProducer<MarketTrade>() {
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    val clientWrapper = WebSocketClientWrapper()

    val tickers = products.map { PhemexWebSocketApi.tickerMap[it] }
    val productMap = PhemexWebSocketApi.tickerMap.entries.map {it.value to it.key}.toMap()

    override fun produceData() {
        while (true) {
            clientWrapper.createClient()
            try {
                runBlocking {
                    clientWrapper.client.webSocket(
                        urlString = "${PhemexWebSocketApi.WEBSOCKET_URL}"
                    ) {
                        for (ticker in tickers) {
                            val subscription =
                                """{
                              "id": 1,
                              "method": "trade.subscribe",
                              "params": [
                                "${ticker}"
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
                                        val trades = buildTrades(jsonStructure)
                                        for (trade in trades) {
                                            notifyConsumers(trade)
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
                logger.error("Failed to process the websocket data for market '${marketCode}' and products '${products.joinToString(", ")}'!", e)
            } catch (e: Exception) {
                logger.error("Failed to process the websocket data for market '${marketCode}' and products '${products.joinToString(", ")}'!", e)
            }
        }
    }


    private fun buildTrades(json: JSONObject): List<MarketTrade> {
        val result = mutableListOf<MarketTrade>()
        val jsonTrades = json.getJSONArray("trades")
        val product = productMap[json.getString("symbol")]
        if (product != null) {
            for (it in jsonTrades) {
                val trd = it as JSONArray
                val trade = Entity.create<MarketTrade>()
                trade.marketCode = marketCode
                trade.product = product
                trade.price = Numeric(trd[2].toString()) / Numeric(100_000_000)
                trade.amount = Numeric(trd[3].toString()) / Numeric(100_000_000)
                trade.dataStamp =
                    Instant.fromEpochMilliseconds(Numeric(trd[0].toString()).div(1_000_000).doubleValue().toLong())
                trade.tradeSide = when (trd[1].toString().lowercase()) {
                    "buy" -> {
                        OrderSideCode.BUY
                    }
                    "sell" -> {
                        OrderSideCode.SELL
                    }
                    else -> throw DataInconsistent("Wrong value '${trd[3]}' for tradeSide!")
                }
                trade.orderType = OrderTypeCode.LIMIT
                result.add(trade)
            }
        }
        result.sortBy { it.dataStamp }
        return result
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${marketCode}/${products.joinToString("|")}"
    }
}