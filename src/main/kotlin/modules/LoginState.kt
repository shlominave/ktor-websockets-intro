package modules

enum class LoginState(private val loginResponse: String) {
    LOGINSUCCEEDED("login successful"),
    TRYAGAIN("user and/or password incorrect"),
    ALREADYLOGGEDIN("user is already logged in"),
    NOUSERSFOUND("no users found");
    fun getResponse()=this.loginResponse
}