package de.lolhens.resticui.restic

import java.net.URI

class ResticRepoB2(
    restic: Restic,
    password: String,
    private val b2Url: URI,
    private val b2AccountId: String,
    private val b2AccountKey: String
) : ResticRepo(
    restic,
    password
) {
    override fun repository(): String = "b2:$b2Url"

    override fun hosts(): List<String> = listOf(b2Url.host)

    override fun vars(): List<Pair<String, String>> = listOf(
        Pair("B2_ACCOUNT_ID", b2AccountId),
        Pair("B2_ACCOUNT_KEY", b2AccountKey)
    )
}