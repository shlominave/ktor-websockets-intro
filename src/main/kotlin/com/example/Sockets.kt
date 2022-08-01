package com.example.plugins

import Json.MyJson
import com.example.User
import com.example.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import modules.UserOption
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashSet

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.synchronizedSet<WebSocketConnection?>(LinkedHashSet())

        webSocket("/chat") {
//TODO exception for closed channel error
            var user: User? = null
            try {
                while (user==null) {
                    send(
                        "Hello and welcome to my chat project! ${System.lineSeparator()}}" +
                                "write R/r to register or L/l to login as an existing user"
                    )
                   user=enter( connections)
                }

            } catch (e: Exception) {
                println(e.localizedMessage)
            }

            val thisConnection = WebSocketConnection(this, user!!.username)
            connections += thisConnection
            try {
                send(
                    "You are connected! There are ${connections.count()} users here.${System.lineSeparator()} " +
                            "to send a PM use 'targeted username/msg' command "
                )
                sendNewUserConnectedMsg(connections)
                val privateMessageKey = "/msg"
                for (frame in incoming) {
                    if (!File("src/main/resources/allUsers.json").exists())
                        break
                    frame as? Frame.Text ?: continue
                    var receivedText = frame.readText() as String
                    if (receivedText == "quit") {
                        send("quiting..")
                        break
                    }
                    val msg = MessageContentManagment()
                    msg.content = msg.setMessageContent(thisConnection, receivedText)
                    var message=msg.content
                    if (receivedText.contains(privateMessageKey)) {
                        val targetedUsername = msg.findTargetedUsernameFromMsg(privateMessageKey)
                        val targetedConnection: WebSocketConnection? = msg.getPrivateMessageReceivingConnection(connections, targetedUsername)
                        if (targetedConnection != null) {
                            val privateMessageCommand= targetedConnection.username +privateMessageKey
                            if (message.contains(privateMessageCommand)) {
                                receivedText=receivedText.removePrefix(privateMessageCommand)
                                if (targetedConnection.username == thisConnection.username) {
                                    send(msg.getMessageDateAndTime() + " Here's a message to myself:" + receivedText)
                                }
                                else {
                                   message=msg.setMessageContent(thisConnection,privateMessageCommand)
                                    targetedConnection.session.send(message.removePrefix(privateMessageCommand)+"(private message)")
                                    send("${msg.getMessageDateAndTime()} I've sent a private message:$receivedText")
                                }
                            }
                        } else {
                            thisConnection.session.send("failed to send $receivedText (targeted username not found)")
                        }
                    } else {

                        connections.forEach {
                            if (it.username == thisConnection.username) {
                                send("I've sent a message:$receivedText at ${msg.getMessageDateAndTime()}")
                            } else
                                it.session.send(message)
                        }
                    }
                }
            } catch (e: Exception) {
                send("there is an error")
                println(e.localizedMessage)
            } finally {
                send("disconnecting $thisConnection!")
                connections -= thisConnection
                func(connections, thisConnection)
//                if (File("src/main/resources/allUsers.json").exists())
//                    user.logout()

            }
        }
    }
}

private suspend fun sendNewUserConnectedMsg(connections: MutableSet<WebSocketConnection>) {
    connections.forEach {
        if (it != connections.last())
            it.session.send(
                "a new user named ${connections.last().username} has just logged in. ${System.lineSeparator()}" +
                        "Now There are ${connections.count()} users here"
            )
    }
}

private suspend fun DefaultWebSocketServerSession.enter(
    connections: MutableSet<WebSocketConnection>
) :User? {
    val myJson= MyJson()
    val user: User?
    val choice = (incoming.receive() as Frame.Text).readText()
    when (choice.lowercase()) {
        UserOption.REGISTEROPTION.userChoice() -> {
            send("going register")
            if (myJson.registerSucceeded(this)) {
                user = myJson.loginNewUser(connections.toMutableList())
                if (user != null && !user.hasEmptyField()) {
                    send("You are logged in!")
                    return user
                } else {
                    send("auto login failed:users not found")
                }
            }
        }
        UserOption.LOGINOPTION.userChoice() -> {
            send("going login")
            user = myJson.login(this, connections.toMutableList())
            if (user != null && !user.hasEmptyField())
                return user
        }
        else -> {
            send("invalid input, try again")
        }
    }
    return null
}

private suspend fun func(
    connections: MutableSet<WebSocketConnection>,
    thisConnection: WebSocketConnection
) {
    connections.forEach {
        it.session.send(
            " $thisConnection! is longer connected ${System.lineSeparator()}" +
                    "number of users still connected:${connections.count()} "
        )
    }
}