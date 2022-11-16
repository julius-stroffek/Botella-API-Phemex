package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.data.entities.MarketTrade
import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.producers.ActiveDataProducer
import com.ai4traders.botella.data.types.MarketCode
import com.ai4traders.botella.data.types.Numeric
import com.ai4traders.botella.data.types.OrderSideCode
import com.ai4traders.botella.data.types.OrderTypeCode
import com.ai4traders.botella.exceptions.DataInconsistent
import com.ai4traders.botella.utils.HttpUtils
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.json.*
import org.ktorm.entity.Entity
import java.net.ProtocolException

class MarketTradeWebSocketProducer(
    private val product: TradableProduct,
    private val marketCode: MarketCode
) : ActiveDataProducer<MarketTrade>() {
    /** The kotlin logger. */
    private val logger = KotlinLogging.logger {}

    val ticker = KrakenWebSocketApi.tickerMap[product]

    /*
    [
      0,
      [
        [
          "5541.20000",
          "0.15850568",
          "1534614057.321597",
          "s",
          "l",
          ""
        ],
        [
          "6060.00000",
          "0.02455000",
          "1534614057.324998",
          "b",
          "l",
          ""
        ]
      ],
      "trade",
      "XBT/USD"
    ]
    */

    override fun produceData() {
        val client = HttpUtils.createWebsocketClient()
        while (true) {
            try {
                runBlocking {
                    client.webSocket(
                        urlString = "${KrakenWebSocketApi.WEBSOCKET_URL}"
                    ) {
                        val subscription =
                            """{
                              "event": "subscribe",
                              "pair": [
                                "${ticker}"
                              ],
                              "subscription": {
                                "name": "trade"
                              }
                            }""".replace(" ", "")
                        logger.info("Sending Kraken subscription: ${subscription}")
                        send(subscription)
                        while (true) {
                            val frame = incoming.receive()
                            val json = (frame as Frame.Text).readText()
                            val jsonStructure = JSONTokener(json).nextValue();
                            when (jsonStructure) {
                                is JSONObject -> {
                                    if (jsonStructure["event"] != "heartbeat") {
                                        println("Received response: '${json}'")
                                    }
                                }
                                is JSONArray -> {
                                    val trades = buildTrades(jsonStructure)
                                    for (trade in trades) {
                                        notifyConsumers(trade)
                                    }
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


    private fun buildTrades(json: JSONArray): List<MarketTrade> {
        val result = mutableListOf<MarketTrade>()
        val jsonTrades = json[1]
        if (jsonTrades is JSONArray) {
            for (it in jsonTrades) {
                val trd = it as JSONArray
                val trade = Entity.create<MarketTrade>()
                trade.marketCode = marketCode
                trade.product = product
                trade.price = Numeric(trd[0].toString())
                trade.amount = Numeric(trd[1].toString())
                trade.dataStamp = Instant.fromEpochMilliseconds(Numeric(trd[2].toString()).times(1000).doubleValue().toLong())
                trade.tradeSide = when (trd[3]) {
                    "b" -> {OrderSideCode.BUY}
                    "s" -> {OrderSideCode.SELL}
                    else -> throw DataInconsistent("Wrong value '${trd[3]}' for tradeSide!")
                }
                trade.orderType = when (trd[4]) {
                    "l" -> {OrderTypeCode.LIMIT}
                    "m" -> {OrderTypeCode.MARKET}
                    else -> throw DataInconsistent("Wrong value '${trd[4]}' for tradeType!")
                }
                result.add(trade)
            }
        } else {
            throw DataInconsistent("Unexpected json format for trades: ${json}")
        }
        return result
    }

    override fun uniquePathId(): String {
        return "${this::class.qualifiedName}/${marketCode}/${product}"
    }
}