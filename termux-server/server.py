from flask import Flask, request, jsonify
from datetime import datetime
import subprocess
import os
import json
import urllib.request
import urllib.parse

app = Flask(__name__)

START_TIME = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
LAST_COMMAND = ""
COMMAND_HISTORY = []
VOICE_COMMAND_HISTORY = []
AUTO_UPDATE_STATUS = "not_checked"

LOCAL_SERVER_VERSION_CODE = 8
LOCAL_SERVER_VERSION_NAME = "1.0.7"

ANDROID_APP_VERSION_CODE = 73
ANDROID_APP_VERSION_NAME = "1.8.1"

REMOTE_VERSION_URL = "https://raw.githubusercontent.com/andrei99989/JarvisAccessibility/main/server_version.json"
LOCAL_SERVER_PATH = os.path.abspath(__file__)

def safe_shell(cmd, timeout_seconds=8):
    try:
        result = subprocess.check_output(
            cmd,
            shell=True,
            stderr=subprocess.STDOUT,
            timeout=timeout_seconds
        )
        return result.decode("utf-8", errors="ignore")
    except subprocess.TimeoutExpired:
        return (
            "Termux:API nu răspunde. Instalează/deschide aplicația Android Termux:API "
            "și dezactivează optimizarea bateriei pentru Termux și Termux:API."
        )
    except subprocess.CalledProcessError as e:
        output = e.output.decode("utf-8", errors="ignore")
        if "not found" in output:
            return "Comanda Termux:API lipsește. Rulează: pkg install termux-api -y"
        return output
    except Exception as e:
        return str(e)

def download_text(url):
    with urllib.request.urlopen(url, timeout=20) as response:
        return response.read().decode("utf-8", errors="ignore")

def get_remote_server_info():
    raw = download_text(REMOTE_VERSION_URL)
    return json.loads(raw)


def add_command_history(text, intent, reply):
    global COMMAND_HISTORY

    item = {
        "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "text": text,
        "intent": intent,
        "reply": reply
    }

    COMMAND_HISTORY.insert(0, item)
    COMMAND_HISTORY = COMMAND_HISTORY[:30]


def add_voice_command_history(text, intent, reply):
    global VOICE_COMMAND_HISTORY

    item = {
        "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "text": text,
        "intent": intent,
        "reply": reply
    }

    VOICE_COMMAND_HISTORY.insert(0, item)
    VOICE_COMMAND_HISTORY = VOICE_COMMAND_HISTORY[:30]

def normalize(text):
    return (
        text.strip()
        .lower()
        .replace("ă", "a")
        .replace("â", "a")
        .replace("î", "i")
        .replace("ș", "s")
        .replace("ş", "s")
        .replace("ț", "t")
        .replace("ţ", "t")
    )

def open_url(url):
    safe_shell(f'am start -a android.intent.action.VIEW -d "{url}"')
    return url

def speak_text(text):
    return safe_shell(f'termux-tts-speak "{text}"', timeout_seconds=5)

def extract_after_phrases(text, phrases):
    value = text.strip()

    for phrase in phrases:
        value = value.replace(phrase, "", 1).strip()

    return value.strip(" .?,")

