package de.lolhens.resticui.restic

import java.net.URI

class ResticRepoS3(
    restic: Restic,
    password: String,
    private val s3Url: URI,
    private val accessKeyId: String,
    private val secretAccessKey: String
) : ResticRepo(
    restic,
    password
) {

    override fun repository(): String = "s3:$s3Url"

    override fun hosts(): List<String> = listOf(s3Url.host)

    override fun vars(): List<Pair<String, String>> = listOf(
        Pair("AWS_ACCESS_KEY_ID", accessKeyId),
        Pair("AWS_SECRET_ACCESS_KEY", secretAccessKey)
    )
}