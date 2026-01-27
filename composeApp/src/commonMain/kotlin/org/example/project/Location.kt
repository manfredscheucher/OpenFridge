package org.example.project

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val id: UInt,
    val name: String,
    val notes: String? = null,
    val deleted: Boolean? = null,
    val imageIds: List<UInt> = emptyList()
)
