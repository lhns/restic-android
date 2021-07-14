package de.lolhens.resticui.restic

import kotlinx.serialization.Serializable

@Serializable
data class ResticForgetResult(
    val keep: List<ResticSnapshot>,
    val remove: List<ResticSnapshot>
)