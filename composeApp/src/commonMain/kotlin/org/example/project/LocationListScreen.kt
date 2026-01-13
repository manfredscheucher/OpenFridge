package org.example.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import openfridge.composeapp.generated.resources.Res
import openfridge.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    locations: List<Location>,
    assignments: List<Assignment>,
    imageManager: ImageManager,
    settings: Settings,
    onAddClick: () -> Unit,
    onOpen: (UInt) -> Unit,
    onBack: () -> Unit,
    onSettingsChange: (Settings) -> Unit
) {
    BackButtonHandler {
        onBack()
    }

    var filter by remember { mutableStateOf("") }

    // Calculate total amount and earliest expiration for each location
    val locationStats = remember(locations, assignments) {
        locations.associate { location ->
            val locationAssignments = assignments.filter { it.locationId == location.id && it.removedDate == null }
            val totalAmount = locationAssignments.sumOf { it.amount }
            val earliestExpiration = locationAssignments
                .mapNotNull { it.expirationDate }
                .minOrNull()
            location.id to Pair(totalAmount, earliestExpiration)
        }
    }

    val filteredLocations = locations.filter {
        if (filter.isNotBlank()) {
            Json.encodeToString(it).contains(filter, ignoreCase = true)
        } else {
            true
        }
    }
    val sortedLocations = filteredLocations.sortedBy { it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.locations),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.location_list_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = stringResource(Res.string.common_plus_symbol),
                    style = MaterialTheme.typography.displayMedium
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text(stringResource(Res.string.location_list_filter)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(Res.string.location_list_filter)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            if (sortedLocations.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(stringResource(Res.string.location_list_empty))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    val state = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = state,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedLocations) { p ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpen(p.id) },
                                colors = CardDefaults.cardColors(containerColor = ColorPalette.idToColor(p.id))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
                                    LaunchedEffect(p.id, p.imageIds) {
                                        val imageId = p.imageIds.firstOrNull()
                                        imageBytes = if (imageId != null) {
                                            imageManager.getLocationImageThumbnail(p.id, imageId)
                                        } else {
                                            null
                                        }
                                    }
                                    val bitmap = remember(imageBytes) { imageBytes?.toImageBitmap() }

                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Location image for ${p.name}",
                                            modifier = Modifier.size(64.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(Res.drawable.locations),
                                            contentDescription = "Location icon",
                                            modifier = Modifier.size(64.dp).alpha(0.5f),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold)

                                        val (totalAmount, earliestExpiration) = locationStats[p.id] ?: Pair(0u, null)
                                        if (totalAmount > 0u) {
                                            val today = getCurrentDateString()
                                            val isExpired = earliestExpiration != null && earliestExpiration < today
                                            val amountText = if (earliestExpiration != null) {
                                                val warning = if (isExpired) " ⚠️" else ""
                                                "$totalAmount units (until $earliestExpiration)$warning"
                                            } else {
                                                "$totalAmount units"
                                            }
                                            Text(
                                                amountText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

