package com.ai4traders.botella.apis.kraken

import com.ai4traders.botella.data.entities.TradableProduct

object KrakenWebSocketApi {
    const val WEBSOCKET_URL = "wss://ws.kraken.com/spot/public/v3"
    const val WEBSOCKET_TOPIC_TRADE = "trade."
    const val WEBSOCKET_TOPIC_ORDER = "orderbook.40."

    val tickerMap = mapOf(
        TradableProduct.BTCUSD to "BTC/USD",
        TradableProduct.ETHBTC to "ETH/BTC",
        TradableProduct.ETHUSD to "ETH/USD",
        TradableProduct.XRPUSD to "XRP/USD",
        TradableProduct.XRPBTC to "XRP/BTC",
        TradableProduct.LTCUSD to "LTC/USD",
        TradableProduct.LTCBTC to "LTC/BTC",
        TradableProduct.XMRUSD to "XMR/USD",
        TradableProduct.XMRBTC to "XMR/BTC",
        TradableProduct.BCHUSD to "BCH/USD",
        TradableProduct.BCHBTC to "BCH/BTC",
        TradableProduct.DOGEUSD to "DOGE/USD",
        TradableProduct.DOGEBTC to "DOGE/BTC",
        TradableProduct.XLMUSD to "XLM/USD",
        TradableProduct.XLMBTC to "XLM/BTC",
        TradableProduct.QNTUSD to "QNT/USD",
        //TradableProduct.QNTBTC to "QNTBTC",
        TradableProduct.XNOUSD to "XNO/USD",
        TradableProduct.XNOBTC to "XNO/BTC",
    )
}

