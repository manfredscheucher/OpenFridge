package org.example.project

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Repository for managing articles, locations, and their assignments from a JSON file.
 */
class JsonDataManager(private val fileHandler: FileHandler, private val filePath: String = "stash.json") {

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
                appData
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
        return fileHandler.backupFile(filePath)
    }


    /**
     * Validates the given JSON content, backs up the current data file, and then overwrites it with the new content.
     * If the content is invalid, it throws a SerializationException.
     */
    suspend fun importData(content: String) {
        // First, validate the new content. This will throw if content is corrupt.
        val newData = Json.decodeFromString<AppData>(content)
        validateData(newData)

        // If validation is successful, then backup the existing file.
        fileHandler.backupFile(filePath)

        // Then, write the new content to the main file.
        fileHandler.writeText(filePath, content)

        // Finally, update the in-memory data.
        data = newData
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
    fun getArticleById(id: UInt): Article? = data.articles.firstOrNull { it.id == id }

    fun createNewArticle(defaultName: String): Article {
        val existingIds = data.articles.map { it.id }.toSet()
        var newId: UInt
        do {
            newId = Random.nextInt().toUInt()
        } while (existingIds.contains(newId))
        val articleName = defaultName.replace("%1\$d", newId.toString())
        return Article(
            id = newId,
            name = articleName,
            modified = getCurrentTimestamp()
        )
    }

    suspend fun addOrUpdateArticle(article: Article) {
        val index = data.articles.indexOfFirst { it.id == article.id }
        if (index != -1) {
            data.articles[index] = article
        } else {
            data.articles.add(article)
        }
        save()
    }

    suspend fun deleteArticle(id: UInt) {
        data.articles.removeAll { it.id == id }
        data.assignments.removeAll { it.articleId == id }
        save()
    }

    // Location management functions
    fun getLocationById(id: UInt): Location? = data.locations.firstOrNull { it.id == id }

    fun createNewLocation(defaultName: String): Location {
        val existingIds = data.locations.map { it.id }.toSet()
        var newId: UInt
        do {
            newId = Random.nextInt().toUInt()
        } while (existingIds.contains(newId))
        val locationName = defaultName.replace("%1\$d", newId.toString())
        return Location(
            id = newId,
            name = locationName
        )
    }

    suspend fun addOrUpdateLocation(location: Location) {
        val index = data.locations.indexOfFirst { it.id == location.id }
        if (index != -1) {
            data.locations[index] = location
        } else {
            data.locations.add(location)
        }
        save()
    }

    suspend fun deleteLocation(id: UInt) {
        data.locations.removeAll { it.id == id }
        data.assignments.removeAll { it.locationId == id }
        save()
    }

    // Assignment/Inventory management functions
    fun createNewAssignment(articleId: UInt, locationId: UInt): Assignment {
        val existingIds = data.assignments.map { it.id }.toSet()
        var newId: UInt
        do {
            newId = Random.nextInt().toUInt()
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
            amount = 0u,
            addedDate = today,
            expirationDate = expirationDate
        )
    }

    suspend fun setLocationAssignments(locationId: UInt, assignments: List<Assignment>) {
        // Remove existing assignments for this location
        data.assignments.removeAll { it.locationId == locationId }

        // Add new assignments
        for (assignment in assignments) {
            if (assignment.amount > 0u) {
                data.assignments.add(assignment)
            }
        }
        save()
    }

    suspend fun setArticleAssignments(articleId: UInt, assignments: List<Assignment>) {
        // Remove existing assignments for this article
        data.assignments.removeAll { it.articleId == articleId }

        // Add new assignments
        for (assignment in assignments) {
            if (assignment.amount > 0u) {
                data.assignments.add(assignment)
            }
        }
        save()
    }
}
