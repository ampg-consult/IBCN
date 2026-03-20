package com.ampgconsult.ibcn.data.network

import okhttp3.*
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null

    fun connect(url: String, listener: WebSocketListener) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    fun sendMessage(text: String) {
        webSocket?.send(text)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
