package com.ai4traders.botella.apis.phemex

import com.ai4traders.botella.data.entities.TradableProduct

object PhemexWebSocketApi {
    const val WEBSOCKET_URL = "wss://phemex.com/ws"

    val tickerMap = mapOf(
        TradableProduct.BTCUSD to "sBTCUSDT",
        //TradableProduct.ETHBTC to "ETHBTC",
        TradableProduct.ETHUSD to "sETHUSDT",
        TradableProduct.XRPUSD to "sXRPUSDT",
        //TradableProduct.XRPBTC to "XRP/BTC",
        TradableProduct.LTCUSD to "sLTCUSDT",
        //TradableProduct.LTCBTC to "LTC/BTC",
        TradableProduct.XMRUSD to "sXMRUSDT",
        //TradableProduct.XMRBTC to "XMR/BTC",
        TradableProduct.BCHUSD to "sBCHUSDT",
        //TradableProduct.BCHBTC to "BCH/BTC",
        TradableProduct.DOGEUSD to "sDOGEUSDT",
        //TradableProduct.DOGEBTC to "DOGE/BTC",
        TradableProduct.XLMUSD to "sXLMUSDT",
        //TradableProduct.XLMBTC to "XLM/BTC",
        //TradableProduct.QNTUSD to "QNT/USD",
        //TradableProduct.QNTBTC to "QNTBTC",
        //TradableProduct.XNOUSD to "XNO/USD",
        //TradableProduct.XNOBTC to "XNO/BTC",
    )
}

