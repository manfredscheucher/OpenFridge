package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import openfridge.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    articles: List<Article>,
    locations: List<Location>,
    imageManager: ImageManager,
    assignments: List<Assignment>,
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
    var showMissingOnly by remember { mutableStateOf(false) }
    var showExpiringOnly by remember { mutableStateOf(false) }
    var expiryThresholdDays by remember { mutableStateOf(7u) }

    val barcodeScannerLauncher = rememberBarcodeScannerLauncher { barcode ->
        if (barcode != null) {
            filter = barcode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.articles),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.article_list_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back)
                        )
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (articles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(stringResource(Res.string.article_list_empty))
                }
            } else {
                // Calculate current amount and earliest expiration for each article
                val articleCurrentAmounts = remember(assignments) {
                    articles.associate { article ->
                        val totalAssigned = assignments
                            .filter { it.articleId == article.id }
                            .sumOf { it.amount }
                        article.id to totalAssigned
                    }
                }

                val articleEarliestExpiration = remember(assignments) {
                    articles.associate { article ->
                        val earliestExpiration = assignments
                            .filter { it.articleId == article.id && it.expirationDate != null }
                            .mapNotNull { it.expirationDate }
                            .minOrNull()
                        article.id to earliestExpiration
                    }
                }

                // Calculate which articles have expiring assignments
                val articlesWithExpiringAssignments = remember(assignments, expiryThresholdDays) {
                    val today = getCurrentDateString()
                    val thresholdDate = addDaysToDate(today, expiryThresholdDays)

                    articles.filter { article ->
                        assignments.any { assignment ->
                            assignment.articleId == article.id &&
                            assignment.expirationDate != null &&
                            assignment.expirationDate!! <= thresholdDate
                        }
                    }.map { it.id }.toSet()
                }

                val filteredArticles = articles.filter { article ->
                    // Filter by search term
                    val matchesSearch = if (filter.isNotBlank()) {
                        Json.encodeToString(article).contains(filter, ignoreCase = true)
                    } else {
                        true
                    }

                    // Filter by missing (current amount < minimum amount)
                    val matchesMissing = if (showMissingOnly) {
                        val currentAmount = articleCurrentAmounts[article.id] ?: 0u
                        currentAmount < article.minimumAmount
                    } else {
                        true
                    }

                    // Filter by expiring assignments
                    val matchesExpiring = if (showExpiringOnly) {
                        articlesWithExpiringAssignments.contains(article.id)
                    } else {
                        true
                    }

                    matchesSearch && matchesMissing && matchesExpiring
                }.sortedByDescending { it.modified ?: "" }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showMissingOnly,
                            onCheckedChange = { showMissingOnly = it }
                        )
                        Text(
                            text = stringResource(Res.string.article_list_show_missing),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showExpiringOnly,
                            onCheckedChange = { showExpiringOnly = it }
                        )
                        Text(
                            text = stringResource(Res.string.article_list_show_expired),
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        if (showExpiringOnly) {
                            var expanded by remember { mutableStateOf(false) }
                            Spacer(Modifier.width(16.dp))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = when (expiryThresholdDays) {
                                        0u -> stringResource(Res.string.article_list_expiry_today)
                                        7u -> stringResource(Res.string.article_list_expiry_1_week)
                                        14u -> stringResource(Res.string.article_list_expiry_2_weeks)
                                        21u -> stringResource(Res.string.article_list_expiry_3_weeks)
                                        28u -> stringResource(Res.string.article_list_expiry_4_weeks)
                                        else -> "$expiryThresholdDays days"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.article_list_expiry_today)) },
                                        onClick = {
                                            expiryThresholdDays = 0u
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.article_list_expiry_1_week)) },
                                        onClick = {
                                            expiryThresholdDays = 7u
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.article_list_expiry_2_weeks)) },
                                        onClick = {
                                            expiryThresholdDays = 14u
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.article_list_expiry_3_weeks)) },
                                        onClick = {
                                            expiryThresholdDays = 21u
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.article_list_expiry_4_weeks)) },
                                        onClick = {
                                            expiryThresholdDays = 28u
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        label = { Text(stringResource(Res.string.article_list_filter)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    if (barcodeScannerLauncher != null) {
                        Button(
                            onClick = { barcodeScannerLauncher.launch() },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Scan")
                        }
                    }
                }

                val state = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = state,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredArticles) { article ->
                        val currentAmount = articleCurrentAmounts[article.id] ?: 0u
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpen(article.id) },
                            colors = CardDefaults.cardColors(containerColor = ColorPalette.idToColor(article.id))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
                                LaunchedEffect(article.id, article.imageIds) {
                                    val imageId = article.imageIds.firstOrNull()
                                    imageBytes = if (imageId != null) {
                                        imageManager.getArticleImageThumbnail(article.id, imageId)
                                    } else {
                                        null
                                    }
                                }
                                val bitmap = remember(imageBytes) { imageBytes?.toImageBitmap() }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Article image for ${article.name}",
                                        modifier = Modifier.size(64.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(Res.drawable.articles),
                                        contentDescription = "Article icon",
                                        modifier = Modifier.size(64.dp).alpha(0.5f),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(article.name, fontWeight = FontWeight.Bold)
                                    article.brand?.let {
                                        Text(it, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    val isMissing = currentAmount < article.minimumAmount
                                    val earliestExpiration = articleEarliestExpiration[article.id]
                                    val amountText = if (earliestExpiration != null) {
                                        "Amount: $currentAmount (until $earliestExpiration)"
                                    } else {
                                        "Amount: $currentAmount"
                                    }
                                    val warningText = if (isMissing && article.minimumAmount > 0u) {
                                        " ⚠️ (minimum amount: ${article.minimumAmount})"
                                    } else {
                                        ""
                                    }
                                    Text(
                                        amountText + warningText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isMissing && article.minimumAmount > 0u) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
