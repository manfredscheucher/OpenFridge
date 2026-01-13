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
                // Calculate current amount for each article from assignments
                val articleCurrentAmounts = remember(assignments) {
                    articles.associate { article ->
                        val totalAssigned = assignments
                            .filter { it.articleId == article.id }
                            .sumOf { it.amount }
                        article.id to totalAssigned
                    }
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

                    matchesSearch && matchesMissing
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

                }

                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text(stringResource(Res.string.article_list_filter)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

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
                                    Text(
                                        "Amount: $currentAmount / ${article.minimumAmount}${if (isMissing && article.minimumAmount > 0u) " ⚠️" else ""}",
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
