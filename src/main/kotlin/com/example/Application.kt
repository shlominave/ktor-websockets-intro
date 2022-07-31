package com.example

import com.example.plugins.configureSockets
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        //configureRouting()
        configureSockets()
    }.start(wait = true)
}
