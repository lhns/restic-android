package de.lolhens.resticui.restic

data class ResticException(
    val exitCode: Int,
    val stderr: List<String>,
    val cancelled: Boolean = false
) :
    Exception("Restic error $exitCode:\n${stderr.joinToString("\n")}")