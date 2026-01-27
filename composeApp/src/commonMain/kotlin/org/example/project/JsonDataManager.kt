package org.example.project

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * Repository for managing articles, locations, and their assignments from a JSON file.
 */
class JsonDataManager(private val fileHandler: FileHandler, private val filePath: String = "inventory.json") {

    private var data: AppData = AppData()

    /**
     * Loads data from the JSON file. If the file is empty or doesn't exist,
     * it initializes with default empty lists.
     */
    suspend fun load(): AppData {
        val content = fileHandler.readText(filePath)
        data = if (content.isNotEmpty()) {
            try {
                val appData = Json.decodeFromString<AppData>(content)
                validateData(appData)

                // Filter out deleted items
                val filteredData = appData.copy(
                    articles = appData.articles.filter { it.deleted != true }.toMutableList(),
                    locations = appData.locations.filter { it.deleted != true }.toMutableList(),
                    assignments = appData.assignments.filter { it.deleted != true }.toMutableList()
                )

                Logger.log(LogLevel.INFO, "Loaded data: ${filteredData.articles.size} articles, ${filteredData.locations.size} locations, ${filteredData.assignments.size} assignments (filtered out deleted items)")
                filteredData
            } catch (e: SerializationException) {
                Logger.log(LogLevel.ERROR, "Failed to decode JSON data in fun load: ${e.message}", e)
                // Re-throw the exception to be handled by the caller
                throw e
            } catch (e: Exception) {
                Logger.log(LogLevel.ERROR, "Failed to load data in fun load: ${e.message}", e)
                // Handle other exceptions
                throw e
            }
        } else {
            Logger.log(LogLevel.INFO, "No existing data file found, initialized empty AppData")
            AppData()
        }
        return data
    }

    /**
     * Saves the current application data to the JSON file.
     */
    private suspend fun save() {
        val content = Json.encodeToString(data)
        fileHandler.writeText(filePath, content)
    }

    /**
     * Provides the raw JSON content of the current data.
     */
    suspend fun getRawJson(): String {
        return fileHandler.readText(filePath)
    }

    /**
     * Backs up the current data file.
     * @return The name of the backup file, or null if backup failed.
     */
    suspend fun backup(): String? {
        return fileHandler.backupFile(filePath).also { backupName ->
            if (backupName != null) {
                Logger.log(LogLevel.INFO, "Created backup: $backupName")
            } else {
                Logger.log(LogLevel.WARN, "Backup failed")
            }
        }
    }


    /**
     * Validates the given JSON content, backs up the current data file, and then overwrites it with the new content.
     * If the content is invalid, it throws a SerializationException.
     */
    suspend fun importData(content: String) {
        // First, validate the new content. This will throw if content is corrupt.
        val newData = Json.decodeFromString<AppData>(content)
        validateData(newData)
        Logger.log(LogLevel.INFO, "Import validation successful: ${newData.articles.size} articles, ${newData.locations.size} locations, ${newData.assignments.size} assignments")

        // If validation is successful, then backup the existing file.
        val backupName = fileHandler.backupFile(filePath)
        Logger.log(LogLevel.INFO, "Created backup before import: $backupName")

        // Then, write the new content to the main file.
        fileHandler.writeText(filePath, content)

        // Finally, update the in-memory data.
        data = newData
        Logger.log(LogLevel.INFO, "Import completed successfully")
    }

    private fun validateData(appData: AppData) {
        val articleIds = appData.articles.map { it.id }.toSet()
        val locationIds = appData.locations.map { it.id }.toSet()

        for (article in appData.articles) {
            if (article.name.isBlank()) {
                throw SerializationException("Article with id ${article.id} has a blank name.")
            }
        }

        for (location in appData.locations) {
            if (location.name.isBlank()) {
                throw SerializationException("Location with id ${location.id} has a blank name.")
            }
        }

        for (assignment in appData.assignments) {
            if (!articleIds.contains(assignment.articleId)) {
                throw SerializationException("Assignment refers to a non-existent article with id ${assignment.articleId}.")
            }
            if (!locationIds.contains(assignment.locationId)) {
                throw SerializationException("Assignment refers to a non-existent location with id ${assignment.locationId}.")
            }
        }
    }

    // Article management functions
    fun getArticleById(id: UInt): Article? = data.articles.firstOrNull { it.id == id && it.deleted != true }

    fun createNewArticle(defaultName: String): Article {
        val existingIds = data.articles.map { it.id }.toSet()
        var newId: UInt
        do {
            newId = Random.nextUInt()
        } while (existingIds.contains(newId))
        val articleName = defaultName.replace("%1\$d", newId.toString())
        return Article(
            id = newId,
            name = articleName,
            modified = getCurrentTimestamp()
        ).also {
            GlobalScope.launch {
                Logger.log(LogLevel.INFO, "Created new article: id=${it.id}, name=${it.name}")
            }
        }
    }

    suspend fun addOrUpdateArticle(article: Article) {
        val index = data.articles.indexOfFirst { it.id == article.id }
        if (index != -1) {
            Logger.log(LogLevel.INFO, "Updated article: id=${article.id}, name=${article.name}")
            data.articles[index] = article
        } else {
            Logger.log(LogLevel.INFO, "Added article: id=${article.id}, name=${article.name}")
            data.articles.add(article)
        }
        save()
    }

