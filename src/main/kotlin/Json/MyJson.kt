package Json

import modules.LoginState
import com.example.User
import com.example.WebSocketConnection
import com.google.gson.Gson
import io.ktor.network.sockets.*
import io.ktor.websocket.*
import java.io.File
import java.io.FileWriter

class MyJson {
    private val myFile = File("src/main/resources/allUsers.json")
    private val myGson = Gson()
    private fun addUserToJsonIfValid(username: String, password: String): Boolean {
        val user = User(username, password)

        if (!myFile.exists()) {
            myFile.createNewFile()
            updateFileContent(listOf(user))
            return true
        }
        if (myFile.length() == 0L) {
            updateFileContent(listOf(user))
            return true
        }
        var allUsers: List<User>? = getAllUsers()
        val isUsernameOK=!(isUsernameTaken(allUsers, user))
        if (isUsernameOK) {
            allUsers = allUsers?.plus(user)
            updateFileContent(allUsers)
        }
        return isUsernameOK
    }

    private fun isUsernameTaken(allUsers: List<User>?, user: User)=
        allUsers?.find {
            it.username == user.username
            } !=null


    fun getAllUsers(): List<User>? {
        if (!myFile.exists())
            println("null")
        if (myFile.length() != 0L)
            return myGson.fromJson(myFile.readText(), Array<User>::class.java).toList()
        return null
    }

    fun updateFileContent(userList: List<User>?) {
        val usersWriter = FileWriter(myFile)
        myGson.toJson(userList, usersWriter)
        usersWriter.close()
    }

    suspend fun registerSucceeded(session: DefaultWebSocketSession): Boolean {
        session.send("if you wish to return to the menu write (my menu) in one of the fields")
        session.send("Welcome to register ${System.lineSeparator()} Enter Username")
        val username = (session.incoming.receive() as Frame.Text).readText()
        if (username.contains("my menu")) {
            session.send("back to the menu!")
            return false
        }
        session.send("Enter Password")
        val password = (session.incoming.receive() as Frame.Text).readText()
        if (password.contains("my menu")) {
            session.send("back to the menu!")
            return false
        }
        if (username.length >= 3 && password.length >= 3) {
            try {
                val myJson = MyJson()
                if (myJson.addUserToJsonIfValid(username, password)) {
                    session.send("Registration completed successfully")
                    return true
                } else
                    session.send("username is taken, try again")
            } catch (e: Exception) {
                println("HA NOPE")
                println(e.localizedMessage)
            }
        } else session.send("username and password both need to be at least 3 chars long")

        return false
    }

    fun loginNewUser(connections: MutableList<WebSocketConnection>): User? {
        val userList = this.getAllUsers()
        if (!userList.isNullOrEmpty()) {
            val user = userList.last()
            validateLoginAndRespond(user, connections)
            return user
        }
        return null
    }

    suspend fun login(session: DefaultWebSocketSession, connections: MutableList<WebSocketConnection>): User? {
        session.send("if you wish to return to the menu write (my menu) in one of the fields")
        session.send("Welcome to login ${System.lineSeparator()} Enter username")
        val username = (session.incoming.receive() as Frame.Text).readText()
        val toMenuCommand="my menu"
        if (username.contains(toMenuCommand)) {
            session.send("back to the menu!")
            return null
        }
        session.send("Enter Password")
        val password = (session.incoming.receive() as Frame.Text).readText()
        if (password.contains(toMenuCommand)) {
            session.send("back to the menu!")
            return null
        }
        try {
            if (fieldsAreNotEmpty(username, password)) {
                val loginUser=User(username, password)
                val loginState = validateLoginAndRespond(loginUser, connections)
                session.send(loginState.getResponse())
                if (loginState == LoginState.LOGINSUCCEEDED) {
                    return loginUser
                }
            }
        } catch (e: Exception) {
            println(e.localizedMessage)
        }
        return null
    }


    private fun fieldsAreNotEmpty(username: String, password: String) =
        username.isNotEmpty() && password.isNotEmpty()

    private fun validateLoginAndRespond(
        user: User,
        connections: MutableList<WebSocketConnection>
    ): LoginState {
        if (!myFile.exists())
            myFile.createNewFile()
        val users: List<User>? = getAllUsers()
        if (!users.isNullOrEmpty()) {
            if (users.find {
                    it.username == user.username && it.password == user.password
                } == null)
                return LoginState.TRYAGAIN
            if (connections.find { it.username == user.username } != null)
                return LoginState.ALREADYLOGGEDIN
            return LoginState.LOGINSUCCEEDED
        }
        return LoginState.NOUSERSFOUND
    }
}

//    fun logoutEveryone() {
//        if (myFile.list()?.isNotEmpty() == true) {
//            val users: List<User>? = this.getAllUsers()
//            users?.forEach {
//                it.isLoggedIn = false
//            }
//            if (users != null)
//                this.updateFileContent(users)
//        }
//    }

