package org.example.project

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    val fileHandler = IosFileHandler()

    // Initialize Logger with default settings before anything else
    Logger.init(fileHandler, Settings())

    val jsonDataManager = JsonDataManager(fileHandler, "inventory.json")
    val imageManager = ImageManager(fileHandler)
    val settingsManager = JsonSettingsManager(fileHandler, "settings.json")
    val fileDownloader = FileDownloader()

    App(
        jsonDataManager = jsonDataManager,
        imageManager = imageManager,
        fileDownloader = fileDownloader,
        fileHandler = fileHandler,
        settingsManager = settingsManager
    )
}
