package org.example.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Assignment(
    val id: UInt,
    val articleId: UInt,
    val locationId: UInt,
    val amount: UInt,
    val addedDate: String? = null,
    val expirationDate: String? = null,
    val consumedDate: String? = null,
    val lastModified: String? = null,
    val deleted: Boolean? = null
)
