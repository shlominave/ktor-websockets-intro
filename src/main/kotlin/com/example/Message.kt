package com.example

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Message {
    var content: String = ""
    fun removeCommandFromMsg(thisConnection: Connection): String =
        content.removePrefix("$thisConnection/msg")
    fun findTargetedUsernameFromMsg(afterUsernameStr: String) = content.splitToSequence("kj")
     //   (content.indexOf("a message:") + 10, content.indexOf(afterUsernameStr))

    fun setMessageByMeContent(thisConnection: Connection, privateMessageKey: String): String = content.replace("$thisConnection sent", "I've just sent", true)
//    fun setMessageToMyselfContent(thisConnection: Connection): String = content.replace("$thisConnection sent a message:$thisConnection/msg", "here's a message from myself:")

    fun setMessageContent(
        thisConnection: Connection,
        receivedText: String
    )=getMessageDateAndTime()+" "+thisConnection.name + " sent a message:$receivedText"

    private fun getMessageDateAndTime() =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))

    fun getPrivateMessageReceivingConnection(
        connections: MutableSet<Connection>,
        targetedUsername: String
    ): Connection?= connections.find { it.name == targetedUsername }
        //returns null if connection was not found
    }
