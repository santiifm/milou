package com.santiifm.milou.util

object Constants {
    const val ANIMATION_DURATION_MS = 2000
    const val FLOAT_ANIMATION_OFFSET = 4f

    const val DEFAULT_PADDING_DP = 16
    const val BUTTON_SPACING_DP = 8
    const val SECTION_SPACING_DP = 12

    const val BUTTON_WIDTH_SMALL = 50
    const val BUTTON_WIDTH_MEDIUM = 80

    const val DEFAULT_CONCURRENT_DOWNLOADS = 3

    const val CONNECTION_TIMEOUT_MS = 30000L
    const val READ_TIMEOUT_MS = 60000L
    const val BUFFER_SIZE = 8192

    const val SETTINGS_DATASTORE_NAME = "user_settings"

    const val PROGRESS_COMPLETE = 1f


    const val EXTRACTION_BUFFER_SIZE = 524288 // 512 KB — reduces SAF IPC call count ~64x vs 8 KB
    const val MAX_ARCHIVE_ENTRIES = 10000

    const val KILOBYTE = 1000L
    const val KIBIBYTE = 1024L
    const val MEGABYTE = 1000L * 1000L
    const val MEBIBYTE = 1024L * 1024L
    const val GIGABYTE = 1000L * 1000L * 1000L
    const val GIBIBYTE = 1024L * 1024L * 1024L
    const val TERABYTE = 1000L * 1000L * 1000L * 1000L
    const val TEBIBYTE = 1024L * 1024L * 1024L * 1024L

    const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    const val SPEED_CHECK_INTERVAL_MS = 100L

    // Tag Constants
    object Tags {

        // Video Standards
        val VIDEO_STANDARDS = setOf("PAL", "NTSC", "NTSC-J", "NTSC-U", "NTSC-C")

        // Content Types
        val CONTENT_TYPES = setOf(
            "DLC", "UPDATE", "TITLE UPDATE", "PATCH", "DEMO", "BETA", "PROTO",
            "PIRATE", "KIOSK", "GAME", "MISCELLANEOUS", "DEBUG", "RETROACHIEVEMENTS", "HACK"
        )

        // Regions
        val REGIONS = setOf(
            "USA", "EUROPE", "JAPAN", "KOREA", "CHINA", "AUSTRALIA", "CANADA", "MEXICO",
            "BRAZIL", "GERMANY", "FRANCE", "SPAIN", "ITALY", "RUSSIA", "UK", "UNITED KINGDOM",
            "BRITAIN", "WORLD", "POLAND", "FINLAND", "PORTUGAL", "SCANDINAVIA", "BELGIUM",
            "NETHERLANDS", "SWITZERLAND", "DENMARK", "GREECE"
        )

        // Continents
        val CONTINENTS = setOf(
            "NORTH AMERICA", "SOUTH AMERICA", "EUROPE", "ASIA", "AFRICA", "OCEANIA"
        )

        // Special tags that should be matched partially (contains)
        val PARTIAL_MATCHERS = listOf(
            "BETA", "PROTO", "REV", "VERSION", "VER", "LODGENET",
            "GAMECUBE", "LIMITED", "LAYER", "PAK", "PACK", "BEST", "BOX", "DISC"
        )

        // Regex patterns
        val LANGUAGE_CODE_PATTERN = Regex("^[A-Z]{2,3}$")
        val VERSION_PATTERN = Regex("^V\\d+(\\.\\d+)?$")
        val FILE_EXTENSION_PATTERN = Regex("^\\.[A-Z0-9]+$")

        // All regions (regions + continents)
        val ALL_REGIONS = REGIONS + CONTINENTS
    }
}
