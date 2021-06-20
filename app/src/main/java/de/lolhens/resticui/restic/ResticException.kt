package de.lolhens.resticui.restic

data class ResticException(val exitCode: Int, val stderr: List<String>) :
    Exception("Restic error $exitCode:\n${stderr.joinToString("\n")}")