def interpret_command(text):
    original = text.strip()
    clean = normalize(original)

    if not original:
        return {
            "intent": "empty",
            "reply": "No command received, sir.",
            "action": "none",
            "result": ""
        }

    if "status telefon" in clean or clean == "status" or "device status" in clean:
        return {
            "intent": "device_status",
            "reply": "Checking device status, sir.",
            "action": "device",
            "result": {
                "battery": safe_shell("termux-battery-status", timeout_seconds=5),
                "wifi": safe_shell("termux-wifi-connectioninfo", timeout_seconds=5),
                "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
        }

    if "baterie" in clean or "battery" in clean:
        return {
            "intent": "battery",
            "reply": "Checking battery, sir.",
            "action": "battery",
            "result": safe_shell("termux-battery-status", timeout_seconds=5)
        }

    if "wifi" in clean or "wi-fi" in clean:
        return {
            "intent": "wifi",
            "reply": "Checking Wi-Fi, sir.",
            "action": "wifi",
            "result": safe_shell("termux-wifi-connectioninfo", timeout_seconds=5)
        }

    if clean.startswith("spune ") or clean.startswith("say "):
        to_say = original
        to_say = to_say.replace("spune", "", 1).replace("say", "", 1).strip()
        if not to_say:
            to_say = "At your service, sir."
        output = speak_text(to_say)

        return {
            "intent": "speak",
            "reply": f"Speaking, sir: {to_say}",
            "action": "tts",
            "result": output
        }

    if "youtube" in clean or "yootube" in clean or "you tube" in clean:
        wants_search = (
            "cauta" in clean or
            "search" in clean or
            "gaseste" in clean or
            "melodie" in clean or
            "muzica" in clean or
            "stiri" in clean or
            "content" in clean
        )

        if wants_search:
            query = original
            remove_phrases = [
                "caută în youtube",
                "cauta in youtube",
                "caută pe youtube",
                "cauta pe youtube",
                "search youtube for",
                "search on youtube",
                "youtube",
                "yootube",
                "you tube",
                "melodie",
                "muzică",
                "muzica",
                "știri",
                "stiri",
                "content"
            ]

            for phrase in remove_phrases:
                query = query.replace(phrase, "", 1).strip()

            query = query.strip(" .?,")
            if not query:
                query = "YouTube"

            url = "https://www.youtube.com/results?search_query=" + urllib.parse.quote(query)
            open_url(url)

            return {
                "intent": "youtube_search",
                "reply": f"Searching YouTube for {query}, sir.",
                "action": "open_url",
                "result": url
            }

        url = "https://www.youtube.com"
        open_url(url)

        return {
            "intent": "open_youtube",
            "reply": "Opening YouTube, sir.",
            "action": "open_url",
            "result": url
        }

    if "chrome" in clean or "browser" in clean or "google" in clean:
        if "cauta" in clean or "search" in clean or "gaseste" in clean:
            query = original
            for phrase in [
                "caută în chrome",
                "cauta in chrome",
                "caută pe chrome",
                "cauta pe chrome",
                "search chrome for",
                "search google for",
                "chrome",
                "google chrome",
                "google",
                "browser",
                "caută",
                "cauta",
                "search",
                "găsește",
                "gaseste"
            ]:
                query = query.replace(phrase, "", 1).strip()

            query = query.strip(" .?,")
            if not query:
                query = "Google"

            url = "https://www.google.com/search?q=" + urllib.parse.quote(query)
            open_url(url)

            return {
                "intent": "google_search",
                "reply": f"Searching Google for {query}, sir.",
                "action": "open_url",
                "result": url
            }

        url = "https://www.google.com"
        open_url(url)

        return {
            "intent": "open_chrome",
            "reply": "Opening Chrome, sir.",
            "action": "open_url",
            "result": url
        }

    if clean.startswith("cauta ") or clean.startswith("search "):
        query = original
        query = query.replace("caută", "", 1).replace("cauta", "", 1).replace("search", "", 1).strip()
        query = query.strip(" .?,")

        if not query:
            query = "Jarvis"

        url = "https://www.google.com/search?q=" + urllib.parse.quote(query)
        open_url(url)

        return {
            "intent": "google_search",
            "reply": f"Searching Google for {query}, sir.",
            "action": "open_url",
            "result": url
        }

    if "update server" in clean or "verifica update server" in clean or "verifică update server" in clean:
        try:
            remote = get_remote_server_info()
            remote_code = int(remote.get("serverVersionCode", 0))
            return {
                "intent": "server_update_check",
                "reply": "Checking server update, sir.",
                "action": "update_check",
                "result": {
                    "localVersionCode": LOCAL_SERVER_VERSION_CODE,
                    "localVersionName": LOCAL_SERVER_VERSION_NAME,
                    "remoteVersionCode": remote_code,
                    "remoteVersionName": remote.get("serverVersionName", ""),
                    "updateAvailable": remote_code > LOCAL_SERVER_VERSION_CODE,
                    "notes": remote.get("notes", "")
                }
            }
        except Exception as e:
            return {
                "intent": "server_update_check",
                "reply": "I could not check the server update, sir.",
                "action": "error",
                "result": str(e)
            }

    return {
        "intent": "unknown",
        "reply": f"Command received, sir: {original}",
        "action": "none",
        "result": original
    }


def auto_update_on_start():
    global AUTO_UPDATE_STATUS

    try:
        remote = get_remote_server_info()
        remote_code = int(remote.get("serverVersionCode", 0))
        server_url = remote.get("serverUrl", "")

        if remote_code <= LOCAL_SERVER_VERSION_CODE:
            AUTO_UPDATE_STATUS = "Serverul este la zi."
            return

        if not server_url:
            AUTO_UPDATE_STATUS = "Update disponibil, dar serverUrl lipsește."
            return

        new_server = download_text(server_url)

        backup_path = LOCAL_SERVER_PATH + ".bak"
        with open(backup_path, "w", encoding="utf-8") as backup:
            with open(LOCAL_SERVER_PATH, "r", encoding="utf-8") as current:
                backup.write(current.read())

        with open(LOCAL_SERVER_PATH, "w", encoding="utf-8") as target:
            target.write(new_server)

        AUTO_UPDATE_STATUS = (
            f"Update automat instalat la server {remote.get('serverVersionName', remote_code)}. "
            "Repornește serverul Termux pentru activare."
        )

    except Exception as e:
        AUTO_UPDATE_STATUS = f"Auto-update server eșuat: {e}"


@app.route("/")
def home():
    return """
    <html>
    <head>
        <title>Jarvis Termux Server</title>
        <style>
            body {
                background: #020817;
                color: #0EA5E9;
                font-family: monospace;
                padding: 30px;
            }
            .panel {
                border: 1px solid #0EA5E9;
                border-radius: 14px;
                padding: 20px;
                margin-bottom: 20px;
                background: #071426;
            }
            h1 { color: #22C55E; }
            code { color: #FACC15; }
        </style>
    </head>
    <body>
        <div class="panel">
            <h1>JARVIS TERMUX SERVER ONLINE</h1>
            <p>Status: active</p>
            <p>Jarvis Android App: """ + ANDROID_APP_VERSION_NAME + """ (""" + str(ANDROID_APP_VERSION_CODE) + """)</p>
            <p>Jarvis Termux Server: """ + LOCAL_SERVER_VERSION_NAME + """ (""" + str(LOCAL_SERVER_VERSION_CODE) + """)</p>
            <p>Auto update: """ + AUTO_UPDATE_STATUS + """</p>
            <p>Command history: """ + str(len(COMMAND_HISTORY)) + """</p>
            <p>Voice history: """ + str(len(VOICE_COMMAND_HISTORY)) + """</p>
            <p>Endpoints:</p>
            <p><code>/status</code></p>
            <p><code>/command?text=hello</code></p>
            <p><code>/voice-history</code></p>
            <p><code>/command-history</code></p>
            <p><code>/clear-history</code></p>
            <p><code>/say?text=hello sir</code></p>
            <p><code>/device</code></p>
            <p><code>/update/check</code></p>
            <p><code>/update/install</code></p>
        </div>
    </body>
    </html>
    """

@app.route("/status")
def status():
    return jsonify({
        "status": "online",
        "name": "Jarvis Termux Server",
        "started_at": START_TIME,
        "last_command": LAST_COMMAND,
        "commandHistoryCount": len(COMMAND_HISTORY),
        "voiceCommandCount": len(VOICE_COMMAND_HISTORY),
        "lastVoiceCommand": VOICE_COMMAND_HISTORY[0]["text"] if VOICE_COMMAND_HISTORY else "",
        "serverVersionCode": LOCAL_SERVER_VERSION_CODE,
        "serverVersionName": LOCAL_SERVER_VERSION_NAME,
        "androidAppVersionCode": ANDROID_APP_VERSION_CODE,
        "androidAppVersionName": ANDROID_APP_VERSION_NAME,
        "autoUpdateStatus": AUTO_UPDATE_STATUS
    })

@app.route("/command", methods=["GET", "POST"])
def command():
    global LAST_COMMAND

    if request.method == "POST":
        data = request.get_json(silent=True) or {}
        text = data.get("text", "")
    else:
        text = request.args.get("text", "")

    text = text.strip()
    source = request.args.get("source", "").strip().lower()
    LAST_COMMAND = text

    brain = interpret_command(text)
    add_command_history(text, brain.get("intent"), brain.get("reply"))

    if source == "voice":
        add_voice_command_history(text, brain.get("intent"), brain.get("reply"))

    return jsonify({
        "ok": True,
        "received": text,
        "intent": brain.get("intent"),
        "reply": brain.get("reply"),
        "action": brain.get("action"),
        "result": brain.get("result")
    })

@app.route("/say")
def say():
    text = request.args.get("text", "At your service, sir.").strip()

    if not text:
        text = "At your service, sir."

    output = speak_text(text)

    return jsonify({
        "ok": True,
        "spoken": text,
        "output": output
    })

@app.route("/device")
def device():
    return jsonify({
        "battery": safe_shell("termux-battery-status", timeout_seconds=5),
        "wifi": safe_shell("termux-wifi-connectioninfo", timeout_seconds=5),
        "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    })



@app.route("/voice-history")
def voice_history():
    return jsonify({
        "ok": True,
        "count": len(VOICE_COMMAND_HISTORY),
        "items": VOICE_COMMAND_HISTORY
    })

@app.route("/command-history")
def command_history():
    return jsonify({
        "ok": True,
        "count": len(COMMAND_HISTORY),
        "items": COMMAND_HISTORY
    })

@app.route("/clear-history")
def clear_history():
    global COMMAND_HISTORY
    COMMAND_HISTORY = []
VOICE_COMMAND_HISTORY = []

    return jsonify({
        "ok": True,
        "message": "Istoricul comenzilor serverului a fost șters."
    })

@app.route("/update/check")
def update_check():
    try:
        remote = get_remote_server_info()
        remote_code = int(remote.get("serverVersionCode", 0))

        return jsonify({
            "ok": True,
            "localVersionCode": LOCAL_SERVER_VERSION_CODE,
            "localVersionName": LOCAL_SERVER_VERSION_NAME,
            "remoteVersionCode": remote_code,
            "remoteVersionName": remote.get("serverVersionName", ""),
            "updateAvailable": remote_code > LOCAL_SERVER_VERSION_CODE,
            "notes": remote.get("notes", ""),
            "serverUrl": remote.get("serverUrl", "")
        })
    except Exception as e:
        return jsonify({
            "ok": False,
            "error": str(e)
        })

@app.route("/update/install")
def update_install():
    try:
        remote = get_remote_server_info()
        remote_code = int(remote.get("serverVersionCode", 0))
        server_url = remote.get("serverUrl", "")

        if remote_code <= LOCAL_SERVER_VERSION_CODE:
            return jsonify({
                "ok": True,
                "message": "Serverul este deja la zi.",
                "localVersionCode": LOCAL_SERVER_VERSION_CODE
            })

        if not server_url:
            return jsonify({
                "ok": False,
                "error": "serverUrl lipsește în server_version.json"
            })

        new_server = download_text(server_url)

        backup_path = LOCAL_SERVER_PATH + ".bak"
        with open(backup_path, "w", encoding="utf-8") as backup:
            with open(LOCAL_SERVER_PATH, "r", encoding="utf-8") as current:
                backup.write(current.read())

        with open(LOCAL_SERVER_PATH, "w", encoding="utf-8") as target:
            target.write(new_server)

        return jsonify({
            "ok": True,
            "message": "Update server instalat. Repornește serverul Termux.",
            "backup": backup_path,
            "newVersionCode": remote_code,
            "newVersionName": remote.get("serverVersionName", "")
        })

    except Exception as e:
        return jsonify({
            "ok": False,
            "error": str(e)
        })

if __name__ == "__main__":
    auto_update_on_start()
    print("Jarvis Termux Server running on http://127.0.0.1:8765")
    print("Auto update status:", AUTO_UPDATE_STATUS)
    app.run(host="127.0.0.1", port=8765)
