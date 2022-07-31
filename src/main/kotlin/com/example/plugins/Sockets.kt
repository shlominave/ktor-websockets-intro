package com.example.plugins

import MyJson
import com.example.User
import com.example.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
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
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        val myJson = MyJson()
        if (connections.isEmpty())
            myJson.logoutEveryone()
        webSocket("/chat") {
//TODO exception for closed channel error
            var user :User?= User("", "")
            try {
                while (user?.hasEmptyField() == true) {
                    send(
                        "Hello and welcome to my chat project! \n" +
                                "write R/r to register or L/l to login as an existing user"
                    )
                    val choice = (incoming.receive() as Frame.Text).readText()
                    when (choice.lowercase()) {
                        UserOption.REGISTEROPTION.userChoice() -> {
                            send("going register")
                            if (myJson.registerSucceeded(this)) {
                                user = myJson.loginNewUser()
                                if (!user.hasEmptyField()) {
                                    send("You are logged in!")
                                } else {
                                    send("auto login failed:users not found")
                                }
                            }
                        }
                        UserOption.LOGINOPTION.userChoice() -> {
                            send("going login")
                            user = myJson.login(this)
                            // if the client chooses to return to the menu user returns with empty username
                        }
                        else -> {
                            send("invalid input, try again")
                        }
                    }
                }
                send("there is user:${user.username}")

            } catch (e: Exception) {
                println(e.localizedMessage)
            }
            val thisConnection = Connection(this, user.username)
            connections += thisConnection
            try {
                send(
                    "You are connected! There are ${connections.count()} users here.\n " +
                            "to send a PM use 'targeted username/msg' command "
                )
                connections.forEach {
                    if (it != connections.last())
                        it.session.send(
                            "a new user named ${connections.last().name} has just logged in. \n" +
                                    "Now There are ${connections.count()} users here"
                        )
                }
                val privateMessageKey = "/msg"
                for (frame in incoming) {
                    if (!File("src/main/resources/allUsers.json").exists())
                        break
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    if (receivedText == "quit") {
                        send("quiting..")
                        break
                    }
                    val msg = Message()
                    msg.content = msg.setMessageContent(thisConnection, receivedText)
                    if (receivedText.contains(privateMessageKey)) {
                        val targetedUsername = msg.findTargetedUsernameFromMsg(privateMessageKey)
                        val targetedConnection: Connection? =
                            msg.getPrivateMessageReceivingConnection(connections, targetedUsername)
                        if (targetedConnection != null) {
                            if (msg.content.contains("${targetedConnection.name}/msg")) {
                                val privateMessage =
                                    msg.removeCommandFromMsg(targetedConnection) + "(private message)"
                                if (targetedConnection.name == thisConnection.name)
                                    send(msg.setMessageToMyselfContent(thisConnection))
                                else {
                                    targetedConnection.session.send(privateMessage)
                                    send(
                                        msg.setMessageByMeContent(
                                            thisConnection,
                                            privateMessageKey
                                        )
                                    )
                                }

                            }
                        } else {
                            thisConnection.session.send("failed to send $receivedText (targeted username not found)")
                        }
                    } else {
                        connections.forEach {
                            if (it.name == thisConnection.name) {
                                send(msg.setMessageByMeContent(thisConnection, privateMessageKey))
                            } else
                                it.session.send(msg.content)
                        }
                    }
                }
            } catch (e: Exception) {
                send("there is an error")
                println(e.localizedMessage)
            } finally {
                send("disconnecting $thisConnection!")
                connections -= thisConnection
                connections.forEach {
                    it.session.send(
                        " $thisConnection! is longer connected \n" +
                                "number of users still connected:${connections.count()} "
                    )
                }
                if (File("src/main/resources/allUsers.json").exists())
                    user.logout()

            }
        }
    }
}