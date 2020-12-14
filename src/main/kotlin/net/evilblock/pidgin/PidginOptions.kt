package net.evilblock.pidgin

data class PidginOptions(val async: Boolean = true, val passwordEnabled: Boolean = false, val password: String = "", val debug: Boolean = false) {

    constructor() : this(true)

}