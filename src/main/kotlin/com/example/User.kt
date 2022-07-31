package com.example

import MyJson

data class User(val username: String, val password: String) {
    var isLoggedIn = false
        fun hasEmptyField(): Boolean = this.username.isEmpty() || this.password.isEmpty()
        fun logout() {
        val myJson = MyJson()
        val users: List<User>? = myJson.getAllUsers()
        val logoutUser = users?.find { it.username == this.username }
        if (logoutUser != null) {
            logoutUser.isLoggedIn=false
            myJson.updateFileContent(users)
        }
    }


    override fun toString(): String {
        return "[username:$username password:$password]"
    }
}