package com.example

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MessageContentManagment(var msg: String){
    fun findTargetedUsernameFromMsg(afterUsernameStr: String) =
        msg.split("a message:",afterUsernameStr).component2()
    fun setMessageContent(
        thisConnection: WebSocketConnection,
        receivedText: String
    ) = getMessageDateAndTime() + " " + thisConnection.username + " sent a message:$receivedText"

    fun getMessageDateAndTime(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))

    fun getPrivateMessageReceivingConnection(
        connections: MutableSet<WebSocketConnection>,
        targetedUsername: String
    ): WebSocketConnection? = connections.find { it.username == targetedUsername }
    //returns null if connection was not found
}