    suspend fun deleteArticle(id: UInt) {
        val index = data.articles.indexOfFirst { it.id == id }
        if (index != -1) {
            val article = data.articles[index]
            data.articles[index] = article.copy(
                deleted = true,
                modified = getCurrentTimestamp()
            )

            // Also mark related assignments as deleted
            val timestamp = getCurrentTimestamp()
            val assignmentsCount = data.assignments.count { it.articleId == id && it.deleted != true }
            data.assignments.forEachIndexed { idx, assignment ->
                if (assignment.articleId == id && assignment.deleted != true) {
                    data.assignments[idx] = assignment.copy(
                        deleted = true,
                        lastModified = timestamp
                    )
                }
            }
            Logger.log(LogLevel.INFO, "Marked article as deleted: id=$id, name=${article.name}, marked $assignmentsCount assignments as deleted")
            save()
        }
    }

    // Location management functions
    fun getLocationById(id: UInt): Location? = data.locations.firstOrNull { it.id == id && it.deleted != true }

    fun createNewLocation(defaultName: String): Location {
        val existingIds = data.locations.map { it.id }.toSet()
        var newId: UInt
        do {
            newId = Random.nextUInt()
        } while (existingIds.contains(newId))
        val locationName = defaultName.replace("%1\$d", newId.toString())
        return Location(
            id = newId,
            name = locationName
        ).also {
            GlobalScope.launch {
                Logger.log(LogLevel.INFO, "Created new location: id=${it.id}, name=${it.name}")
            }
        }
    }

    suspend fun addOrUpdateLocation(location: Location) {
        val index = data.locations.indexOfFirst { it.id == location.id }
        if (index != -1) {
            Logger.log(LogLevel.INFO, "Updated location: id=${location.id}, name=${location.name}")
            data.locations[index] = location
        } else {
            Logger.log(LogLevel.INFO, "Added location: id=${location.id}, name=${location.name}")
            data.locations.add(location)
        }
        save()
    }

    suspend fun deleteLocation(id: UInt) {
        val index = data.locations.indexOfFirst { it.id == id }
        if (index != -1) {
            val location = data.locations[index]
            data.locations[index] = location.copy(
                deleted = true
            )

            // Also mark related assignments as deleted
            val timestamp = getCurrentTimestamp()
            val assignmentsCount = data.assignments.count { it.locationId == id && it.deleted != true }
            data.assignments.forEachIndexed { idx, assignment ->
                if (assignment.locationId == id && assignment.deleted != true) {
                    data.assignments[idx] = assignment.copy(
                        deleted = true,
                        lastModified = timestamp
                    )
                }
            }
            Logger.log(LogLevel.INFO, "Marked location as deleted: id=$id, name=${location.name}, marked $assignmentsCount assignments as deleted")
            save()
        }
    }

    // Assignment/Inventory management functions
    fun createNewAssignment(articleId: UInt, locationId: UInt): Assignment {
        val existingIds = data.assignments.map { it.id }.toSet()
        var newId: UInt
        do {
            newId = Random.nextUInt()
        } while (existingIds.contains(newId))

        val article = getArticleById(articleId)
        val today = getCurrentDateString()
        val expirationDate = article?.defaultExpirationDays?.let { days ->
            if (days > 0u) addDaysToDate(today, days) else null
        }

        return Assignment(
            id = newId,
            articleId = articleId,
            locationId = locationId,
            amount = 1u,
            addedDate = today,
            expirationDate = expirationDate,
            lastModified = getCurrentTimestamp()
        ).also {
            GlobalScope.launch {
                Logger.log(LogLevel.INFO, "Created new assignment: id=${it.id}, articleId=$articleId, locationId=$locationId")
            }
        }
    }

    suspend fun setLocationAssignments(locationId: UInt, assignments: List<Assignment>) {
        // Remove existing assignments for this location
        val removedCount = data.assignments.count { it.locationId == locationId }
        data.assignments.removeAll { it.locationId == locationId }
        Logger.log(LogLevel.INFO, "Removed $removedCount existing assignments for locationId=$locationId")

        // Add new assignments with updated lastModified timestamp
        val timestamp = getCurrentTimestamp()
        var addedCount = 0
        for (assignment in assignments) {
            if (assignment.amount > 0u) {
                data.assignments.add(assignment.copy(lastModified = timestamp))
                Logger.log(LogLevel.INFO, "Updated assignment: id=${assignment.id}, articleId=${assignment.articleId}, locationId=$locationId, amount=${assignment.amount}")
                addedCount++
            }
        }
        Logger.log(LogLevel.INFO, "Added $addedCount assignments for locationId=$locationId")
        save()
    }

    suspend fun setArticleAssignments(articleId: UInt, assignments: List<Assignment>) {
        // Remove existing assignments for this article
        val removedCount = data.assignments.count { it.articleId == articleId }
        data.assignments.removeAll { it.articleId == articleId }
        Logger.log(LogLevel.INFO, "Removed $removedCount existing assignments for articleId=$articleId")

        // Add new assignments with updated lastModified timestamp
        val timestamp = getCurrentTimestamp()
        var addedCount = 0
        for (assignment in assignments) {
            if (assignment.amount > 0u) {
                data.assignments.add(assignment.copy(lastModified = timestamp))
                Logger.log(LogLevel.INFO, "Updated assignment: id=${assignment.id}, articleId=$articleId, locationId=${assignment.locationId}, amount=${assignment.amount}")
                addedCount++
            }
        }
        Logger.log(LogLevel.INFO, "Added $addedCount assignments for articleId=$articleId")
        save()
    }
}
