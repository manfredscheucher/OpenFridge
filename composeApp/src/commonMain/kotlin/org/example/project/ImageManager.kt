package org.example.project

class ImageManager(private val fileHandler: FileHandler) {

    companion object {
        private const val THUMBNAIL_WIDTH = 256
        private const val THUMBNAIL_HEIGHT = 256
    }
    private val locationImagesDir = "img/location"
    private val locationImageThumbnailsDir = "img/location/thumbnails"
    private val articleImagesDir = "img/article"
    private val articleImageThumbnailsDir = "img/article/thumbnails"

    private fun getLocationImagePath(locationId: UInt, imageId: UInt) = "$locationImagesDir/${locationId}_$imageId.jpg"
    private fun getLocationImageThumbnailPath(locationId: UInt, imageId: UInt) = "$locationImageThumbnailsDir/${locationId}_${imageId}_${THUMBNAIL_WIDTH}x${THUMBNAIL_HEIGHT}.jpg"

    private fun getArticleImagePath(articleId: UInt, imageId: UInt) = "$articleImagesDir/${articleId}_$imageId.jpg"
    private fun getArticleImageThumbnailPath(articleId: UInt, imageId: UInt) = "$articleImageThumbnailsDir/${articleId}_${imageId}_${THUMBNAIL_WIDTH}x${THUMBNAIL_HEIGHT}.jpg"


    suspend fun saveLocationImage(locationId: UInt, imageId: UInt, image: ByteArray) {
        fileHandler.writeBytes(getLocationImagePath(locationId, imageId), image)
    }

    suspend fun getLocationImage(locationId: UInt, imageId: UInt): ByteArray? {
        return fileHandler.readBytes(getLocationImagePath(locationId, imageId))
    }

    fun getLocationImageInputStream(locationId: UInt, imageId: UInt): FileInputSource? {
        return fileHandler.openInputStream(getLocationImagePath(locationId, imageId))
    }

    suspend fun getLocationImageThumbnail(locationId: UInt, imageId: UInt): ByteArray? {
        val thumbnailPath = getLocationImageThumbnailPath(locationId, imageId)
        val cachedThumbnail = fileHandler.readBytes(thumbnailPath)
        if (cachedThumbnail != null) {
            return cachedThumbnail
        }

        val imageBytes = getLocationImage(locationId, imageId)
        return imageBytes?.let {
            val resizedImage = resizeImage(it, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            resizedImage?.let {
                fileHandler.writeBytes(thumbnailPath, it)
            }
            resizedImage
        }
    }

    suspend fun deleteLocationImage(locationId: UInt, imageId: UInt) {
        fileHandler.deleteFile(getLocationImagePath(locationId, imageId))
        fileHandler.deleteFile(getLocationImageThumbnailPath(locationId, imageId))
    }

    suspend fun saveArticleImage(articleId: UInt, imageId: UInt, image: ByteArray) {
        fileHandler.writeBytes(getArticleImagePath(articleId, imageId), image)
    }

    suspend fun getArticleImage(articleId: UInt, imageId: UInt): ByteArray? {
        return fileHandler.readBytes(getArticleImagePath(articleId, imageId))
    }

    fun getArticleImageInputStream(articleId: UInt, imageId: UInt): FileInputSource? {
        return fileHandler.openInputStream(getArticleImagePath(articleId, imageId))
    }

    suspend fun getArticleImageThumbnail(articleId: UInt, imageId: UInt): ByteArray? {
        val thumbnailPath = getArticleImageThumbnailPath(articleId, imageId)
        val cachedThumbnail = fileHandler.readBytes(thumbnailPath)
        if (cachedThumbnail != null) {
            return cachedThumbnail
        }

        val imageBytes = getArticleImage(articleId, imageId)
        return imageBytes?.let {
            val resizedImage = resizeImage(it, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            resizedImage?.let {
                fileHandler.writeBytes(thumbnailPath, it)
            }
            resizedImage
        }
    }

    suspend fun deleteArticleImage(articleId: UInt, imageId: UInt) {
        fileHandler.deleteFile(getArticleImagePath(articleId, imageId))
        fileHandler.deleteFile(getArticleImageThumbnailPath(articleId, imageId))
    }
}
