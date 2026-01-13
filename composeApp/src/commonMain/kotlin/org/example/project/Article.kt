package org.example.project

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Article(
    val id: UInt,
    val name: String,
    val brand: String? = null,
    val abbreviation: String? = null,
    val minimumAmount: UInt = 0u,
    val defaultExpirationDays: UInt? = null,
    val notes: String? = null,
    val modified: String? = null,
    val added: String? = null,
    val imageIds: List<UInt> = emptyList(),
    @Transient val imagesChanged: Boolean = false
)
