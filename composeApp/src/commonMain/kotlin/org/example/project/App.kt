package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mistermanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
//import java.util.Locale // TODO
import kotlin.NoSuchElementException // Ensure this import is present

sealed class Screen {
    data object Home : Screen()
    data object ArticleList : Screen()
    data class ArticleForm(val articleId: UInt) : Screen()
    data object LocationList : Screen()
    data class LocationForm(val locationId: UInt) : Screen()
    data class LocationAssignments(val locationId: UInt, val locationName: String) : Screen()
    data class ArticleAssignments(val articleId: UInt, val articleName: String) : Screen()
    data object Info : Screen()
    data object HowToHelp : Screen()
    data object Statistics : Screen()
    data object Settings : Screen()
}

@Composable
fun App(jsonDataManager: JsonDataManager, imageManager: ImageManager, fileDownloader: FileDownloader, fileHandler: FileHandler, settingsManager: JsonSettingsManager) {
    var navStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val screen = navStack.last()
    var settings by remember { mutableStateOf(Settings()) }
    var articles by remember { mutableStateOf(emptyList<Article>()) }
    var locations by remember { mutableStateOf(emptyList<Location>()) }
    var assignments by remember { mutableStateOf(emptyList<Assignment>()) }
    var showNotImplementedDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun navigateTo(newScreen: Screen) {
        navStack = navStack + newScreen
    }

    fun navigateBack() {
        if (navStack.size > 1) {
            navStack = navStack.dropLast(1)
        }
    }

    suspend fun reloadAllData() {
        try {
            val data = withContext(Dispatchers.Default) { jsonDataManager.load() }
            articles = data.articles
            locations = data.locations
            assignments = data.assignments
        } catch (e: Exception) {
            val errorMessage = "Failed to load data: ${e.message}. The data file might be corrupt."
            errorDialogMessage = errorMessage
            Logger.log(LogLevel.ERROR, "Failed to load data in fun reloadAllData: ${e.message}", e)
        }
        Logger.log(LogLevel.INFO,"Data reloaded" )
        Logger.logImportantFiles(LogLevel.TRACE)
    }

    LaunchedEffect(Unit) {
        settings = withContext(Dispatchers.Default) { settingsManager.loadSettings() }
    }

    LaunchedEffect(settings) {
        Logger.log(LogLevel.INFO,"Settings reloaded" )
        Logger.logImportantFiles(LogLevel.DEBUG)
        setAppLanguage(settings.language)
        reloadAllData()
    }

    LaunchedEffect(screen) {
        val screenName = when (val s = screen) {
            is Screen.Home -> "Home"
            is Screen.ArticleList -> "ArticleList"
            is Screen.ArticleForm -> "ArticleForm(articleId=${s.articleId})"
            is Screen.LocationList -> "LocationList"
            is Screen.LocationForm -> "LocationForm(locationId=${s.locationId})"
            is Screen.LocationAssignments -> "LocationAssignments(locationId=${s.locationId}, locationName='${s.locationName}')"
            is Screen.ArticleAssignments -> "ArticleAssignments(articleId=${s.articleId}, articleName='${s.articleName}')"
            is Screen.Info -> "Info"
            is Screen.HowToHelp -> "HowToHelp"
            is Screen.Statistics -> "Statistics"
            is Screen.Settings -> "Settings"
        }
        Logger.log(LogLevel.INFO, "Navigating to screen: $screenName")
        Logger.logImportantFiles(LogLevel.TRACE)
    }

    if (showNotImplementedDialog) {
        AlertDialog(
            onDismissRequest = { showNotImplementedDialog = false },
            title = { Text(stringResource(Res.string.not_implemented_title)) },
            text = { Text(stringResource(Res.string.not_implemented_message)) },
            confirmButton = {
                TextButton(onClick = { showNotImplementedDialog = false }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("Error") },
            text = { Text(errorDialogMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorDialogMessage = null }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    MaterialTheme(
        colorScheme = LightColorScheme
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
        ) { innerPadding ->
            key(settings.language, settings.logLevel, settings.backupOldFolderOnImport) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    when (val s = screen) {
                        Screen.Home -> HomeScreen(
                            onOpenArticles = { navigateTo(Screen.ArticleList) },
                            onOpenLocations = { navigateTo(Screen.LocationList) },
                            onOpenInfo = { navigateTo(Screen.Info) },
                            onOpenStatistics = { navigateTo(Screen.Statistics) },
                            onOpenSettings = { navigateTo(Screen.Settings) },
                            onOpenHowToHelp = { navigateTo(Screen.HowToHelp) }
                        )

                        Screen.ArticleList -> {
                            val defaultArticleName = stringResource(Res.string.article_new_default_name)
                            ArticleListScreen(
                                articles = articles.sortedByDescending { it.modified },
                                locations = locations,
                                imageManager = imageManager,
                                assignments = assignments,
                                settings = settings,
                                onAddClick = {
                                    scope.launch {
                                        val newArticle = jsonDataManager.createNewArticle(defaultArticleName)
                                        withContext(Dispatchers.Default) { jsonDataManager.addOrUpdateArticle(newArticle) }
                                        reloadAllData()
                                        navigateTo(Screen.ArticleForm(newArticle.id))
                                    }
                                },
                                onOpen = { id -> navigateTo(Screen.ArticleForm(id)) },
                                onBack = { navigateBack() },
                                onSettingsChange = { newSettings ->
                                    scope.launch {
                                        withContext(Dispatchers.Default) {
                                            settingsManager.saveSettings(newSettings)
                                        }
                                        settings = newSettings
                                    }
                                }
                            )
                        }

                        is Screen.ArticleForm -> {
                            val existingArticle = remember(s.articleId, articles) {
                                try {
                                    jsonDataManager.getArticleById(s.articleId)
                                } catch (e: NoSuchElementException) {
                                    scope.launch {
                                        Logger.log(LogLevel.ERROR, "Failed to get article by id ${s.articleId} in ArticleForm: ${e.message}", e)
                                    }
                                    null
                                }
                            }
                            var articleImagesMap by remember { mutableStateOf<Map<UInt, ByteArray>>(emptyMap()) }

                            LaunchedEffect(s.articleId, existingArticle) {
                                val imageMap = mutableMapOf<UInt, ByteArray>()
                                existingArticle?.imageIds?.forEach { imageId ->
                                    try {
                                        withContext(Dispatchers.Default) {
                                            imageManager.getArticleImage(existingArticle.id, imageId)
                                                ?.let {
                                                    imageMap[imageId] = it
                                                } ?: scope.launch {
                                                Logger.log(LogLevel.WARN, "Image not found for article ${existingArticle.id}, imageId $imageId")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        scope.launch {
                                            Logger.log(LogLevel.ERROR, "Failed to load image for article ${existingArticle.id}, imageId $imageId: ${e.message}", e)
                                        }
                                    }
                                }
                                articleImagesMap = imageMap
                            }

                            if (existingArticle == null) {
                                LaunchedEffect(s.articleId) { navigateBack() }
                            } else {
                                val relatedAssignments = assignments.filter { it.articleId == existingArticle.id }
                                ArticleFormScreen(
                                    initial = existingArticle,
                                    initialImages = articleImagesMap,
                                    assignmentsForArticle = relatedAssignments,
                                    allLocations = locations,
                                    locationById = { pid ->
                                        locations.firstOrNull { it.id == pid }.also {
                                            if (it == null) {
                                                scope.launch {
                                                    Logger.log(LogLevel.WARN, "Location with id $pid not found, referenced by article ${existingArticle.id}")
                                                }
                                            }
                                        }
                                    },
                                    imageManager = imageManager,
                                    settings = settings,
                                    onBack = { navigateBack() },
                                    onDelete = { articleIdToDelete ->
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.Default) {
                                                    val articleToDelete = jsonDataManager.getArticleById(articleIdToDelete)
                                                    articleToDelete!!.imageIds.forEach { imageId ->
                                                        imageManager.deleteArticleImage(articleIdToDelete, imageId)
                                                    }
                                                    jsonDataManager.deleteArticle(articleIdToDelete)
                                                }
                                                reloadAllData()
                                                navStack = navStack.filterNot { it is Screen.ArticleForm && it.articleId == articleIdToDelete }
                                            } catch (e: Exception) {
                                                Logger.log(LogLevel.ERROR, "Failed to delete article with id $articleIdToDelete: ${e.message}", e)
                                                errorDialogMessage = "Failed to delete article: ${e.message}"
                                            }
                                        }
                                    },
                                    onSave = { editedArticle, newImages ->
                                        scope.launch {
                                            val existingImageIds = existingArticle.imageIds
                                            val newImagesToUpload = newImages.filter { it.key !in existingArticle.imageIds }
                                            val idsToDelete = existingImageIds.filter { it !in newImages.keys }
                                            Logger.log(LogLevel.DEBUG, "Upload images: $newImagesToUpload")
                                            Logger.log(LogLevel.DEBUG, "Removing old images with ids: $idsToDelete")

                                            withContext(Dispatchers.Default) {
                                                idsToDelete.forEach { imageId ->
                                                    imageManager.deleteArticleImage(editedArticle.id, imageId)
                                                }

                                                newImagesToUpload.entries.forEach { (imageId, imageData) ->
                                                    imageManager.saveArticleImage(
                                                        editedArticle.id,
                                                        imageId,
                                                        imageData
                                                    )
                                                }

                                                jsonDataManager.addOrUpdateArticle(editedArticle)
                                            }
                                            reloadAllData()
                                            navigateBack()
                                        }
                                    },
                                    onAddColor = { articleToCopy ->
                                        scope.launch {
                                            val newArticleWithNewId = jsonDataManager.createNewArticle(articleToCopy.name)
                                            val newArticle = newArticleWithNewId.copy(
                                                brand = articleToCopy.brand,
                                                abbreviation = articleToCopy.abbreviation,
                                                minimumAmount = articleToCopy.minimumAmount,
                                                defaultExpirationDays = articleToCopy.defaultExpirationDays,
                                                notes = articleToCopy.notes
                                            )
                                            withContext(Dispatchers.Default) { jsonDataManager.addOrUpdateArticle(newArticle) }
                                            reloadAllData()
                                            navigateTo(Screen.ArticleForm(newArticle.id))
                                        }
                                    },
                                    onNavigateToAssignments = {
                                        navigateTo(Screen.ArticleAssignments(
                                            existingArticle.id,
                                            existingArticle.name
                                        ))
                                    },
                                    onNavigateToLocation = { projectId -> navigateTo(Screen.LocationForm(projectId)) }
                                )
                            }
                        }

                        Screen.LocationList -> {
                            val defaultLocationName =
                                stringResource(Res.string.location_new_default_name)
                            LocationListScreen(
                                locations = locations,
                                assignments = assignments,
                                imageManager = imageManager,
                                settings = settings,
                                onAddClick = {
                                    scope.launch {
                                        val newLocation =
                                            jsonDataManager.createNewLocation(defaultLocationName)
                                        withContext(Dispatchers.Default) {
                                            jsonDataManager.addOrUpdateLocation(
                                                newLocation
                                            )
                                        }
                                        reloadAllData()
                                        navigateTo(Screen.LocationForm(newLocation.id))
                                    }
                                },
                                onOpen = { id -> navigateTo(Screen.LocationForm(id)) },
                                onBack = { navigateBack() },
                                onSettingsChange = { newSettings ->
                                    scope.launch {
                                        withContext(Dispatchers.Default) {
                                            settingsManager.saveSettings(newSettings)
                                        }
                                        settings = newSettings
                                    }
                                }
                            )
                        }

                        is Screen.LocationForm -> {
                            val existingLocation = remember(s.locationId, locations) {
                                try {
                                    jsonDataManager.getLocationById(s.locationId)
                                } catch (e: NoSuchElementException) {
                                    scope.launch {
                                        Logger.log(LogLevel.ERROR, "Failed to get location by id ${s.locationId} in LocationForm: ${e.message}", e)
                                    }
                                    null
                                }
                            }
                            var locationImagesMap by remember { mutableStateOf<Map<UInt, ByteArray>>(emptyMap()) }

                            LaunchedEffect(s.locationId, existingLocation) {
                                val imageMap = mutableMapOf<UInt, ByteArray>()
                                existingLocation?.imageIds?.forEach { imageId ->
                                    try {
                                        withContext(Dispatchers.Default) {
                                            imageManager.getLocationImage(existingLocation.id, imageId)
                                                ?. let{
                                                    imageMap[imageId] = it
                                                } ?: scope.launch {
                                                Logger.log(LogLevel.WARN, "Image not found for location ${existingLocation.id}, imageId $imageId")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        scope.launch {
                                            Logger.log(LogLevel.ERROR, "Failed to load image for location ${existingLocation.id}, imageId $imageId: ${e.message}", e)
                                        }
                                    }
                                }
                                locationImagesMap = imageMap
                            }

                            if (existingLocation == null) {
                                LaunchedEffect(s.locationId) { navigateBack() }
                            } else {
                                val assignmentsForCurrentLocation =
                                    assignments.filter { it.locationId == existingLocation.id }
                                LocationFormScreen(
                                    initial = existingLocation,
                                    initialImages = locationImagesMap,
                                    assignmentsForLocation = assignmentsForCurrentLocation,
                                    articleById = { articleId ->
                                        articles.firstOrNull { it.id == articleId }.also {
                                            if (it == null) {
                                                scope.launch {
                                                    Logger.log(LogLevel.WARN, "Article with id $articleId not found, referenced by location ${existingLocation.id}")
                                                }
                                            }
                                        }
                                    },
                                    imageManager = imageManager,
                                    settings = settings,
                                    onBack = { navigateBack() },
                                    onDelete = { locationIdToDelete ->
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.Default) {
                                                    val locationToDelete = jsonDataManager.getLocationById(locationIdToDelete)
                                                    locationToDelete!!.imageIds.forEach { imageId ->
                                                        imageManager.deleteLocationImage(locationIdToDelete, imageId)
                                                    }
                                                    jsonDataManager.deleteLocation(locationIdToDelete)
                                                }
                                                reloadAllData()
                                                navStack = navStack.filterNot { (it is Screen.LocationForm && it.locationId == locationIdToDelete) || (it is Screen.LocationAssignments && it.locationId == locationIdToDelete) }
                                            } catch (e: Exception) {
                                                Logger.log(LogLevel.ERROR, "Failed to delete project with id $locationIdToDelete: ${e.message}", e)
                                                errorDialogMessage = "Failed to delete project: ${e.message}"
                                            }
                                        }
                                    },
                                    onSave = { editedLocation, newImages ->
                                        scope.launch {
                                            val existingImageIds = existingLocation.imageIds
                                            val newImagesToUpload = newImages.filter { it.key !in existingImageIds }
                                            val idsToDelete = existingImageIds.filter { it !in newImages.keys }
                                            Logger.log(LogLevel.DEBUG, "Upload images: $newImagesToUpload")
                                            Logger.log(LogLevel.DEBUG, "Removing old images with ids: $idsToDelete")

                                            withContext(Dispatchers.Default) {
                                                idsToDelete.forEach { imageId ->
                                                    imageManager.deleteLocationImage(editedLocation.id, imageId)
                                                }
                                                newImagesToUpload.entries.sortedBy { it.key }.forEach { (imageId, imageData) ->
                                                    imageManager.saveLocationImage(
                                                        editedLocation.id,
                                                        imageId,
                                                        imageData
                                                    )
                                                }

                                                jsonDataManager.addOrUpdateLocation(editedLocation)
                                            }
                                            reloadAllData()
                                            navigateBack()
                                        }
                                    },
                                    onNavigateToAssignments = {
                                        navigateTo(Screen.LocationAssignments(
                                            existingLocation.id,
                                            existingLocation.name
                                        ))
                                    },
                                    onNavigateToArticle = { articleId ->
                                        navigateTo(Screen.ArticleForm(articleId))
                                    }
                                )
                            }
                        }

                        is Screen.LocationAssignments -> {
                            val initialAssignmentsForLocation = assignments.filter { it.locationId == s.locationId }

                            LocationAssignmentsScreen(
                                locationName = s.locationName,
                                locationId = s.locationId,
                                allArticles = articles,
                                initialAssignments = initialAssignmentsForLocation,
                                settings = settings,
                                onCreateNewAssignment = { articleId, locationId ->
                                    jsonDataManager.createNewAssignment(articleId, locationId)
                                },
                                onSave = { updatedAssignments ->
                                    scope.launch {
                                        withContext(Dispatchers.Default) {
                                            jsonDataManager.setLocationAssignments(s.locationId, updatedAssignments)
                                        }
                                        reloadAllData()
                                    }
                                },
                                onBack = { navigateBack() }
                            )
                        }

                        is Screen.ArticleAssignments -> {
                            val initialAssignmentsForArticle = assignments.filter { it.articleId == s.articleId }

                            ArticleAssignmentsScreen(
                                articleName = s.articleName,
                                articleId = s.articleId,
                                allLocations = locations,
                                initialAssignments = initialAssignmentsForArticle,
                                settings = settings,
                                onCreateNewAssignment = { articleId, locationId ->
                                    jsonDataManager.createNewAssignment(articleId, locationId)
                                },
                                onSave = { updatedAssignments ->
                                    scope.launch {
                                        withContext(Dispatchers.Default) {
                                            jsonDataManager.setArticleAssignments(s.articleId, updatedAssignments)
                                        }
                                        reloadAllData()
                                    }
                                },
                                onBack = { navigateBack() }
                            )
                        }

                        Screen.Info -> {
                            InfoScreen(onBack = { navigateBack() }, onNavigateToHelp = { navigateTo(Screen.HowToHelp) })
                        }

                        Screen.HowToHelp -> {
                            HowToHelpScreen(onBack = { navigateBack() })
                        }

                        Screen.Statistics -> {
                            StatisticsScreen(
                                articles = articles,
                                locations = locations,
                                assignments = assignments,
                                onBack = { navigateBack() },
                                settings = settings
                            )
                        }

                        Screen.Settings -> {
                            SettingsScreen(
                                currentLocale = settings.language,
                                currentLogLevel = settings.logLevel,
                                backupOldFolderOnImport = settings.backupOldFolderOnImport,
                                enableExpirationDates = settings.enableExpirationDates,
                                fileHandler = fileHandler,
                                onBack = { navigateBack() },
                                onExportZip = {
                                    scope.launch {
                                        val exportFileName = fileHandler.createTimestampedFileName("mistermanager", "zip")
                                        fileDownloader.download(exportFileName, fileHandler.zipFiles(), getContext())
                                    }
                                },
                                onImport = { fileContent ->
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.Default) {
                                                jsonDataManager.importData(fileContent)
                                            }
                                            reloadAllData()
                                            snackbarHostState.showSnackbar("Import successful")
                                        } catch (e: Exception) {
                                            val errorMessage = "Failed to import data: ${e.message}. The data file might be corrupt."
                                            errorDialogMessage = errorMessage
                                            scope.launch {
                                                Logger.log(LogLevel.ERROR, "Failed to import data in onImport: ${e.message}", e)
                                            }
                                        }
                                    }
                                },
                                onImportZip = { zipInputStream ->
                                    scope.launch {
                                        try {
                                            if (settings.backupOldFolderOnImport) {
                                                val timestamp = getCurrentTimestamp()
                                                fileHandler.renameFilesDirectory("files_$timestamp") // backup for debugging in case of error
                                            } else {
                                                fileHandler.deleteFilesDirectory()
                                            }
                                            withContext(Dispatchers.Default) {
                                                fileHandler.unzipAndReplaceFiles(zipInputStream)
                                            }
                                            reloadAllData()
                                            snackbarHostState.showSnackbar("ZIP import successful")
                                        } catch (e: Exception) {
                                            val errorMessage = "Failed to import ZIP: ${e.message}"
                                            errorDialogMessage = errorMessage
                                            scope.launch {
                                                Logger.log(LogLevel.ERROR, "Failed to import ZIP in onImportZip: ${e.message}", e)
                                            }
                                        }
                                    }
                                },
                                onLocaleChange = { newLocale ->
                                    scope.launch {
                                        val newSettings = settings.copy(language = newLocale)
                                        withContext(Dispatchers.Default) {
                                            settingsManager.saveSettings(newSettings)
                                        }
                                        setAppLanguage(newLocale)
                                        settings = newSettings
                                    }
                                },
                                onLogLevelChange = { newLogLevel ->
                                    scope.launch {
                                        val newSettings = settings.copy(logLevel = newLogLevel)
                                        withContext(Dispatchers.Default) {
                                            settingsManager.saveSettings(newSettings)
                                        }
                                        settings = newSettings
                                    }
                                },
                                onBackupOldFolderOnImportChange = { newBackupOldFolderOnImport ->
                                    scope.launch {
                                        val newSettings = settings.copy(backupOldFolderOnImport = newBackupOldFolderOnImport)
                                        withContext(Dispatchers.Default) {
                                            settingsManager.saveSettings(newSettings)
                                        }
                                        settings = newSettings
                                    }
                                },
                                onEnableExpirationDatesChange = { newEnableExpirationDates ->
                                    scope.launch {
                                        val newSettings = settings.copy(enableExpirationDates = newEnableExpirationDates)
                                        withContext(Dispatchers.Default) {
                                            settingsManager.saveSettings(newSettings)
                                        }
                                        settings = newSettings
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
