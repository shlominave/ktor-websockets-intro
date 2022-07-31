package com.example
import io.ktor.websocket.*
import java.util.concurrent.atomic.*

 data class Connection(val session: DefaultWebSocketSession, val username: String) {
    val name = username
  override fun toString():String{
        return this.name
    }
}