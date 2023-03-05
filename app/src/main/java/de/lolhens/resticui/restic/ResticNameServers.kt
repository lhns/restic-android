package de.lolhens.resticui.restic

import android.content.Context
import de.lolhens.resticui.util.DnsServersDetector

interface ResticNameServers {
    companion object {
        fun fromContext(context: Context): ResticNameServers {
            val dnsServersDetector = DnsServersDetector(context)
            return object : ResticNameServers {
                override fun nameServers(): List<String> = dnsServersDetector.servers.asList()

            }
        }

        fun fromList(nameServers: List<String>): ResticNameServers = object : ResticNameServers {
            override fun nameServers(): List<String> = nameServers

        }
    }

    fun nameServers(): List<String>
}