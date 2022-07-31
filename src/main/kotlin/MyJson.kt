import com.example.User
import com.google.gson.Gson
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
        if (myFile.length()==0L) {
            updateFileContent(listOf(user))
            return true
        }
        var allUsers: List<User>? = getAllUsers()
        if (!isUsernameTaken(allUsers, user)) {
            allUsers = allUsers?.plus(user)
            updateFileContent(allUsers)
            return true
        }
        return false
    }

    private fun isUsernameTaken(allUsers: List<User>?, user: User): Boolean {
        allUsers?.forEach {
            if (it.username == user.username) {
                return true
            }
        }
        return false
    }

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
                    session.send("HA NOPE")
                    session.send(e.localizedMessage)
                }
        } else   session.send("username and password both need to be at least 3 chars long")

        return registerSucceeded(session)
    }

    internal fun loginNewUser(): User? {
        val userList = this.getAllUsers()
        if (!userList.isNullOrEmpty()) {
            val user = userList.last()
            validateLoginAndRespond(user.username, user.password)
            return user
        }
      return null
    }

    suspend fun login(session: DefaultWebSocketSession): User {
        session.send("if you wish to return to the menu write (my menu) in one of the fields")
        session.send("Welcome to login \n Enter username")
        val username = (session.incoming.receive() as Frame.Text).readText()
        if (username.contains("my menu")) {
            session.send("back to the menu!")
            return User("", "")
        }
        session.send("Enter Password")
        val password = (session.incoming.receive() as Frame.Text).readText()
        if (password.contains("my menu")) {
            session.send("back to the menu!")
            return User("", "")
        }
        try {
            if (fieldsAreNotEmpty(username, password)) {
                val response = validateLoginAndRespond(username, password)
                session.send(response)
                if (response == "logged in successfully") {
                    return User(username, password)
                }
            }
        } catch (e: Exception) {

            println(e.localizedMessage)
        }
        return login(session)
    }

    private fun fieldsAreNotEmpty(username: String, password: String) =
        username.isNotEmpty() && password.isNotEmpty()

    private fun validateLoginAndRespond(username: String, password: String): String {
        if (!myFile.exists())
            myFile.createNewFile()
        val users: List<User>? = getAllUsers()
        if (!users.isNullOrEmpty()) {
            val loginUser = users.find {
                it.username == username && it.password == password
            } ?: return "user or/and password incorrect!"
            if (!loginUser.isLoggedIn) {
                loginUser.isLoggedIn = true
                updateFileContent(users)
                return "logged in successfully"
            }
            return "user already logged In"
        }
        return "there are no users in the file"
    }

    fun logoutEveryone() {
        if (myFile.length() > 0) {
            val users: List<User>? = this.getAllUsers()
            users?.forEach {
                it.isLoggedIn = false
            }
            if (users != null)
                this.updateFileContent(users)
        }
    }
}
