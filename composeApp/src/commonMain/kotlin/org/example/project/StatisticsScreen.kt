package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mistermanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class MonthlyStats(
    val month: String,
    val added: UInt,
    val consumed: UInt
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    articles: List<Article>,
    locations: List<Location>,
    assignments: List<Assignment>,
    settings: Settings,
    onBack: () -> Unit
) {
    BackButtonHandler { onBack() }

    var selectedYear by remember { mutableStateOf<String?>(null) }
    var selectedLocationId by remember { mutableStateOf<UInt?>(null) }
    var selectedArticleId by remember { mutableStateOf<UInt?>(null) }

    // Extract all years from assignments
    val availableYears = remember(assignments) {
        val years = mutableSetOf<String>()
        assignments.forEach { assignment ->
            assignment.addedDate?.let { years.add(it.substring(0, 4)) }
            assignment.consumedDate?.let { years.add(it.substring(0, 4)) }
        }
        years.sorted().reversed()
    }

    // Filter assignments
    val filteredAssignments = remember(assignments, selectedYear, selectedLocationId, selectedArticleId) {
        assignments.filter { assignment ->
            val matchesLocation = selectedLocationId == null || assignment.locationId == selectedLocationId
            val matchesArticle = selectedArticleId == null || assignment.articleId == selectedArticleId
            matchesLocation && matchesArticle
        }
    }

    // Calculate monthly statistics
    val monthlyStats = remember(filteredAssignments, selectedYear) {
        val stats = mutableMapOf<String, MonthlyStats>()

        val yearFilter = selectedYear
        if (yearFilter != null) {
            // Show monthly breakdown for selected year
            for (month in 1..12) {
                val monthKey = "%04d-%02d".format(yearFilter.toInt(), month)
                stats[monthKey] = MonthlyStats(monthKey, 0u, 0u)
            }
        }

        filteredAssignments.forEach { assignment ->
            // Count added
            assignment.addedDate?.let { date ->
                val yearFilterString = selectedYear
                if (yearFilterString == null || date.startsWith(yearFilterString)) {
                    val key = if (yearFilterString != null) date.substring(0, 7) else date.substring(0, 4)
                    val current = stats[key] ?: MonthlyStats(key, 0u, 0u)
                    stats[key] = current.copy(added = current.added + assignment.amount)
                }
            }

            // Count consumed
            assignment.consumedDate?.let { date ->
                val yearFilterString = selectedYear
                if (yearFilterString == null || date.startsWith(yearFilterString)) {
                    val key = if (yearFilterString != null) date.substring(0, 7) else date.substring(0, 4)
                    val current = stats[key] ?: MonthlyStats(key, 0u, 0u)
                    stats[key] = current.copy(consumed = current.consumed + assignment.amount)
                }
            }
        }

        stats.values.sortedBy { it.month }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.statistics),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.statistics_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back)
                        )
                    }
                },
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters
            item {
                Text("Filters", style = MaterialTheme.typography.titleMedium)
            }

            item {
                var expandedYear by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedYear,
                    onExpandedChange = { expandedYear = it }
                ) {
                    OutlinedTextField(
                        value = selectedYear ?: "All Years",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Years") },
                            onClick = {
                                selectedYear = null
                                expandedYear = false
                            }
                        )
                        availableYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year) },
                                onClick = {
                                    selectedYear = year
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                var expandedLocation by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedLocation,
                    onExpandedChange = { expandedLocation = it }
                ) {
                    OutlinedTextField(
                        value = selectedLocationId?.let { id -> locations.find { it.id == id }?.name } ?: "All Locations",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Location") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLocation) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLocation,
                        onDismissRequest = { expandedLocation = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Locations") },
                            onClick = {
                                selectedLocationId = null
                                expandedLocation = false
                            }
                        )
                        locations.sortedBy { it.name }.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    selectedLocationId = location.id
                                    expandedLocation = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                var expandedArticle by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedArticle,
                    onExpandedChange = { expandedArticle = it }
                ) {
                    OutlinedTextField(
                        value = selectedArticleId?.let { id -> articles.find { it.id == id }?.name } ?: "All Articles",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Article") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedArticle) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedArticle,
                        onDismissRequest = { expandedArticle = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Articles") },
                            onClick = {
                                selectedArticleId = null
                                expandedArticle = false
                            }
                        )
                        articles.sortedBy { it.name }.forEach { article ->
                            DropdownMenuItem(
                                text = { Text(article.name) },
                                onClick = {
                                    selectedArticleId = article.id
                                    expandedArticle = false
                                }
                            )
                        }
                    }
                }
            }

            // Statistics Table
            item {
                Spacer(Modifier.height(16.dp))
                Text("Monthly Statistics", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Period", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Text("Added", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Text("Consumed", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        monthlyStats.forEach { stat ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(stat.month, modifier = Modifier.weight(1f))
                                Text(stat.added.toString(), modifier = Modifier.weight(1f))
                                Text(stat.consumed.toString(), modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            // TODO: Add chart visualization here
        }
    }
}
