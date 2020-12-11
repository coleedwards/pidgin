package net.evilblock.pidgin

data class PidginOptions(val async: Boolean = true, val password: String = "") {

    constructor() : this(true)

}