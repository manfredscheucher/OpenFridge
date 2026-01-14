package org.example.project

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.URLByAppendingPathComponent
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToURL
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class FileDownloader actual constructor() {
    actual fun download(fileName: String, data: String, context: Any?) {
        val tempDir = NSTemporaryDirectory()
        val fileURL = NSURL.fileURLWithPath(tempDir).URLByAppendingPathComponent(fileName)!!
        (data as platform.Foundation.NSString).writeToURL(fileURL, true, NSUTF8StringEncoding, null)
        shareFile(fileURL)
    }

    actual fun download(fileName: String, data: ByteArray, context: Any?) {
        val tempDir = NSTemporaryDirectory()
        val fileURL = NSURL.fileURLWithPath(tempDir).URLByAppendingPathComponent(fileName)!!
        val nsData = data.toNSData()
        nsData.writeToURL(fileURL, true)
        shareFile(fileURL)
    }

    private fun shareFile(fileURL: NSURL) {
        val activityViewController = UIActivityViewController(listOf(fileURL), null)
        val window = UIApplication.sharedApplication.keyWindow
        window?.rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
