package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.UIKit.UIViewController

@Composable
actual fun BackButtonHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS hat keinen physischen Back-Button
}

actual fun getContext(): Any? = null

actual fun sendEmail(address: String, subject: String) {
    val urlString = "mailto:$address?subject=${subject.encodeURL()}"
    val url = NSURL.URLWithString(urlString)
    if (url != null) {
        UIApplication.sharedApplication.openURL(url)
    }
}

private fun String.encodeURL(): String = this.replace(" ", "%20")

@Composable
actual fun rememberImagePickerLauncher(onImagesSelected: (List<ByteArray>) -> Unit): ImagePickerLauncher {
    return ImagePickerLauncher()
}

actual class ImagePickerLauncher {
    actual fun launch() { 
        // TODO: Implementiere iOS Image Picker
    }
}

actual suspend fun resizeImage(bytes: ByteArray, maxWidth: Int, maxHeight: Int): ByteArray = bytes

actual fun createEmptyImageByteArray(): ByteArray = ByteArray(0)

actual fun initializeLogger(fileHandler: FileHandler, settings: Settings) {
    println("iOS Logger initialized")
}

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val skiaImage = Image.makeFromEncoded(this)
    val skiaBitmap = Bitmap()
    skiaBitmap.allocN32Pixels(skiaImage.width, skiaImage.height)
    val canvas = Canvas(skiaBitmap)
    canvas.drawImage(skiaImage, 0f, 0f)
    return skiaBitmap.asComposeImageBitmap()
}

@Composable
actual fun FilePicker(show: Boolean, onFileSelected: (String?) -> Unit) {
    // TODO
}

@Composable
actual fun FilePickerForZip(show: Boolean, onFileSelected: (Any?) -> Unit) {
    // TODO
}
