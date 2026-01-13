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
fun LocationFormScreen(
    initial: Location,
    initialImages: Map<UInt, ByteArray>,
    assignmentsForLocation: List<Assignment>,
    articleById: (UInt) -> Article?,
    imageManager: ImageManager,
    onBack: () -> Unit,
    onDelete: (UInt) -> Unit,
    onSave: (Location, Map<UInt, ByteArray>) -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToArticle: (UInt) -> Unit
) {
    val isNewLocation = initial.id == 0u

    var name by remember { mutableStateOf(initial.name) }
    var notes by remember { mutableStateOf(initial.notes ?: "") }
    var showDeleteRestrictionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var onConfirmUnsaved by remember { mutableStateOf<(() -> Unit)?>(null) }

    val images = remember { mutableStateMapOf<UInt, ByteArray>() }
    LaunchedEffect(initialImages) {
        images.clear()
        images.putAll(initialImages)
    }
    var nextTempId by remember(initial.id) { mutableStateOf((initial.imageIds.maxOrNull() ?: 0u) + 1u) }
    var selectedImageId by remember(initial.id) { mutableStateOf(initial.imageIds.firstOrNull()) }

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
                     notes != (initial.notes ?: "") ||
                     images.keys.toSet() != initial.imageIds.toSet()

    fun saveLocation() {
        val updatedLocation = initial.copy(
            name = name,
            notes = notes.takeIf { it.isNotBlank() },
            imageIds = images.keys.toList()
        )
        onSave(updatedLocation, images.toMap())
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
                title = { Text(if (isNewLocation) stringResource(Res.string.location_form_new) else stringResource(Res.string.location_form_edit)) },
                navigationIcon = {
                    IconButton(onClick = backAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
                actions = {
                    if (!isNewLocation) {
                        TextButton(onClick = {
                            if (assignmentsForLocation.isNotEmpty()) {
                                showDeleteRestrictionDialog = true
                            } else {
                                showDeleteDialog = true
                            }
                        }) {
                            Text(stringResource(Res.string.common_delete))
                        }
                    }
                    TextButton(onClick = { saveLocation(); onBack() }, enabled = name.isNotBlank()) {
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
                            contentDescription = "Selected location image",
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.locations),
                            contentDescription = "Location icon",
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }

            item {
                Text(stringResource(Res.string.location_form_select_image), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { imagePicker.launch() }) {
                        Text(stringResource(Res.string.location_form_select_image))
                    }
                    Button(onClick = { cameraLauncher?.launch() }) {
                        Text(stringResource(Res.string.location_form_take_image))
                    }
                    if (selectedImageId != null) {
                        Button(onClick = {
                            selectedImageId?.let { id ->
                                images.remove(id)
                                selectedImageId = images.keys.firstOrNull()
                            }
                        }) {
                            Text(stringResource(Res.string.location_form_remove_image))
                        }
                    }
                }
            }

            if (images.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                        contentDescription = "Location image",
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
                    label = { Text(stringResource(Res.string.location_label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(Res.string.location_label_notes)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }

            item {
                Text(stringResource(Res.string.assignment_articles_title), style = MaterialTheme.typography.titleMedium)
            }

            if (assignmentsForLocation.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.assignment_none),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(assignmentsForLocation) { assignment ->
                    val article = articleById(assignment.articleId)
                    if (article != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { confirmDiscardChanges { onNavigateToArticle(article.id) } }
                        ) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                var articleImageBytes by remember { mutableStateOf<ByteArray?>(null) }
                                LaunchedEffect(article.id, article.imageIds) {
                                    val imageId = article.imageIds.firstOrNull()
                                    articleImageBytes = if (imageId != null) {
                                        imageManager.getArticleImageThumbnail(article.id, imageId)
                                    } else {
                                        null
                                    }
                                }
                                val articleBitmap = remember(articleImageBytes) { articleImageBytes?.toImageBitmap() }

                                if (articleBitmap != null) {
                                    Image(
                                        bitmap = articleBitmap,
                                        contentDescription = "Article image",
                                        modifier = Modifier.size(48.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(Res.drawable.articles),
                                        contentDescription = "Article icon",
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(article.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("${assignment.amount} units", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        Text("- ERROR: articleId ${assignment.articleId} does not exist!")
                    }
                }
            }

            item {
                Button(
                    onClick = onNavigateToAssignments,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.location_form_button_assignments))
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
                    saveLocation()
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

    if (showDeleteRestrictionDialog) {
        DeleteRestrictionDialog(onDismiss = { showDeleteRestrictionDialog = false })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.form_delete_location_title)) },
            text = { Text(stringResource(Res.string.form_delete_location_message)) },
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

@Composable
private fun DeleteRestrictionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.delete_location_restricted_title)) },
        text = { Text(stringResource(Res.string.delete_location_restricted_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_ok)) }
        }
    )
}
