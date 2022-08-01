package modules

enum class UserOption(private val choice: String) {
    LOGINOPTION("l"),
    REGISTEROPTION("r");

    fun userChoice() = this.choice
}