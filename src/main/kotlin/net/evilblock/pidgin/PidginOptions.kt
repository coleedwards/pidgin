package net.evilblock.pidgin

data class PidginOptions(val async: Boolean = true, val passwordEnabled: Boolean = false, val password: String = "") {

    constructor() : this(true)

}