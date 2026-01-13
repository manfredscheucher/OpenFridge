package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import openfridge.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAssignmentsScreen(
    locationName: String,
    locationId: UInt,
    allArticles: List<Article>,
    initialAssignments: List<Assignment>,
    onCreateNewAssignment: (articleId: UInt, locationId: UInt) -> Assignment,
    onSave: (updatedAssignments: List<Assignment>) -> Unit,
    onBack: () -> Unit
) {
    var currentAssignments by remember { mutableStateOf(initialAssignments.toMutableList()) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRemoved by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var assignmentToDelete by remember { mutableStateOf<Int?>(null) }

    // Store initial removedDate for each assignment to determine visibility
    val initialRemovedDates = remember(initialAssignments) {
        initialAssignments.associate { it.id to it.removedDate }
    }

    val hasChanges by remember(currentAssignments) {
        derivedStateOf { currentAssignments != initialAssignments }
    }

    // Group assignments by article, filtering based on initial removedDate
    val assignmentsByArticle = remember(currentAssignments, allArticles, showRemoved) {
        allArticles.associate { article ->
            article to currentAssignments.filter { assignment ->
                assignment.articleId == article.id &&
                (showRemoved || initialRemovedDates[assignment.id] == null)
            }.toMutableList()
        }
    }

    val backAction = {
        if (hasChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackButtonHandler {
        backAction()
    }

    val saveAction = {
        val finalAssignments = currentAssignments.filter { it.amount > 0u }
        onSave(finalAssignments)
        onBack()
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(Res.string.form_unsaved_changes_title)) },
            text = { Text(stringResource(Res.string.form_unsaved_changes_message)) },
            confirmButton = {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text(stringResource(Res.string.common_stay))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onBack()
                    }) {
                        Text(stringResource(Res.string.common_no))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        saveAction()
                        showUnsavedDialog = false
                    }) {
                        Text(stringResource(Res.string.common_yes))
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.form_delete_assignment_title)) },
            text = { Text(stringResource(Res.string.form_delete_assignment_message)) },
            confirmButton = {
                TextButton(onClick = {
                    assignmentToDelete?.let { index ->
                        currentAssignments = currentAssignments.toMutableList().apply {
                            removeAt(index)
                        }
                    }
                    showDeleteDialog = false
                    assignmentToDelete = null
                }) {
                    Text(stringResource(Res.string.common_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    assignmentToDelete = null
                }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$locationName - Assignments") },
                navigationIcon = {
                    IconButton(onClick = backAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (allArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(stringResource(Res.string.article_list_empty))
            }
        } else {
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                val state = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .navigationBarsPadding(),
                    state = state,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item(key = "show_removed_toggle") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showRemoved,
                                onCheckedChange = { showRemoved = it }
                            )
                            Text(
                                text = "Show Removed",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    allArticles.forEach { article ->
                        item(key = "header_${article.id}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    article.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val newAssignment = onCreateNewAssignment(article.id, locationId)
                                    currentAssignments = (currentAssignments + newAssignment).toMutableList()
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add assignment")
                                }
                            }
                        }

                        val articleAssignments = assignmentsByArticle[article] ?: emptyList()
                        items(articleAssignments, key = { it.id.toLong() }) { assignment ->
                            val assignmentIndex = currentAssignments.indexOfFirst { it.id == assignment.id }
                            if (assignmentIndex >= 0) {
                                AssignmentRow(
                                    assignment = currentAssignments[assignmentIndex],
                                    onUpdate = { updated ->
                                        currentAssignments = currentAssignments.toMutableList().apply {
                                            this[assignmentIndex] = updated
                                        }
                                    },
                                    onRemove = {
                                        val today = getCurrentDateString()
                                        currentAssignments = currentAssignments.toMutableList().apply {
                                            this[assignmentIndex] = this[assignmentIndex].copy(removedDate = today)
                                        }
                                    },
                                    onDelete = {
                                        assignmentToDelete = assignmentIndex
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }

                        item(key = "divider_${article.id}") {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = backAction) { Text(stringResource(Res.string.common_cancel)) }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = saveAction) { Text(stringResource(Res.string.common_save)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentRow(
    assignment: Assignment,
    onUpdate: (Assignment) -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = assignment.amount.toString(),
                    onValueChange = { textValue ->
                        val numericValue = textValue.toUIntOrNull()
                        if (numericValue != null) {
                            onUpdate(assignment.copy(amount = numericValue))
                        }
                    },
                    label = { Text("Amount (units)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (assignment.amount > 0u) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = assignment.addedDate ?: "",
                    onValueChange = { onUpdate(assignment.copy(addedDate = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Added Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = assignment.expirationDate ?: "",
                    onValueChange = { onUpdate(assignment.copy(expirationDate = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Expiration Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = assignment.removedDate ?: "",
                    onValueChange = { onUpdate(assignment.copy(removedDate = it.takeIf { it.isNotBlank() })) },
                    label = { Text("Removed Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
