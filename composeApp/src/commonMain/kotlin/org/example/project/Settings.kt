package org.example.project

import kotlinx.serialization.Serializable

enum class LogLevel {
    OFF,
    FATAL,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE
}

@Serializable
data class Settings(
    val language: String = "en",
    val statisticTimespan: String = "year",
    val logLevel: LogLevel = LogLevel.ERROR,
    val backupOldFolderOnImport: Boolean = false,
    val enableExpirationDates: Boolean = true
)
