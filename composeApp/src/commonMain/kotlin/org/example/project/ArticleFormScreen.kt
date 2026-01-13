package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import openfridge.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleFormScreen(
    initial: Article,
    initialImages: Map<UInt, ByteArray>,
    assignmentsForArticle: List<Assignment>,
    allLocations: List<Location>,
    locationById: (UInt) -> Location?,
    imageManager: ImageManager,
    settings: Settings,
    onBack: () -> Unit,
    onDelete: (UInt) -> Unit,
    onSave: (Article, Map<UInt, ByteArray>) -> Unit,
    onAddColor: (Article) -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToLocation: (UInt) -> Unit
) {
    val isNewArticle = initial.id == 0u

    var name by remember { mutableStateOf(initial.name) }
    var brand by remember { mutableStateOf(initial.brand ?: "") }
    var abbreviation by remember { mutableStateOf(initial.abbreviation ?: "") }
    var minimumAmount by remember { mutableStateOf(initial.minimumAmount.toString()) }
    var defaultExpirationDays by remember { mutableStateOf(initial.defaultExpirationDays?.toString() ?: "") }
    var notes by remember { mutableStateOf(initial.notes ?: "") }
    val modifiedState by remember { mutableStateOf(initial.modified ?: getCurrentTimestamp()) }

    val images = remember { mutableStateMapOf<UInt, ByteArray>() }
    LaunchedEffect(initialImages) {
        images.clear()
        images.putAll(initialImages)
    }
    var nextTempId by remember(initial.id) { mutableStateOf((initial.imageIds.maxOrNull() ?: 0u) + 1u) }
    var selectedImageId by remember(initial.id) { mutableStateOf(initial.imageIds.firstOrNull()) }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var onConfirmUnsaved by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()

    val imagePicker = rememberImagePickerLauncher { newImageBytes ->
        newImageBytes.forEach { bytes ->
            val newId = nextTempId++
            images[newId] = bytes
            scope.launch {
                Logger.log(LogLevel.DEBUG, "Image added with id: $newId")
            }
        }
    }

    val cameraLauncher = rememberCameraLauncher { result ->
        result?.let {
            val newId = nextTempId++
            images[newId] = it
            scope.launch {
                Logger.log(LogLevel.DEBUG, "Image added with id: $newId")
            }
        }
    }

    val hasChanges = name != initial.name ||
                     brand != (initial.brand ?: "") ||
                     abbreviation != (initial.abbreviation ?: "") ||
                     minimumAmount != initial.minimumAmount.toString() ||
                     defaultExpirationDays != (initial.defaultExpirationDays?.toString() ?: "") ||
                     notes != (initial.notes ?: "") ||
                     images.keys.toSet() != initial.imageIds.toSet()

    fun saveArticle() {
        val updatedArticle = initial.copy(
            name = name,
            brand = brand.takeIf { it.isNotBlank() },
            abbreviation = abbreviation.takeIf { it.isNotBlank() },
            minimumAmount = minimumAmount.toUIntOrNull() ?: 0u,
            defaultExpirationDays = defaultExpirationDays.toUIntOrNull(),
            notes = notes.takeIf { it.isNotBlank() },
            modified = modifiedState,
            imageIds = images.keys.toList()
        )
        onSave(updatedArticle, images.toMap())
    }

    fun confirmDiscardChanges(action: () -> Unit) {
        if (hasChanges) {
            onConfirmUnsaved = action
            showUnsavedDialog = true
        } else {
            action()
        }
    }

    val backAction = {
        confirmDiscardChanges { onBack() }
    }

    BackButtonHandler { backAction() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewArticle) stringResource(Res.string.article_form_new) else stringResource(Res.string.article_form_edit)) },
                navigationIcon = {
                    IconButton(onClick = backAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
                actions = {
                    if (!isNewArticle) {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text(stringResource(Res.string.common_delete))
                        }
                    }
                    TextButton(onClick = { saveArticle(); onBack() }, enabled = name.isNotBlank()) {
                        Text(stringResource(Res.string.common_save))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val selectedBytes = selectedImageId?.let { images[it] }
                val selectedBitmap = selectedBytes?.toImageBitmap()
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap,
                            contentDescription = "Selected article image",
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.articles),
                            contentDescription = "Article icon",
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }

            item {
                Text("Images", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { imagePicker.launch() }) {
                        Text(stringResource(Res.string.common_select_image))
                    }
                    cameraLauncher?.let {
                        Button(onClick = { it.launch() }) {
                            Text(stringResource(Res.string.location_form_take_image))
                        }
                    }
                    if (selectedImageId != null) {
                        Button(onClick = {
                            selectedImageId?.let { id ->
                                images.remove(id)
                                selectedImageId = images.keys.firstOrNull()
                            }
                        }) {
                            Text("Remove")
                        }
                    }
                }
            }

            if (images.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(images.keys.toList()) { imageId ->
                            val imageBytes = images[imageId]
                            val bitmap = imageBytes?.toImageBitmap()
                            if (bitmap != null) {
                                Card(
                                    onClick = { selectedImageId = imageId },
                                    modifier = Modifier.size(100.dp),
                                    colors = if (selectedImageId == imageId) {
                                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    } else {
                                        CardDefaults.cardColors()
                                    }
                                ) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Article image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.article_label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = abbreviation,
                    onValueChange = { abbreviation = it },
                    label = { Text("Abbreviation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(Res.string.article_label_brand)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = minimumAmount,
                    onValueChange = { minimumAmount = it.filter { c -> c.isDigit() } },
                    label = { Text("Minimum Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = defaultExpirationDays,
                    onValueChange = { defaultExpirationDays = it.filter { c -> c.isDigit() } },
                    label = { Text("Default Expiration Days") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(Res.string.article_label_notes)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }

            item {
                Text(stringResource(Res.string.assignment_locations_title), style = MaterialTheme.typography.titleMedium)
            }

            if (assignmentsForArticle.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.assignment_none),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(assignmentsForArticle) { assignment ->
                    val location = locationById(assignment.locationId)
                    if (location != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { confirmDiscardChanges { onNavigateToLocation(location.id) } }
                        ) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                var locationImageBytes by remember { mutableStateOf<ByteArray?>(null) }
                                LaunchedEffect(location.id, location.imageIds) {
                                    val imageId = location.imageIds.firstOrNull()
                                    locationImageBytes = if (imageId != null) {
                                        imageManager.getLocationImageThumbnail(location.id, imageId)
                                    } else {
                                        null
                                    }
                                }
                                val locationBitmap = remember(locationImageBytes) { locationImageBytes?.toImageBitmap() }

                                if (locationBitmap != null) {
                                    Image(
                                        bitmap = locationBitmap,
                                        contentDescription = "Location image",
                                        modifier = Modifier.size(48.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(Res.drawable.locations),
                                        contentDescription = "Location icon",
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(location.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("${assignment.amount} units", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        Text("- ERROR: locationId ${assignment.locationId} does not exist!")
                    }
                }
            }

            item {
                Button(
                    onClick = onNavigateToAssignments,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Assignments")
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(Res.string.form_unsaved_changes_title)) },
            text = { Text(stringResource(Res.string.form_unsaved_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    saveArticle()
                    showUnsavedDialog = false
                    onConfirmUnsaved?.invoke()
                }) {
                    Text(stringResource(Res.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onConfirmUnsaved?.invoke()
                }) {
                    Text(stringResource(Res.string.common_no))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.form_delete_article_title)) },
            text = { Text(stringResource(Res.string.form_delete_article_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(initial.id)
                }) {
                    Text(stringResource(Res.string.common_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}
