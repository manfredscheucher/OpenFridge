package org.example.project

fun String.format(vararg args: Any?): String {
    var result = this
    args.forEach { arg ->
        if (result.contains("%02d")) {
            val formattedArg = arg.toString().toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: arg.toString()
            result = result.replaceFirst("%02d", formattedArg)
        } else if (result.contains("%04d")) {
            val formattedArg = arg.toString().toIntOrNull()?.let { it.toString().padStart(4, '0') } ?: arg.toString()
            result = result.replaceFirst("%04d", formattedArg)
        } else if (result.contains("%s")) {
            result = result.replaceFirst("%s", arg.toString())
        }
    }
    return result
}

fun commonFormatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        "${(mb * 10).toInt() / 10.0} MB"
    } else if (kb >= 1.0) {
        "${(kb * 10).toInt() / 10.0} KB"
    } else {
        "$size B"
    }
}
