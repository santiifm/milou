package com.santiifm.milou.util

object ConsoleFormatter {
    
    fun formatConsoleField(input: String): String {
        // Extract console name (everything after the first underscore)
        val consoleName = if (input.contains("_")) {
            input.substringAfter("_")
        } else {
            input
        }
        
        val formatted = consoleName
            .replace("_", " ")
            .let { StringUtils.capitalizeWords(it) }
        
        return splitLongConsoleName(formatted)
    }

    fun getConsoleFolderName(consoleId: String): String {
        val consoleName = if (consoleId.contains("_")) {
            consoleId.substringAfter("_")
        } else {
            consoleId
        }

        return consoleName.replace("_", " ").let { StringUtils.capitalizeWords(it) }
    }
    
    private fun splitLongConsoleName(name: String): String {
        val words = name.split(" ")
        
        // If 4 or more words, split into two lines
        if (words.size >= 4) {
            val midPoint = words.size / 2
            val firstLine = words.take(midPoint).joinToString(" ")
            val secondLine = words.drop(midPoint).joinToString(" ")
            return "$firstLine\n$secondLine"
        }
        
        // If 3 words and total length > 15, split after first word
        if (words.size == 3 && name.length > 15) {
            return "${words[0]}\n${words[1]} ${words[2]}"
        }
        
        return name
    }
    
    fun getConsoleDisplayName(consoleId: String): String {
        return formatConsoleField(consoleId)
    }
}
