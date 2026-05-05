package com.example.jarvisaccessibility

import android.content.Intent
import android.net.Uri

class JarvisController(
    private val service: JarvisAccessibilityService
) {

    private val customCommandManager = CustomCommandManager(service)
    private val appSafetyManager = AppSafetyManager(service)
    private val customBlockedAppsManager = CustomBlockedAppsManager(service)
    private val customAllowedAppsManager = CustomAllowedAppsManager(service)
    private val installedAppsManager = InstalledAppsManager(service)
    private val commandRouter = JarvisCommandRouter(installedAppsManager)

    fun executeCommand(rawCommand: String): String {
        val original = rawCommand.trim()


        val routed = commandRouter.route(original)
        if (routed.handled) {
            return routed.reply
        }




        val normalizedForDirectAppOpen = original
            .lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ş", "s")
            .replace("ț", "t")
            .replace("ţ", "t")
            .trim()

        if (
            normalizedForDirectAppOpen == "deschide youtube" ||
            normalizedForDirectAppOpen == "deschide yootube" ||
            normalizedForDirectAppOpen == "deschide you tube" ||
            normalizedForDirectAppOpen == "intra pe youtube" ||
            normalizedForDirectAppOpen == "intra pe yootube" ||
            normalizedForDirectAppOpen == "intră pe youtube" ||
            normalizedForDirectAppOpen == "intră pe yootube" ||
            normalizedForDirectAppOpen == "porneste youtube" ||
            normalizedForDirectAppOpen == "pornește youtube"
        ) {
            return installedAppsManager.openApp("YouTube")
        }

        if (
            normalizedForDirectAppOpen == "deschide chrome" ||
            normalizedForDirectAppOpen == "deschide google chrome" ||
            normalizedForDirectAppOpen == "intra pe chrome" ||
            normalizedForDirectAppOpen == "intră pe chrome"
        ) {
            return installedAppsManager.openApp("Chrome")
        }



        val normalizedCommandForApps = original
            .lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ş", "s")
            .replace("ț", "t")
            .replace("ţ", "t")

        if (
            normalizedCommandForApps == "listeaza aplicatiile instalate" ||
            normalizedCommandForApps == "listeaza aplicatii instalate" ||
            normalizedCommandForApps == "lista aplicatii" ||
            normalizedCommandForApps == "lista de aplicatii" ||
            normalizedCommandForApps == "listă de aplicații" ||
            normalizedCommandForApps == "deschide lista de aplicatii" ||
            normalizedCommandForApps == "deschide lista de aplicații" ||
            normalizedCommandForApps == "deschide lista de placati" ||
            normalizedCommandForApps == "deschide lista de placați" ||
            normalizedCommandForApps.contains("lista de aplicatii") ||
            normalizedCommandForApps.contains("lista aplicatii") ||
            normalizedCommandForApps.contains("aplicatii instalate")
        ) {
            return installedAppsManager.listAppsText()
        }

        if (
            normalizedCommandForApps.startsWith("cauta aplicatia ") ||
            normalizedCommandForApps.startsWith("cauta aplicatia instalata ")
        ) {
            val appName = original
                .replaceFirst("caută aplicația instalată", "", ignoreCase = true)
                .replaceFirst("cauta aplicatia instalata", "", ignoreCase = true)
                .replaceFirst("caută aplicația", "", ignoreCase = true)
                .replaceFirst("cauta aplicatia", "", ignoreCase = true)
                .trim()

            return installedAppsManager.searchAppsText(appName)
        }


        if (
            (normalizedCommandForApps.startsWith("deschide aplicatia ") ||
                normalizedCommandForApps.startsWith("deschide aplicația ") ||
                normalizedCommandForApps.startsWith("deschide ") ||
                normalizedCommandForApps.startsWith("intra pe ") ||
                normalizedCommandForApps.startsWith("intră pe ")) &&
            (normalizedCommandForApps.contains(" si cauta ") ||
                normalizedCommandForApps.contains(" și caută ") ||
                normalizedCommandForApps.contains(" si caută ") ||
                normalizedCommandForApps.contains(" și cauta "))
        ) {
            val parts = original.split(
                " și caută",
                " si cauta",
                " si caută",
                " și cauta",
                ignoreCase = true,
                limit = 2
            )

            if (parts.size >= 2) {
                val openPart = parts[0].trim()
                val searchQuery = parts[1].trim()

                val appName = openPart
                    .replaceFirst("deschide aplicația", "", ignoreCase = true)
                    .replaceFirst("deschide aplicatia", "", ignoreCase = true)
                    .replaceFirst("deschide", "", ignoreCase = true)
                    .replaceFirst("intră pe", "", ignoreCase = true)
                    .replaceFirst("intra pe", "", ignoreCase = true)
                    .trim()

                val openResult = installedAppsManager.openApp(appName)

                if (openResult.startsWith("Nu am găsit aplicația instalată")) {
                    return openResult
                }

                Thread.sleep(1500)

                val searchResult = executeCommand("caută $appName $searchQuery")

                return "$openResult\nCaut în $appName: $searchQuery\n$searchResult"
            }
        }

        if (
            normalizedCommandForApps.startsWith("deschide aplicatia ") ||
            normalizedCommandForApps.startsWith("deschide aplicația ")
        ) {
            val appName = original
                .replaceFirst("deschide aplicația", "", ignoreCase = true)
                .replaceFirst("deschide aplicatia", "", ignoreCase = true)
                .trim()

            return installedAppsManager.openApp(appName)
        }

        if (
            normalizedCommandForApps.startsWith("deschide ") ||
            normalizedCommandForApps.startsWith("intra pe ") ||
            normalizedCommandForApps.startsWith("intră pe ") ||
            normalizedCommandForApps.startsWith("intra in ") ||
            normalizedCommandForApps.startsWith("intră în ") ||
            normalizedCommandForApps.startsWith("porneste ") ||
            normalizedCommandForApps.startsWith("pornește ") ||
            normalizedCommandForApps.startsWith("lanseaza ") ||
            normalizedCommandForApps.startsWith("lansează ")
        ) {
            val appName = original
                .replaceFirst("deschide", "", ignoreCase = true)
                .replaceFirst("intră pe", "", ignoreCase = true)
                .replaceFirst("intra pe", "", ignoreCase = true)
                .replaceFirst("intră în", "", ignoreCase = true)
                .replaceFirst("intra in", "", ignoreCase = true)
                .replaceFirst("pornește", "", ignoreCase = true)
                .replaceFirst("porneste", "", ignoreCase = true)
                .replaceFirst("lansează", "", ignoreCase = true)
                .replaceFirst("lanseaza", "", ignoreCase = true)
                .trim()

            val openResult = installedAppsManager.openApp(appName)

            if (!openResult.startsWith("Nu am găsit aplicația instalată")) {
                return openResult
            }
        }


        val command = normalize(original)

        if (command.isBlank()) {
            return "Comandă goală."
        }

        return when {


            command.startsWith("permite aplicatia ") ||
                command.startsWith("permite aplicația ") ||
                command.startsWith("permite aplicaţia ") -> {
                val appName = original
                    .replaceFirst("permite aplicația", "", ignoreCase = true)
                    .replaceFirst("permite aplicatia", "", ignoreCase = true)
                    .replaceFirst("permite aplicaţia", "", ignoreCase = true)
                    .trim()

                if (appSafetyManager.isBlockedAppName(appName)) {
                    "Nu pot permite această aplicație deoarece este blocată pentru siguranță: $appName"
                } else {
                    customAllowedAppsManager.allowApp(appName)
                }
            }

            command.startsWith("sterge aplicatia permisa ") ||
                command.startsWith("șterge aplicația permisă ") ||
                command.startsWith("sterge aplicația permisă ") ||
                command.startsWith("șterge aplicatia permisa ") -> {
                val appName = original
                    .replaceFirst("șterge aplicația permisă", "", ignoreCase = true)
                    .replaceFirst("sterge aplicatia permisa", "", ignoreCase = true)
                    .replaceFirst("sterge aplicația permisă", "", ignoreCase = true)
                    .replaceFirst("șterge aplicatia permisa", "", ignoreCase = true)
                    .trim()

                customAllowedAppsManager.removeAllowedApp(appName)
            }

            command == "listeaza aplicatii permise custom" ||
                command == "listează aplicații permise custom" -> {
                customAllowedAppsManager.listCustomAllowedApps()
            }

            command.startsWith("este permisa ") ||
                command.startsWith("este permisă ") -> {
                val appName = original
                    .replaceFirst("este permisă", "", ignoreCase = true)
                    .replaceFirst("este permisa", "", ignoreCase = true)
                    .trim()

                if (appName.isBlank()) {
                    "Format corect: este permisă TikTok"
                } else if (appSafetyManager.isBlockedAppName(appName)) {
                    "Nu, aplicația este blocată pentru siguranță: $appName"
                } else if (customAllowedAppsManager.isCustomAllowed(appName)) {
                    "Da, aplicația este permisă custom: $appName"
                } else {
                    "Aplicația nu este în lista permisă custom: $appName"
                }
            }

            command.startsWith("blocheaza aplicatia ") ||
                command.startsWith("blochează aplicația ") ||
                command.startsWith("blocheaza aplicația ") ||
                command.startsWith("blochează aplicatia ") -> {
                val appName = original
                    .replaceFirst("blochează aplicația", "", ignoreCase = true)
                    .replaceFirst("blocheaza aplicatia", "", ignoreCase = true)
                    .replaceFirst("blochează aplicatia", "", ignoreCase = true)
                    .replaceFirst("blocheaza aplicația", "", ignoreCase = true)
                    .trim()

                customBlockedAppsManager.blockApp(appName)
            }

            command.startsWith("deblocheaza aplicatia ") ||
                command.startsWith("deblochează aplicația ") ||
                command.startsWith("deblocheaza aplicația ") ||
                command.startsWith("deblochează aplicatia ") -> {
                val appName = original
                    .replaceFirst("deblochează aplicația", "", ignoreCase = true)
                    .replaceFirst("deblocheaza aplicatia", "", ignoreCase = true)
                    .replaceFirst("deblochează aplicatia", "", ignoreCase = true)
                    .replaceFirst("deblocheaza aplicația", "", ignoreCase = true)
                    .trim()

                customBlockedAppsManager.unblockApp(appName)
            }

            command == "listeaza aplicatii blocate custom" ||
                command == "listează aplicații blocate custom" -> {
                customBlockedAppsManager.listCustomBlockedApps()
            }

            command == "aplicatia curenta" ||
                command == "aplicația curentă" ||
                command == "package curent" ||
                command == "pachet curent" -> {
                val pkg = service.currentPackageName ?: "necunoscut"
                val blocked = appSafetyManager.isBlockedPackage(pkg)

                """
                Aplicația curentă:
                $pkg

                Blocată:
                ${if (blocked) "DA" else "NU"}
                """.trimIndent()
            }

            command.startsWith("este blocat pachetul ") -> {
                val packageName = original
                    .replaceFirst("este blocat pachetul", "", ignoreCase = true)
                    .trim()

                if (packageName.isBlank()) {
                    "Format corect: este blocat pachetul com.exemplu.app"
                } else {
                    appSafetyManager.checkBlockedPackage(packageName)
                }
            }

            command == "listeaza aplicatii instalate" ||
                command == "listează aplicații instalate" ||
                command == "lista aplicatii" ||
                command == "lista aplicații" -> {
                listInstalledApps()
            }

            command.startsWith("cauta aplicatia ") ||
                command.startsWith("caută aplicația ") ||
                command.startsWith("cauta aplicația ") ||
                command.startsWith("caută aplicatia ") -> {
                val query = original
                    .replaceFirst("caută aplicația", "", ignoreCase = true)
                    .replaceFirst("cauta aplicatia", "", ignoreCase = true)
                    .replaceFirst("caută aplicatia", "", ignoreCase = true)
                    .replaceFirst("cauta aplicația", "", ignoreCase = true)
                    .trim()

                searchApp(query)
            }

            command == "listeaza aplicatii blocate" ||
                command == "listează aplicații blocate" ||
                command == "listeaza aplicații blocate" ||
                command == "listează aplicatii blocate" -> {
                appSafetyManager.listBlockedApps()
            }

            command.startsWith("este blocata ") || command.startsWith("este blocată ") -> {
                val appName = original
                    .replaceFirst("este blocată", "", ignoreCase = true)
                    .replaceFirst("este blocata", "", ignoreCase = true)
                    .trim()

                if (appName.isBlank()) {
                    "Format corect: este blocată Mobile Banking"
                } else {
                    appSafetyManager.checkBlockedApp(appName)
                }
            }

            command.startsWith("salveaza comanda ") || command.startsWith("salvează comanda ") -> {
                saveCustomCommand(original)
            }

            command.startsWith("ruleaza ") || command.startsWith("rulează ") -> {
                runCustomCommand(original)
            }

            command.startsWith("sterge comanda ") || command.startsWith("șterge comanda ") -> {
                deleteCustomCommand(original)
            }

            command == "listeaza comenzi" || command == "listează comenzi" -> {
                customCommandManager.listCommands()
            }

            command.startsWith("deschide ") -> {
                val appName = original.substringAfter("deschide", "").trim()

                if (appName.isBlank()) {
                    "Spune ce aplicație să deschid."
                } else if (appSafetyManager.isBlockedAppName(appName)) {
                    appSafetyManager.blockedMessage(appName)
                } else {
                    val ok = service.openAppByName(appName)
                    if (ok) "Am deschis: $appName" else "Nu am găsit aplicația permisă: $appName"
                }
            }

            command.startsWith("cauta ") || command.startsWith("căută ") -> {
                val query = original
                    .replaceFirst("caută", "", ignoreCase = true)
                    .replaceFirst("cauta", "", ignoreCase = true)
                    .trim()

                if (query.isBlank()) {
                    "Spune ce vrei să caut."
                } else {
                    openGoogleSearch(query)
                    "Caut pe Google: $query"
                }
            }

            command.startsWith("apasa pe ") || command.startsWith("apasă pe ") -> {
                if (isCurrentAppBlocked()) return blockedCurrentApp()

                val text = original
                    .replaceFirst("apasă pe", "", ignoreCase = true)
                    .replaceFirst("apasa pe", "", ignoreCase = true)
                    .trim()

                if (text.isBlank()) {
                    "Spune pe ce text să apăs."
                } else {
                    val ok = service.clickNodeByText(text)
                    if (ok) "Am apăsat pe: $text" else "Nu am găsit pe ecran: $text"
                }
            }

            command.startsWith("scrie ") -> {
                if (isCurrentAppBlocked()) return blockedCurrentApp()

                val text = original.substringAfter("scrie", "").trim()

                if (text.isBlank()) {
                    "Spune ce text să scriu."
                } else {
                    val ok = service.writeText(text)
                    if (ok) "Am scris: $text" else "Nu am găsit un câmp text activ."
                }
            }

            command == "scroll jos" || command == "deruleaza jos" || command == "derulează jos" -> {
                if (isCurrentAppBlocked()) return blockedCurrentApp()

                val ok = service.scrollDown()
                if (ok) "Am făcut scroll jos." else "Nu am putut face scroll jos."
            }

            command == "scroll sus" || command == "deruleaza sus" || command == "derulează sus" -> {
                if (isCurrentAppBlocked()) return blockedCurrentApp()

                val ok = service.scrollUp()
                if (ok) "Am făcut scroll sus." else "Nu am putut face scroll sus."
            }

            command == "citeste ecranul" || command == "citește ecranul" -> {
                if (isCurrentAppBlocked()) return blockedCurrentApp()

                val text = service.readScreenText()
                if (text.isBlank()) "Nu am găsit text pe ecran." else text
            }

            command == "inapoi" || command == "înapoi" || command == "back" -> {
                val ok = service.pressBack()
                if (ok) "Am apăsat Înapoi." else "Nu am putut apăsa Înapoi."
            }

            command == "home" || command == "acasă" || command == "acasa" -> {
                val ok = service.pressHome()
                if (ok) "Am mers la Home." else "Nu am putut merge la Home."
            }

            command == "recente" || command == "aplicatii recente" || command == "aplicații recente" -> {
                val ok = service.openRecents()
                if (ok) "Am deschis aplicațiile recente." else "Nu am putut deschide recente."
            }

            command.startsWith("tap ") -> {
                if (isCurrentAppBlocked()) return blockedCurrentApp()

                val parts = command.split(" ")
                if (parts.size < 3) {
                    "Format corect: tap 500 800"
                } else {
                    val x = parts[1].toIntOrNull()
                    val y = parts[2].toIntOrNull()

                    if (x == null || y == null) {
                        "Coordonate invalide. Exemplu: tap 500 800"
                    } else {
                        val ok = service.tap(x, y)
                        if (ok) "Am făcut tap la $x, $y." else "Nu am putut face tap."
                    }
                }
            }

            command.startsWith("swipe ") -> {
                if (isCurrentAppBlocked()) return blockedCurrentApp()

                val parts = command.split(" ")
                if (parts.size < 5) {
                    "Format corect: swipe 500 1200 500 300"
                } else {
                    val startX = parts[1].toIntOrNull()
                    val startY = parts[2].toIntOrNull()
                    val endX = parts[3].toIntOrNull()
                    val endY = parts[4].toIntOrNull()

                    if (startX == null || startY == null || endX == null || endY == null) {
                        "Coordonate invalide. Exemplu: swipe 500 1200 500 300"
                    } else {
                        val ok = service.swipe(startX, startY, endX, endY)
                        if (ok) "Am făcut swipe." else "Nu am putut face swipe."
                    }
                }
            }

            else -> {
                "Comandă necunoscută: $original"
            }
        }
    }

    private fun listInstalledApps(): String {
        val apps = service.getInstalledLaunchableApps()

        if (apps.isEmpty()) {
            return "Nu am găsit aplicații instalate."
        }

        val builder = StringBuilder()
        builder.append("Aplicații instalate:\n")

        apps.take(120).forEach { app ->
            builder.append(if (app.isBlocked) "🔒 " else "✅ ")
                .append(app.label)
                .append("\n")
        }

        if (apps.size > 120) {
            builder.append("\nȘi încă ")
                .append(apps.size - 120)
                .append(" aplicații.")
        }

        return builder.toString().trim()
    }

    private fun searchApp(query: String): String {
        if (query.isBlank()) {
            return "Format corect: caută aplicația Spotify"
        }

        val apps = service.searchInstalledApps(query)

        if (apps.isEmpty()) {
            return "Nu am găsit aplicații pentru: $query"
        }

        val builder = StringBuilder()
        builder.append("Rezultate aplicații pentru: ")
            .append(query)
            .append("\n")

        apps.take(30).forEach { app ->
            builder.append(if (app.isBlocked) "🔒 " else "✅ ")
                .append(app.label)
                .append(" — ")
                .append(app.packageName)
                .append("\n")
        }

        return builder.toString().trim()
    }

    private fun isCurrentAppBlocked(): Boolean {
        return appSafetyManager.isBlockedPackage(service.currentPackageName)
    }

    private fun blockedCurrentApp(): String {
        return appSafetyManager.blockedCurrentAppMessage(service.currentPackageName)
    }

    private fun saveCustomCommand(original: String): String {
        val cleaned = original
            .replaceFirst("salvează comanda", "", ignoreCase = true)
            .replaceFirst("salveaza comanda", "", ignoreCase = true)
            .trim()

        val parts = cleaned.split("=", limit = 2)

        if (parts.size != 2) {
            return "Format corect: salvează comanda chrome = deschide Chrome"
        }

        val name = parts[0].trim()
        val command = parts[1].trim()

        val normalizedCommand = normalize(command)
        if (normalizedCommand.startsWith("deschide ")) {
            val appName = command.substringAfter("deschide", "").trim()
            if (appSafetyManager.isBlockedAppName(appName)) {
                return appSafetyManager.blockedMessage(appName)
            }
        }

        return customCommandManager.saveCommand(name, command)
    }

    private fun runCustomCommand(original: String): String {
        val name = original
            .replaceFirst("rulează", "", ignoreCase = true)
            .replaceFirst("ruleaza", "", ignoreCase = true)
            .trim()

        if (name.isBlank()) {
            return "Format corect: rulează chrome"
        }

        val savedCommand = customCommandManager.getCommand(name)
            ?: return "Nu am găsit comanda rapidă: $name"

        val normalizedSaved = normalize(savedCommand)
        if (normalizedSaved.startsWith("deschide ")) {
            val appName = savedCommand.substringAfter("deschide", "").trim()
            if (appSafetyManager.isBlockedAppName(appName)) {
                return appSafetyManager.blockedMessage(appName)
            }
        }

        val result = executeCommand(savedCommand)

        return """
            Rulez comanda rapidă: $name
            Comandă: $savedCommand

            Rezultat:
            $result
        """.trimIndent()
    }

    private fun deleteCustomCommand(original: String): String {
        val name = original
            .replaceFirst("șterge comanda", "", ignoreCase = true)
            .replaceFirst("sterge comanda", "", ignoreCase = true)
            .trim()

        if (name.isBlank()) {
            return "Format corect: șterge comanda chrome"
        }

        return customCommandManager.deleteCommand(name)
    }

    private fun openGoogleSearch(query: String) {
        val encoded = Uri.encode(query)
        val url = "https://www.google.com/search?q=$encoded"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        service.startActivity(intent)
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
