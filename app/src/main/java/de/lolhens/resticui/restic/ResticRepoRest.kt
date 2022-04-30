package de.lolhens.resticui.restic

import java.net.URI

class ResticRepoRest(
    restic: Restic,
    password: String,
    private val restUrl: URI,
) : ResticRepo(
    restic,
    password
) {
    override fun repository(): String = "rest:$restUrl"

    override fun hosts(): List<String> = listOf(restUrl.host)
}