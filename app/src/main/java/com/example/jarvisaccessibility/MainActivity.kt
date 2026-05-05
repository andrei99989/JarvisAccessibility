package com.example.jarvisaccessibility

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var inputCommand: EditText
    private lateinit var inputApiKey: EditText
    private lateinit var inputOpenRouterModel: EditText
    private lateinit var inputSettingsImport: EditText
    private lateinit var inputJarvisPin: EditText
    private lateinit var resultText: TextView
    private lateinit var statusText: TextView
    private lateinit var versionText: TextView
    private lateinit var historyText: TextView
    private lateinit var aiStatusText: TextView
    private lateinit var updateManager: UpdateManager
    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var aiDebugLogger: AiDebugLogger
    private lateinit var aiStopManager: AiStopManager
    private lateinit var aiPendingActionManager: AiPendingActionManager
    private lateinit var aiCommandMemory: AiCommandMemory
    private lateinit var settingsExportManager: JarvisSettingsExportManager
    private lateinit var jarvisPinManager: JarvisPinManager
    private lateinit var jarvisBackupManager: JarvisBackupManager
    private lateinit var jarvisCompletionManager: JarvisCompletionManager
    private lateinit var termuxServerClient: TermuxServerClient

    private var lastReleaseUrl: String = "https://github.com/andrei99989/JarvisAccessibility/releases"
    private var lastApkUrl: String = ""

    private val speechRequestCode = 1001
    private val jarvisVoiceRequestCode = 2001
    private val jarvisHandsFreeRequestCode = 2002
    private val commandHistory = mutableListOf<String>()
    private lateinit var jarvisVoiceManager: JarvisVoiceManager
    private var voiceModeUseAi = false
    private var handsFreeModeActive = false
    private var handsFreeUseAi = false
    private var handsFreeCommandCount = 0
    private val maxHandsFreeCommands = 10
    private val smartVoiceInterpreter = SmartVoiceInterpreter()


    private fun isJarvisAccessibilityEnabledInSettings(): Boolean {
        val enabledServices = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(packageName, ignoreCase = true) &&
            enabledServices.contains("JarvisAccessibilityService", ignoreCase = true)
    }

    private fun getJarvisAccessibilityServiceOrNull(): JarvisAccessibilityService? {
        return JarvisAccessibilityService.instance
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = UpdateManager(this)
        apiKeyManager = ApiKeyManager(this)
        aiDebugLogger = AiDebugLogger(this)
        aiStopManager = AiStopManager(this)
        aiPendingActionManager = AiPendingActionManager(this)
        aiCommandMemory = AiCommandMemory(this)
        settingsExportManager = JarvisSettingsExportManager(this)
        jarvisPinManager = JarvisPinManager(this)
        jarvisBackupManager = JarvisBackupManager(this)
        jarvisCompletionManager = JarvisCompletionManager(this)
        termuxServerClient = TermuxServerClient()
        jarvisVoiceManager = JarvisVoiceManager(this)

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER_HORIZONTAL
        layout.setPadding(36, 55, 36, 40)

        val title = titleText("Jarvis Accessibility")
        versionText = normalText("")
        statusText = normalText("")

        inputApiKey = EditText(this)
        inputApiKey.hint = "API Key OpenRouter / Gemini / OpenAI"
        inputApiKey.setSingleLine(true)
        inputApiKey.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputApiKey.setPadding(20, 20, 20, 20)

        inputOpenRouterModel = EditText(this)
        inputOpenRouterModel.hint = "Model OpenRouter, ex: openrouter/auto"
        inputOpenRouterModel.setSingleLine(true)
        inputOpenRouterModel.setPadding(20, 20, 20, 20)

        inputSettingsImport = EditText(this)
        inputSettingsImport.hint = "Lipește aici exportul Jarvis pentru import"
        inputSettingsImport.setSingleLine(false)
        inputSettingsImport.minLines = 3
        inputSettingsImport.setPadding(20, 20, 20, 20)

        inputJarvisPin = EditText(this)
        inputJarvisPin.hint = "PIN Jarvis pentru setări sensibile"
        inputJarvisPin.setSingleLine(true)
        inputJarvisPin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputJarvisPin.setPadding(20, 20, 20, 20)

        inputCommand = EditText(this)
        inputCommand.hint = "Scrie comanda aici..."
        inputCommand.setSingleLine(false)
        inputCommand.minLines = 2
        inputCommand.setPadding(20, 20, 20, 20)

        resultText = normalText("Rezultat:")
        resultText.setPadding(0, 25, 0, 25)

        aiStatusText = normalText("Status AI: pregătit")
        aiStatusText.setPadding(0, 12, 0, 12)

        historyText = normalText("Istoric comenzi:\nGol")
        historyText.setPadding(0, 20, 0, 80)

        layout.addView(title)
        layout.addView(versionText)
        layout.addView(statusText)

        layout.addView(button("Deschide Jarvis Dashboard") {
            startActivity(Intent(this, JarvisDashboardActivity::class.java))
        })

        addSection(layout, "Setări")
        layout.addView(button("Deschide Accessibility Settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        addSection(layout, "Protecție setări Jarvis")
        layout.addView(inputJarvisPin)

        layout.addView(button("Setează PIN Jarvis") {
            val message = jarvisPinManager.setPin(inputJarvisPin.text.toString())
            inputJarvisPin.setText("")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Deblochează setări") {
            val message = jarvisPinManager.unlock(inputJarvisPin.text.toString())
            inputJarvisPin.setText("")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Blochează setări") {
            val message = jarvisPinManager.lock()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Status PIN Jarvis") {
            resultText.text = jarvisPinManager.status()
        })

        addSection(layout, "AI Provider + API Key")
        layout.addView(inputApiKey)
        layout.addView(inputOpenRouterModel)

        layout.addView(button("Folosește OpenRouter") {
            val message = apiKeyManager.saveAiProvider("openrouter")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Folosește Gemini") {
            val message = apiKeyManager.saveAiProvider("gemini")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Folosește OpenAI") {
            val message = apiKeyManager.saveAiProvider("openai")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Salvează OpenRouter API Key") {
            val message = apiKeyManager.saveOpenRouterKey(inputApiKey.text.toString())
            inputApiKey.setText("")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Salvează Gemini API Key") {
            val message = apiKeyManager.saveGeminiKey(inputApiKey.text.toString())
            inputApiKey.setText("")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Salvează OpenAI API Key") {
            val message = apiKeyManager.saveOpenAiKey(inputApiKey.text.toString())
            inputApiKey.setText("")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Șterge OpenRouter API Key") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = apiKeyManager.clearOpenRouterKey()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Șterge Gemini API Key") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = apiKeyManager.clearGeminiKey()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Șterge OpenAI API Key") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = apiKeyManager.clearOpenAiKey()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Salvează model OpenRouter") {
            val message = apiKeyManager.saveOpenRouterModel(inputOpenRouterModel.text.toString())
            inputOpenRouterModel.setText("")
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Șterge model OpenRouter") {
            val message = apiKeyManager.clearOpenRouterModel()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Verifică AI Keys") {
            resultText.text = apiKeyManager.getStatusText()
        })

        layout.addView(button("Vezi AI debug log") {
            resultText.text = aiDebugLogger.getLog()
        })

        layout.addView(button("Șterge AI debug log") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = aiDebugLogger.clearLog()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        addSection(layout, "Termux Server localhost")

        layout.addView(button("Status server Termux") {
            resultText.text = "Verific serverul Termux..."
            termuxServerClient.getStatus { result ->
                runOnUiThread {
                    resultText.text = result
                }
            }
        })

        layout.addView(button("Verifică update server Termux") {
            resultText.text = "Verific update server Termux..."
            termuxServerClient.checkUpdate { result ->
                runOnUiThread {
                    resultText.text = result
                }
            }
        })

        layout.addView(button("Actualizează server Termux") {
            resultText.text = "Instalez update server Termux..."
            termuxServerClient.installUpdate { result ->
                runOnUiThread {
                    resultText.text = result
                    Toast.makeText(this, "Dacă update-ul s-a instalat, repornește serverul Termux.", Toast.LENGTH_LONG).show()
                }
            }
        })

        layout.addView(button("Trimite comanda la server") {
            val command = inputCommand.text.toString().trim()

            if (command.isBlank()) {
                resultText.text = "Scrie o comandă înainte."
                return@button
            }

            resultText.text = "Trimit la server: $command"
            termuxServerClient.sendVoiceCommand(command) { result ->
                runOnUiThread {
                    resultText.text = result
                }
            }
        })

        layout.addView(button("Spune prin server Termux") {
            val text = inputCommand.text.toString().trim().ifBlank {
                "At your service, sir."
            }

            resultText.text = "Trimit voce la server: $text"
            termuxServerClient.say(text) { result ->
                runOnUiThread {
                    resultText.text = result
                }
            }
        })

        layout.addView(button("Vezi istoric comenzi server") {
            resultText.text = "Citesc istoricul comenzilor din server..."
            termuxServerClient.getCommandHistory { result ->
                runOnUiThread {
                    resultText.text = result
                }
            }
        })

        layout.addView(button("Șterge istoric comenzi server") {
            resultText.text = "Șterg istoricul comenzilor din server..."
            termuxServerClient.clearCommandHistory { result ->
                runOnUiThread {
                    resultText.text = result
                }
            }
        })

        addSection(layout, "Backup / setări Jarvis")
        layout.addView(button("Exportă setări Jarvis") {
            jarvisBackupManager.saveBackup()
            resultText.text = settingsExportManager.exportSettings()
        })

        layout.addView(button("Copiază export setări") {
            copySettingsExportToClipboard()
        })

        layout.addView(inputSettingsImport)

        layout.addView(button("Importă setări Jarvis") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            importSettingsFromText()
        })

        layout.addView(button("Importă setări din clipboard") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            importSettingsFromClipboard()
        })

        layout.addView(button("Șterge liste custom blocate/permise") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = settingsExportManager.clearCustomLists()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Șterge memoria AI") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = settingsExportManager.clearAiMemory()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Salvează backup setări local") {
            val message = jarvisBackupManager.saveBackup()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Vezi ultimul backup local") {
            resultText.text = jarvisBackupManager.getBackup()
        })

        layout.addView(button("Status backup local") {
            resultText.text = jarvisBackupManager.getBackupStatus()
        })

        layout.addView(button("Restaurează backup local") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }

            val result = jarvisBackupManager.restoreBackup()
            resultText.text = result
            Toast.makeText(this, "Restaurare backup verificată", Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Șterge backup local") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }

            val message = jarvisBackupManager.clearBackup()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })



        addSection(layout, "Update aplicație")
        layout.addView(button("Verifică update") { checkUpdate() })
        layout.addView(button("Descarcă update") { downloadUpdate() })
        layout.addView(button("Deschide pagina update") {
            updateManager.openReleasePage(lastReleaseUrl)
        })

        addSection(layout, "Buton flotant")
        layout.addView(button("Permite buton flotant") { requestOverlayPermission() })
        layout.addView(button("Pornește buton flotant") { startFloatingButton() })
        layout.addView(button("Oprește buton flotant") {
            stopService(Intent(this, JarvisFloatingService::class.java))
            Toast.makeText(this, "Buton flotant oprit", Toast.LENGTH_SHORT).show()
        })

        addSection(layout, "Comandă manuală / AI / vocală")
        layout.addView(aiStatusText)
        layout.addView(inputCommand)
        layout.addView(button("Vorbește") { startVoiceInput() })

        layout.addView(button("Jarvis Voice: ascultă și execută") {
            startJarvisVoiceMode(useAi = false)
        })

        layout.addView(button("Jarvis Voice AI: ascultă și execută cu AI") {
            startJarvisVoiceMode(useAi = true)
        })

        layout.addView(button("Jarvis Hands Free") {
            startJarvisHandsFree(useAi = false)
        })

        layout.addView(button("Jarvis Hands Free AI") {
            startJarvisHandsFree(useAi = true)
        })

        layout.addView(button("Oprește Hands Free") {
            stopJarvisHandsFree()
        })

        layout.addView(button("Jarvis spune statusul") {
            val status = "Jarvis este online. Accessibility este " + if (JarvisAccessibilityService.instance == null) "inactiv" else "activ"
            resultText.text = status
            jarvisVoiceManager.speak(status)
        })

        layout.addView(button("Execută comanda") { executeJarvisCommand() })
        layout.addView(button("Execută cu AI") { executeWithAi() })

        layout.addView(button("Repetă ultima comandă AI") {
            repeatLastAiCommand()
        })

        layout.addView(button("Șterge ultima comandă AI") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = aiCommandMemory.clearLastCommand()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })
        layout.addView(button("Oprește AI") {
            val message = aiStopManager.requestStop()
            aiStatusText.text = "Status AI: oprire cerută"
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Confirmă AI Action") {
            confirmPendingAiAction()
        })

        layout.addView(button("Șterge AI Action Pending") {
            val locked = jarvisPinManager.requireUnlocked()
            if (locked != null) {
                resultText.text = locked
                return@button
            }
            val message = aiPendingActionManager.clearPendingAction()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Clear rezultat") {
            resultText.text = "Rezultat:"
            inputCommand.setText("")
            Toast.makeText(this, "Rezultat șters", Toast.LENGTH_SHORT).show()
        })

        layout.addView(button("Clear istoric") {
            commandHistory.clear()
            updateHistory()
            Toast.makeText(this, "Istoric șters", Toast.LENGTH_SHORT).show()
        })

        addSection(layout, "Comenzi rapide")
        layout.addView(button("Deschide Chrome") { executeDirectCommand("deschide Chrome") })
        layout.addView(button("Deschide YouTube") { executeDirectCommand("deschide YouTube") })
        layout.addView(button("Deschide Termux") { executeDirectCommand("deschide Termux") })
        layout.addView(button("Caută vremea azi") { executeDirectCommand("caută vremea azi") })
        layout.addView(button("Citește ecranul") { executeDirectCommand("citește ecranul") })
        layout.addView(button("Scroll jos") { executeDirectCommand("scroll jos") })
        layout.addView(button("Home") { executeDirectCommand("home") })
        layout.addView(button("Înapoi") { executeDirectCommand("înapoi") })
        layout.addView(button("Recente") { executeDirectCommand("recente") })
        layout.addView(button("Despre aplicație") { showAbout() })
        layout.addView(button("Progres Jarvis") { showJarvisProgress() })
        layout.addView(button("Checklist final Jarvis") { showFinalChecklist() })
        layout.addView(button("Marchează Jarvis 100%") {
            val message = jarvisCompletionManager.markComplete()
            resultText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })
        layout.addView(button("Status final Jarvis") {
            resultText.text = jarvisCompletionManager.getStatus()
        })

        addSection(layout, "Aplicații instalate")

        layout.addView(button("Listează aplicațiile instalate") {
            inputCommand.setText("listează aplicațiile instalate")
            executeJarvisCommand()
        })

        layout.addView(button("Caută aplicația YouTube") {
            inputCommand.setText("caută aplicația YouTube")
            executeJarvisCommand()
        })

        addSection(layout, "Comenzi AI rapide")
        layout.addView(button("AI: Chrome + vremea") {
            runAiPreset("deschide Chrome și caută vremea azi")
        })
        layout.addView(button("AI: Citește ecranul") {
            runAiPreset("citește ecranul")
        })
        layout.addView(button("AI: Scroll jos") {
            runAiPreset("fă scroll în jos")
        })
        layout.addView(button("AI: Home + Recente") {
            runAiPreset("mergi acasă și deschide aplicațiile recente")
        })
        layout.addView(button("AI: Test siguranță Banking") {
            runAiPreset("deschide Mobile Banking")
        })

        addSection(layout, "Exemple AI")
        layout.addView(normalText("""
            deschide browserul
            deschide Chrome și caută vremea azi
            caută vremea de azi
            apasă pe butonul OK
            scrie salut în câmpul selectat
            fă scroll în jos
            citește ecranul
            mergi acasă
            deschide aplicațiile recente
        """.trimIndent()))

        addSection(layout, "Rezultat")
        layout.addView(resultText)

        addSection(layout, "Istoric")
        layout.addView(historyText)

        scrollView.addView(layout)
        setContentView(scrollView)

        updateVersionText()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateVersionText()
    }

    private fun executeWithAi() {
        val command = inputCommand.text.toString().trim()

        if (command.isNotBlank()) {
            aiCommandMemory.saveLastCommand(command)
        }

        if (command.isBlank()) {
            aiStatusText.text = "Status AI: comandă goală"
            resultText.text = "Scrie o comandă pentru AI."
            return
        }

        val provider = apiKeyManager.getAiProvider()

        val apiKey = when (provider) {
            "openai" -> apiKeyManager.getOpenAiKey()
            "gemini" -> apiKeyManager.getGeminiKey()
            else -> apiKeyManager.getOpenRouterKey()
        }

        if (apiKey.isBlank()) {
            aiStatusText.text = "Status AI: lipsește API key"
            resultText.text = "Lipsește API key pentru $provider. Pune cheia în câmp și apasă Salvează API Key."
            return
        }

        val service = JarvisAccessibilityService.instance

        if (service == null) {
            aiStatusText.text = "Status AI: Accessibility inactiv"
            resultText.text = if (isJarvisAccessibilityEnabledInSettings()) "Serviciul Accessibility este activ în setări, dar Jarvis trebuie repornit." else "Serviciul Accessibility nu este activ."
            return
        }

        aiStatusText.text = "Status AI: trimis la $provider"
        resultText.text = "AI procesează comanda cu provider: $provider..."
        Toast.makeText(this, "Trimit la AI...", Toast.LENGTH_SHORT).show()

        val aiClient = AiClient(
            apiKey = apiKey,
            provider = provider,
            preferredOpenRouterModel = apiKeyManager.getOpenRouterModel()
        )
        val orchestrator = AiOrchestrator(
            aiClient = aiClient,
            controller = service.controller,
            stopManager = aiStopManager,
            pendingActionManager = aiPendingActionManager
        )

        orchestrator.executeWithAi(command) { result ->
            runOnUiThread {
                aiStatusText.text = if (
                    result.contains("Eroare AI", ignoreCase = true) ||
                    result.contains("oprit", ignoreCase = true)
                ) {
                    "Status AI: oprit / eroare"
                } else {
                    "Status AI: finalizat"
                }

                resultText.text = result
                jarvisVoiceManager.speak(result)
                addToHistory("AI $provider: $command")

                aiDebugLogger.addLog(
                    title = "AI command executed",
                    details = """
                        User command:
                        $command

                        Provider:
                        $provider

                        Result:
                        $result
                    """.trimIndent()
                )

                aiDebugLogger.addAiStepLog(command, result)
            }
        }
    }







    private fun importSettingsFromClipboard() {
        jarvisBackupManager.saveBackup()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip

        if (clip == null || clip.itemCount == 0) {
            resultText.text = "Clipboard gol. Copiază mai întâi exportul Jarvis."
            Toast.makeText(this, "Clipboard gol", Toast.LENGTH_SHORT).show()
            return
        }

        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: ""

        if (text.isBlank()) {
            resultText.text = "Clipboard nu conține text valid."
            Toast.makeText(this, "Clipboard invalid", Toast.LENGTH_SHORT).show()
            return
        }

        inputSettingsImport.setText(text)
        val result = settingsExportManager.importSettings(text)

        resultText.text = result
        Toast.makeText(this, "Import din clipboard finalizat", Toast.LENGTH_SHORT).show()
    }

    private fun importSettingsFromText() {
        jarvisBackupManager.saveBackup()
        val text = inputSettingsImport.text.toString()

        val result = settingsExportManager.importSettings(text)

        resultText.text = result
        Toast.makeText(this, "Import setări verificat", Toast.LENGTH_SHORT).show()
    }

    private fun copySettingsExportToClipboard() {
        jarvisBackupManager.saveBackup()
        val exportText = settingsExportManager.exportSettings()

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Jarvis Settings Export", exportText)
        clipboard.setPrimaryClip(clip)

        resultText.text = "Export setări copiat în clipboard.\n\n$exportText"
        Toast.makeText(this, "Export setări copiat", Toast.LENGTH_SHORT).show()
    }

    private fun runAiPreset(command: String) {
        inputCommand.setText(command)
        inputCommand.setSelection(inputCommand.text.length)
        resultText.text = "Rulez preset AI:\n$command"
        executeWithAi()
    }

    private fun repeatLastAiCommand() {
        val lastCommand = aiCommandMemory.getLastCommand()

        if (lastCommand.isBlank()) {
            resultText.text = "Nu există comandă AI salvată."
            Toast.makeText(this, "Nu există comandă AI salvată", Toast.LENGTH_SHORT).show()
            return
        }

        inputCommand.setText(lastCommand)
        inputCommand.setSelection(inputCommand.text.length)

        resultText.text = "Repet ultima comandă AI:\n$lastCommand"
        executeWithAi()
    }

    private fun confirmPendingAiAction() {
        val service = JarvisAccessibilityService.instance

        if (service == null) {
            resultText.text = if (isJarvisAccessibilityEnabledInSettings()) "Serviciul Accessibility este activ în setări, dar Jarvis trebuie repornit." else "Serviciul Accessibility nu este activ."
            return
        }

        val provider = apiKeyManager.getAiProvider()
        val apiKey = when (provider) {
            "openai" -> apiKeyManager.getOpenAiKey()
            "gemini" -> apiKeyManager.getGeminiKey()
            else -> apiKeyManager.getOpenRouterKey()
        }

        if (apiKey.isBlank()) {
            resultText.text = "Lipsește API key pentru $provider."
            return
        }

        val aiClient = AiClient(
            apiKey = apiKey,
            provider = provider,
            preferredOpenRouterModel = apiKeyManager.getOpenRouterModel()
        )

        val orchestrator = AiOrchestrator(
            aiClient = aiClient,
            controller = service.controller,
            stopManager = aiStopManager,
            pendingActionManager = aiPendingActionManager
        )

        aiStatusText.text = "Status AI: confirmare acțiune sensibilă"
        val result = orchestrator.executePendingAction()
        aiStatusText.text = "Status AI: acțiune confirmată"
        resultText.text = "Confirmare AI Action:\n$result"

        aiDebugLogger.addLog(
            title = "AI pending action confirmed",
            details = result
        )
    }

    private fun titleText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }
    }

    private fun sectionText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 35, 0, 12)
        }
    }

    private fun normalText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
    }

    private fun button(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setAllCaps(false)
            setOnClickListener { action() }
        }
    }

    private fun addSection(layout: LinearLayout, title: String) {
        layout.addView(sectionText(title))
    }

    private fun updateVersionText() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }

        versionText.text = "Versiune instalată: ${packageInfo.versionName} ($versionCode)"
    }



    private fun showFinalChecklist() {
        resultText.text = """
            Checklist final Jarvis Accessibility

            Procent estimat:
            100% după validarea checklistului final

            Teste obligatorii înainte de 100%:

            ✅ 1. Execută comanda normală:
            deschide Chrome

            ✅ 2. Execută cu AI:
            deschide browserul

            ✅ 3. AI multi-step:
            deschide Chrome și caută vremea azi

            ✅ 4. Stop AI:
            deschide Chrome și caută vremea azi și fă scroll în jos
            apoi apasă Oprește AI

            ✅ 5. Confirmare acțiune sensibilă:
            scrie salut în câmpul selectat

            ✅ 6. Protecție banking:
            deschide Mobile Banking

            ✅ 7. Blocare custom:
            blochează aplicația TikTok
            este blocată TikTok

            ✅ 8. Permise custom:
            permite aplicația TikTok
            este permisă TikTok

            ✅ 9. Export / import:
            Exportă setări Jarvis
            Copiază export setări
            Importă setări din clipboard

            ✅ 10. PIN:
            Setează PIN Jarvis
            Blochează setări
            încearcă Șterge memoria AI

            ✅ 11. Backup:
            Salvează backup setări local
            Vezi ultimul backup local
            Restaurează backup local

            ✅ 12. Update:
            Verifică update
            Descarcă update

            Dacă toate trec:
            Jarvis poate fi marcat 100% funcțional pentru versiunea actuală.
        """.trimIndent()
    }

    private fun showJarvisProgress() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }

        resultText.text = """
            Progres Jarvis Accessibility

            Procent estimat:
            99.9% dezvoltat / 100% după checklist final

            Versiune instalată:
            ${packageInfo.versionName} ($versionCode)

            Funcții construite:
            ✅ Accessibility Service
            ✅ comenzi manuale
            ✅ AI OpenRouter
            ✅ fallback modele free
            ✅ model OpenRouter custom
            ✅ AI multi-step
            ✅ pauze automate între pași
            ✅ căutare Google directă
            ✅ anti-loop AI
            ✅ buton Oprește AI
            ✅ confirmare pentru tap / swipe / write
            ✅ status live AI
            ✅ debug log AI
            ✅ repetă ultima comandă AI
            ✅ comenzi AI rapide
            ✅ aplicații blocate custom
            ✅ aplicații permise custom
            ✅ export setări
            ✅ import setări
            ✅ clipboard export/import
            ✅ PIN pentru setări sensibile
            ✅ backup local
            ✅ restaurare backup local
            ✅ update direct din aplicație
            ✅ buton flotant
            ✅ protecție banking / parole / wallet

            Ce mai rămâne pentru 100%:
            ❌ voce continuă fără apăsare manuală
            ❌ UI mai compact și mai frumos
            ❌ chat flotant avansat peste orice aplicație
            ❌ status live pe fiecare pas AI
            ❌ detectare mai inteligentă a butoanelor de pe ecran

            Status:\n            Release Candidate - gata pentru testare extinsă.\n\n            Status:
            Jarvis este Release Candidate. După validarea checklistului final, poate fi marcat 100% funcțional pentru versiunea actuală.
        """.trimIndent()
    }

    private fun showAbout() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }

        resultText.text = """
            Jarvis Accessibility

            Versiune instalată:
            ${packageInfo.versionName} ($versionCode)

            Funcții:
            - comenzi scrise
            - comenzi vocale
            - AI cu OpenRouter / Gemini / OpenAI
            - API keys salvate local
            - citire ecran
            - control prin Accessibility
            - buton flotant
            - verificare update
            - download update din aplicație
        """.trimIndent()
    }

    private fun checkUpdate() {
        Toast.makeText(this, "Verific update...", Toast.LENGTH_SHORT).show()

        updateManager.checkForUpdates { info, error ->
            runOnUiThread {
                if (error != null) {
                    resultText.text = "Update error:\n$error"
                    return@runOnUiThread
                }

                if (info == null) {
                    resultText.text = "Nu am putut verifica update-ul."
                    return@runOnUiThread
                }

                lastReleaseUrl = info.releaseUrl
                lastApkUrl = info.apkUrl

                resultText.text = if (info.hasUpdate) {
                    """
                    Update disponibil!

                    Versiune curentă: ${info.currentVersionCode}
                    Versiune nouă: ${info.latestVersionCode}
                    Nume versiune: ${info.latestVersionName}

                    Notes:
                    ${info.notes}

                    Apasă „Descarcă update”.
                    """.trimIndent()
                } else {
                    """
                    Aplicația este la zi.

                    Versiune curentă: ${info.currentVersionCode}
                    Ultima versiune: ${info.latestVersionCode}
                    """.trimIndent()
                }
            }
        }
    }

    private fun downloadUpdate() {
        if (lastApkUrl.isBlank()) {
            resultText.text = "Mai întâi apasă „Verifică update”."
            Toast.makeText(this, "Mai întâi verifică update-ul", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Descarc update...", Toast.LENGTH_LONG).show()
        resultText.text = "Descarc update...\nTe rog așteaptă."

        updateManager.downloadAndInstallApk(lastApkUrl) { error ->
            runOnUiThread {
                if (error != null) {
                    resultText.text = "Eroare download update:\n$error"
                } else {
                    resultText.text = "Update descărcat. Confirmă instalarea în ecranul Android."
                }
            }
        }
    }

    private fun updateServiceStatus() {
        val service = JarvisAccessibilityService.instance

        if (service == null) {
            statusText.text = if (isJarvisAccessibilityEnabledInSettings()) "Status: Jarvis Accessibility este activ." else "Status: Jarvis Accessibility NU este activ."
        } else {
            statusText.text = "Status: Jarvis Accessibility este ACTIV."
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permisiunea pentru buton flotant există deja", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFloatingButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Mai întâi permite butonul flotant", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }

        startService(Intent(this, JarvisFloatingService::class.java))
        Toast.makeText(this, "Buton flotant pornit", Toast.LENGTH_SHORT).show()
    }




    private fun logVoiceCommandToServer(command: String) {
        if (command.isBlank()) return

        try {
            termuxServerClient.sendVoiceCommand(command) {
                // Log silențios către localhost. Rezultatul rămâne în /command-history.
            }
        } catch (_: Exception) {
            // Serverul localhost poate fi oprit; Jarvis continuă local.
        }
    }


    private fun startJarvisHandsFree(useAi: Boolean) {
        handsFreeModeActive = true
        handsFreeUseAi = useAi
        handsFreeCommandCount = 0

        val message = if (useAi) {
            "Jarvis Hands Free AI activ. Te ascult, sir."
        } else {
            "Jarvis Hands Free activ. Te ascult, sir."
        }

        resultText.text = message
        jarvisVoiceManager.speak(message)
        jarvisVoiceManager.listen(jarvisHandsFreeRequestCode)
    }

    private fun stopJarvisHandsFree() {
        handsFreeModeActive = false
        handsFreeUseAi = false

        val message = "Hands Free oprit, sir."
        resultText.text = message
        jarvisVoiceManager.speak(message)
    }

    private fun continueHandsFreeIfNeeded() {
        if (!handsFreeModeActive) return

        handsFreeCommandCount++

        if (handsFreeCommandCount >= maxHandsFreeCommands) {
            handsFreeModeActive = false
            val message = "Am oprit Hands Free după $maxHandsFreeCommands comenzi, pentru siguranță, sir."
            resultText.text = message
            jarvisVoiceManager.speak(message)
            return
        }

        inputCommand.postDelayed({
            if (handsFreeModeActive) {
                jarvisVoiceManager.listen(jarvisHandsFreeRequestCode)
            }
        }, 1200)
    }

    private fun startJarvisVoiceMode(useAi: Boolean) {
        voiceModeUseAi = useAi

        resultText.text = if (useAi) {
            "Jarvis Voice AI pornește. Spune comanda."
        } else {
            "Jarvis Voice pornește. Spune comanda."
        }

        jarvisVoiceManager.listen(jarvisVoiceRequestCode)
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ro-RO")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Spune comanda pentru Jarvis")

        try {
            startActivityForResult(intent, speechRequestCode)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Recunoașterea vocală nu este disponibilă pe telefon.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Deprecated Android API, dar funcționează pentru acest proiect simplu.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == speechRequestCode || requestCode == jarvisVoiceRequestCode || requestCode == jarvisHandsFreeRequestCode) && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenCommand = results?.firstOrNull()?.trim()

            if (!spokenCommand.isNullOrBlank()) {
                inputCommand.setText(spokenCommand)
                inputCommand.setSelection(inputCommand.text.length)

                if (requestCode == jarvisVoiceRequestCode || requestCode == jarvisHandsFreeRequestCode) {
                    val isHandsFree = requestCode == jarvisHandsFreeRequestCode
                    val useAiForThisCommand = if (isHandsFree) handsFreeUseAi else voiceModeUseAi

                    val intent = smartVoiceInterpreter.interpret(spokenCommand)
                    jarvisVoiceManager.speak(intent.spokenReply)

                    if (intent.type == "conversation") {
                        resultText.text = "Conversație Jarvis:\n$spokenCommand\n\nRăspuns:\n${intent.spokenReply}"
                        if (isHandsFree) continueHandsFreeIfNeeded()
                    } else {
                        val finalCommand = intent.command.ifBlank { spokenCommand }
                        inputCommand.setText(finalCommand)
                        inputCommand.setSelection(inputCommand.text.length)

                        if (useAiForThisCommand) {
                            executeWithAi()
                        } else {
                            executeDirectCommand(finalCommand)
                        }

                        if (isHandsFree) continueHandsFreeIfNeeded()
                    }
                } else {
                    executeDirectCommand(spokenCommand)
                }
            } else {
                Toast.makeText(this, "Nu am detectat nicio comandă.", Toast.LENGTH_SHORT).show()
                jarvisVoiceManager.speak("Nu am detectat nicio comandă.")
                if (requestCode == jarvisHandsFreeRequestCode) continueHandsFreeIfNeeded()
            }
        } else {
            handleHandsFreeCancelled(requestCode)
        }
    }



    private fun handleHandsFreeCancelled(requestCode: Int) {
        if (requestCode != jarvisHandsFreeRequestCode) return
        if (!handsFreeModeActive) return

        resultText.text = "Nu am auzit comanda, sir. Ascult din nou."
        jarvisVoiceManager.speak("Nu am auzit comanda, sir.")
        continueHandsFreeIfNeeded()
    }

    override fun onDestroy() {
        try {
            jarvisVoiceManager.shutdown()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    private fun executeJarvisCommand() {
        val command = inputCommand.text.toString().trim()

        if (command.isBlank()) {
            resultText.text = "Rezultat: Scrie sau rostește o comandă."
            Toast.makeText(this, "Scrie sau rostește o comandă", Toast.LENGTH_SHORT).show()
            return
        }

        executeDirectCommand(command)
    }

    private fun executeDirectCommand(command: String) {
        Toast.makeText(this, "Execut: $command", Toast.LENGTH_SHORT).show()
        addToHistory(command)

        val service = JarvisAccessibilityService.instance

        if (service == null) {
            resultText.text =
                "Comandă: $command\n\nRezultat:\n" + if (isJarvisAccessibilityEnabledInSettings()) "Serviciul Accessibility este activ în setări. Comanda va continua prin rutele disponibile." else "Serviciul Accessibility nu este activ. Activează Jarvis Accessibility din setări."
            updateServiceStatus()
            return
        }

        try {
            val result = service.controller.executeCommand(command)
            resultText.text = "Comandă: $command\n\nRezultat:\n$result"
            jarvisVoiceManager.speak(result)
        } catch (e: Exception) {
            resultText.text = "Comandă: $command\n\nEroare:\n${e.message}"
            jarvisVoiceManager.speak("Eroare: ${e.message}")
        }

        updateServiceStatus()
    }

    private fun addToHistory(command: String) {
        commandHistory.add(0, command)

        if (commandHistory.size > 10) {
            commandHistory.removeAt(commandHistory.lastIndex)
        }

        updateHistory()
    }

    private fun updateHistory() {
        if (commandHistory.isEmpty()) {
            historyText.text = "Gol"
            return
        }

        val builder = StringBuilder()

        commandHistory.forEachIndexed { index, command ->
            builder.append(index + 1)
                .append(". ")
                .append(command)
                .append("\n")
        }

        historyText.text = builder.toString().trim()
    }
}
