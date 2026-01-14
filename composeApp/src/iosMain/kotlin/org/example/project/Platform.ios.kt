package org.example.project

import platform.UIKit.UIDevice

class IOSPlatform(override val fileHandler: FileHandler): Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(context: Any?): Platform = IOSPlatform(IosFileHandler())
