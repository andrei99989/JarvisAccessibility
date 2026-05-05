package com.example.jarvisaccessibility

class JarvisCommandRouter(
    private val installedAppsManager: InstalledAppsManager
) {
    data class RouteResult(
        val handled: Boolean,
        val reply: String
    )

    fun route(command: String): RouteResult {
        val original = command.trim()
        val text = normalize(original)

        if (text.isBlank()) {
            return RouteResult(true, "Nu am primit nicio comandă.")
        }

        routeOpenApp(text)?.let { appName ->
            return RouteResult(true, installedAppsManager.openApp(appName))
        }

        routeSearchApp(text)?.let { appName ->
            return RouteResult(true, installedAppsManager.searchAppsText(appName))
        }

        if (isListAppsCommand(text)) {
            return RouteResult(true, installedAppsManager.listAppsText())
        }

        return RouteResult(false, "")
    }

    private fun routeOpenApp(text: String): String? {
        val directAliases = mapOf(
            "youtube" to "YouTube",
            "yootube" to "YouTube",
            "you tube" to "YouTube",
            "iutub" to "YouTube",
            "yt" to "YouTube",

            "chrome" to "Chrome",
            "google chrome" to "Chrome",
            "browser" to "Chrome",
            "internet" to "Chrome",

            "termux" to "Termux",
            "whatsapp" to "WhatsApp",
            "whats app" to "WhatsApp",
            "facebook" to "Facebook",
            "face book" to "Facebook",
            "tiktok" to "TikTok",
            "tik tok" to "TikTok",
            "instagram" to "Instagram",
            "insta" to "Instagram"
        )

        for ((spoken, canonical) in directAliases) {
            if (
                text == "deschide $spoken" ||
                text == "porneste $spoken" ||
                text == "porneste aplicatia $spoken" ||
                text == "deschide aplicatia $spoken" ||
                text == "intra pe $spoken" ||
                text == "intra in $spoken"
            ) {
                return canonical
            }
        }

        val prefixes = listOf(
            "deschide aplicatia ",
            "deschide aplicația ",
            "deschide ",
            "porneste aplicatia ",
            "porneste ",
            "intra pe ",
            "intra in "
        )

        for (prefix in prefixes) {
            if (text.startsWith(prefix)) {
                return commandAppName(text.removePrefix(prefix))
            }
        }

        return null
    }

    private fun routeSearchApp(text: String): String? {
        val prefixes = listOf(
            "cauta aplicatia instalata ",
            "cauta aplicatia ",
            "cauta aplicația instalata ",
            "cauta aplicația "
        )

        for (prefix in prefixes) {
            if (text.startsWith(prefix)) {
                return commandAppName(text.removePrefix(prefix))
            }
        }

        return null
    }

    private fun isListAppsCommand(text: String): Boolean {
        return text == "listeaza aplicatiile instalate" ||
            text == "listeaza aplicatii instalate" ||
            text == "lista aplicatii" ||
            text == "lista de aplicatii" ||
            text == "deschide lista de aplicatii" ||
            text == "deschide lista de placati" ||
            text.contains("aplicatii instalate")
    }

    private fun commandAppName(value: String): String {
        val cleaned = value.trim()

        return when (cleaned) {
            "youtube", "yootube", "you tube", "iutub", "yt" -> "YouTube"
            "chrome", "google chrome", "browser", "internet" -> "Chrome"
            "whatsapp", "whats app" -> "WhatsApp"
            "facebook", "face book" -> "Facebook"
            "tiktok", "tik tok" -> "TikTok"
            "instagram", "insta" -> "Instagram"
            else -> cleaned
        }
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ş", "s")
            .replace("ț", "t")
            .replace("ţ", "t")
    }
}
