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
import org.example.project.components.IntegerInput
import org.jetbrains.compose.resources.stringResource
import openfridge.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleAssignmentsScreen(
    articleName: String,
    articleId: UInt,
    allLocations: List<Location>,
    initialAssignments: List<Assignment>,
    settings: Settings,
    onCreateNewAssignment: (articleId: UInt, locationId: UInt) -> Assignment,
    onSave: (updatedAssignments: List<Assignment>) -> Unit,
    onBack: () -> Unit
) {
    var currentAssignments by remember { mutableStateOf(initialAssignments.toMutableList()) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRemoved by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var assignmentToDelete by remember { mutableStateOf<Int?>(null) }

    // Store initial consumedDate for each assignment to determine visibility
    val initialConsumedDates = remember(initialAssignments) {
        initialAssignments.associate { it.id to it.consumedDate }
    }

    val hasChanges by remember(currentAssignments) {
        derivedStateOf { currentAssignments != initialAssignments }
    }

    // Group assignments by location, filtering based on initial consumedDate
    val assignmentsByLocation = remember(currentAssignments, allLocations, showRemoved) {
        allLocations.associate { location ->
            location to currentAssignments.filter { assignment ->
                assignment.locationId == location.id &&
                (showRemoved || initialConsumedDates[assignment.id] == null)
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
                title = { Text("$articleName - ${stringResource(Res.string.assignment_locations_title)}") },
                navigationIcon = {
                    IconButton(onClick = backAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (allLocations.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(stringResource(Res.string.location_list_empty))
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
                    item(key = "show_consumed_toggle") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showRemoved,
                                onCheckedChange = { showRemoved = it }
                            )
                            Text(
                                text = stringResource(Res.string.assignment_show_consumed),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    allLocations.forEach { location ->
                        item(key = "header_${location.id}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    location.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val newAssignment = onCreateNewAssignment(articleId, location.id)
                                    currentAssignments = (currentAssignments + newAssignment).toMutableList()
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add assignment")
                                }
                            }
                        }

                        val locationAssignments = assignmentsByLocation[location] ?: emptyList()
                        items(locationAssignments, key = { it.id.toLong() }) { assignment ->
                            val assignmentIndex = currentAssignments.indexOfFirst { it.id == assignment.id }
                            if (assignmentIndex >= 0) {
                                AssignmentRow(
                                    assignment = currentAssignments[assignmentIndex],
                                    allAssignments = currentAssignments,
                                    settings = settings,
                                    onUpdate = { updated ->
                                        currentAssignments = currentAssignments.toMutableList().apply {
                                            this[assignmentIndex] = updated
                                        }
                                    },
                                    onConsume = {
                                        val today = getCurrentDateString()
                                        currentAssignments = currentAssignments.toMutableList().apply {
                                            this[assignmentIndex] = this[assignmentIndex].copy(consumedDate = today)
                                        }
                                    },
                                    onSplit = {
                                        val assignment = currentAssignments[assignmentIndex]
                                        val newAssignments = mutableListOf<Assignment>()
                                        // Create individual assignments with amount=1
                                        repeat(assignment.amount.toInt()) {
                                            val existingIds = (currentAssignments + newAssignments).map { it.id }.toSet()
                                            var newId: UInt
                                            do {
                                                newId = kotlin.random.Random.nextInt().toUInt()
                                            } while (existingIds.contains(newId))

                                            newAssignments.add(assignment.copy(
                                                id = newId,
                                                amount = 1u
                                            ))
                                        }
                                        // Remove original and add split assignments
                                        currentAssignments = currentAssignments.toMutableList().apply {
                                            removeAt(assignmentIndex)
                                            addAll(newAssignments)
                                        }
                                    },
                                    onMerge = {
                                        val assignment = currentAssignments[assignmentIndex]
                                        // Find all assignments with same attributes
                                        val toMerge = currentAssignments.filter { other ->
                                            other.articleId == assignment.articleId &&
                                            other.locationId == assignment.locationId &&
                                            other.addedDate == assignment.addedDate &&
                                            other.expirationDate == assignment.expirationDate &&
                                            other.consumedDate == assignment.consumedDate
                                        }
                                        if (toMerge.size > 1) {
                                            val totalAmount = toMerge.sumOf { it.amount }
                                            val mergedAssignment = assignment.copy(amount = totalAmount)
                                            // Remove all merged assignments and add single merged one
                                            currentAssignments = currentAssignments.toMutableList().apply {
                                                removeAll(toMerge)
                                                add(mergedAssignment)
                                            }
                                        }
                                    },
                                    onDelete = {
                                        assignmentToDelete = assignmentIndex
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }

                        item(key = "divider_${location.id}") {
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
    allAssignments: List<Assignment>,
    settings: Settings,
    onUpdate: (Assignment) -> Unit,
    onConsume: () -> Unit,
    onSplit: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Check if merge is possible
    val canMerge = remember(allAssignments, assignment) {
        allAssignments.count { other ->
            other.articleId == assignment.articleId &&
            other.locationId == assignment.locationId &&
            other.addedDate == assignment.addedDate &&
            other.expirationDate == assignment.expirationDate &&
            other.consumedDate == assignment.consumedDate
        } > 1
    }

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
                IntegerInput(
                    value = assignment.amount,
                    onValueChange = { newAmount -> onUpdate(assignment.copy(amount = newAmount)) },
                    label = stringResource(Res.string.assignment_amount_label),
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
                        if (assignment.amount > 1u) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.assignment_split)) },
                                onClick = {
                                    showMenu = false
                                    onSplit()
                                }
                            )
                        }
                        if (canMerge) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.assignment_merge)) },
                                onClick = {
                                    showMenu = false
                                    onMerge()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.assignment_consume)) },
                            onClick = {
                                showMenu = false
                                onConsume()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.common_delete)) },
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
                    label = { Text(stringResource(Res.string.assignment_added_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (settings.enableExpirationDates) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = assignment.expirationDate ?: "",
                        onValueChange = { onUpdate(assignment.copy(expirationDate = it.takeIf { it.isNotBlank() })) },
                        label = { Text(stringResource(Res.string.assignment_best_before_date)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = assignment.consumedDate ?: "",
                    onValueChange = { onUpdate(assignment.copy(consumedDate = it.takeIf { it.isNotBlank() })) },
                    label = { Text(stringResource(Res.string.assignment_consumed_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
