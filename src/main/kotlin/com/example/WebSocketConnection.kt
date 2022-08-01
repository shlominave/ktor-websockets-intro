package com.example
import io.ktor.websocket.*

data class WebSocketConnection(val session: DefaultWebSocketSession, val username: String) {
  //  val name = username
  override fun toString():String{
        return this.username
    }
}