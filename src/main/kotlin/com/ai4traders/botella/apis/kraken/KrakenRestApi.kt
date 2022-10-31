package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.data.entities.TradableProduct
import com.ai4traders.botella.data.types.OrderSideCode
import com.ai4traders.botella.data.types.OrderTypeCode
import org.apache.http.client.utils.URIBuilder
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import javax.json.Json
import javax.json.JsonObject

object KrakenRestApi {
    const val KRAKEN_SCHEME = "https"
    const val KRAKEN_HOST = "api.kraken.com"
    const val KRAKEN_PATH_TRADES = "/0/public/Trades"
    const val KRAKEN_PATH_ORDER_BOOK = "/0/public/Depth"
    const val KRAKEN_DEFAULT_START_NANOS = "0" //"1667246422000000000"
    const val CONNECTION_TIMEOUT = 5000
    const val READ_TIMEOUT = 5000
    const val ORDER_BOOK_DEPTH = 1000
    const val ORDER_BOOK_DEPTH_SPREAD = 0.001
    val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ")
    val tickerMap = mapOf(
        TradableProduct.BTCUSD to "XXBTZUSD",
        TradableProduct.ETHBTC to "XETHXXBT",
        TradableProduct.ETHUSD to "XETHZUSD",
    )

    fun createURIBuilder(ticker: String, path: String?, count: Int?): URIBuilder {
        val uriBuilder = URIBuilder()
        uriBuilder.scheme = KrakenRestApi.KRAKEN_SCHEME
        uriBuilder.host = KrakenRestApi.KRAKEN_HOST
        uriBuilder.path = path
        uriBuilder.charset = StandardCharsets.UTF_8
        uriBuilder.setParameter("pair", ticker)
        if (count != null) {
            uriBuilder.setParameter("count", count.toString())
        }
        return uriBuilder
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun fetchKrakenData(ticker: String, uriBuilder: URIBuilder): JsonObject {
        val krakenUrl = uriBuilder.build().toURL()
        val krakenCon = krakenUrl.openConnection()
        krakenCon.connectTimeout = CONNECTION_TIMEOUT
        krakenCon.readTimeout = READ_TIMEOUT
        val krakenStream = krakenCon.getInputStream()
        val jsonReader = Json.createReader(krakenStream)
        val jsonObject = jsonReader.readObject()
        return jsonObject.getJsonObject("result")
    }

    @Throws(URISyntaxException::class, IOException::class)
    fun fetchTradeData(ticker: String, since: String? = null, count: Int? = null): JsonObject {
        val uriBuilder = createURIBuilder(ticker, KRAKEN_PATH_TRADES, count)
        if (since != null) {
            uriBuilder.setParameter("since", since)
        }
        return fetchKrakenData(ticker, uriBuilder)
    }

    @Throws(URISyntaxException::class, IOException::class)
    fun fetchOrderBookData(ticker: String, count: Int?): JsonObject {
        val uriBuilder = createURIBuilder(ticker, KRAKEN_PATH_ORDER_BOOK, count)
        return fetchKrakenData(ticker, uriBuilder)
    }

    fun parseOrderSide(value: String): OrderSideCode {
        when (value.lowercase()) {
            "b" -> return OrderSideCode.BUY
            "s" -> return OrderSideCode.SELL
        }
        throw IllegalArgumentException("Failed to parse the OrderSideCode value '${value}'.")
    }

    fun parseOrderType(value: String): OrderTypeCode {
        when (value.lowercase()) {
            "m" -> return OrderTypeCode.MARKET
            "l" -> return OrderTypeCode.LIMIT
        }
        throw IllegalArgumentException("Failed to parse the OrderSideCode value '${value}'.")
    }
}