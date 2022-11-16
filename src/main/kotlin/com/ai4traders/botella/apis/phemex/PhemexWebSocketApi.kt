package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.data.entities.TradableProduct

object PhemexWebSocketApi {
    const val WEBSOCKET_URL = "wss://phemex.com/ws"

    val tickerMap = mapOf(
        TradableProduct.BTCUSD to "sBTCUSDT",
        TradableProduct.ETHBTC to "sETHBTC",
        //TradableProduct.ETHUSD to "ETH/USD",
        //TradableProduct.XRPUSD to "XRP/USD",
        //TradableProduct.XRPBTC to "XRP/BTC",
        //TradableProduct.LTCUSD to "LTC/USD",
        //TradableProduct.LTCBTC to "LTC/BTC",
        //TradableProduct.XMRUSD to "XMR/USD",
        //TradableProduct.XMRBTC to "XMR/BTC",
        //TradableProduct.BCHUSD to "BCH/USD",
        //TradableProduct.BCHBTC to "BCH/BTC",
        //TradableProduct.DOGEUSD to "DOGE/USD",
        //TradableProduct.DOGEBTC to "DOGE/BTC",
        //TradableProduct.XLMUSD to "XLM/USD",
        //TradableProduct.XLMBTC to "XLM/BTC",
        //TradableProduct.QNTUSD to "QNT/USD",
        //TradableProduct.QNTBTC to "QNTBTC",
        //TradableProduct.XNOUSD to "XNO/USD",
        //TradableProduct.XNOBTC to "XNO/BTC",
    )
}

