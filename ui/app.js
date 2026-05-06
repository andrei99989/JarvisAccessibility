const HUD_CONFIG = {
  restEndpoint: "http://127.0.0.1:8765/status",
  commandEndpoint: "http://127.0.0.1:8765/command-history",
  websocketUrl: "ws://127.0.0.1:8765/ws",
  pollIntervalMs: 3500,
  websocketEnabled: true,
  restPollingEnabled: true
};

const state = {
  commandCount: 0,
  lastCommand: "",
  websocket: null
};

const elements = {
  clock: document.getElementById("clock"),
  connectionStatus: document.getElementById("connectionStatus"),
  backendMode: document.getElementById("backendMode"),
  aiStatus: document.getElementById("aiStatus"),
  voiceStatus: document.getElementById("voiceStatus"),
  serverStatus: document.getElementById("serverStatus"),
  accessibilityStatus: document.getElementById("accessibilityStatus"),
  commandLog: document.getElementById("commandLog"),
  commandCount: document.getElementById("commandCount"),
  lastCommand: document.getElementById("lastCommand"),
  coreMode: document.getElementById("coreMode"),
  voiceVisualizer: document.getElementById("voiceVisualizer"),
  listeningOrb: document.getElementById("listeningOrb"),
  listeningState: document.getElementById("listeningState"),
  listeningText: document.getElementById("listeningText"),
  processingBars: document.getElementById("processingBars"),
  processingState: document.getElementById("processingState"),
  processingText: document.getElementById("processingText")
};

function bootHud() {
  startClock();
  addCommandLog("HUD boot complete. Jarvis interface standing by.", "boot");
  setHudMode("standby");

  if (HUD_CONFIG.websocketEnabled) {
    connectWebSocket();
  }

  if (HUD_CONFIG.restPollingEnabled) {
    pollBackend();
    setInterval(pollBackend, HUD_CONFIG.pollIntervalMs);
  }

  window.JarvisHUD = {
    receiveCommand,
    setHudMode,
    addCommandLog,
    updateSystemStatus
  };
}

function startClock() {
  updateClock();
  setInterval(updateClock, 1000);
}

function updateClock() {
  elements.clock.textContent = new Date().toLocaleTimeString("ro-RO", {
    hour12: false
  });
}

function connectWebSocket() {
  try {
    const socket = new WebSocket(HUD_CONFIG.websocketUrl);
    state.websocket = socket;

    socket.addEventListener("open", () => {
      elements.connectionStatus.textContent = "WS CONNECTED";
      elements.backendMode.textContent = "WEBSOCKET";
      addCommandLog("WebSocket connected to Termux backend.", "system");
    });

    socket.addEventListener("message", event => {
      handleBackendMessage(event.data, "websocket");
    });

    socket.addEventListener("close", () => {
      elements.connectionStatus.textContent = "WS CLOSED / REST ACTIVE";
      elements.backendMode.textContent = "REST POLLING";
      addCommandLog("WebSocket closed. REST polling remains active.", "warning");
    });

    socket.addEventListener("error", () => {
      elements.connectionStatus.textContent = "WS ERROR / REST ACTIVE";
      elements.backendMode.textContent = "REST POLLING";
    });
  } catch (error) {
    elements.connectionStatus.textContent = "WS UNAVAILABLE";
    elements.backendMode.textContent = "REST POLLING";
  }
}

async function pollBackend() {
  try {
    const statusResponse = await fetch(HUD_CONFIG.restEndpoint, { cache: "no-store" });

    if (statusResponse.ok) {
      const status = await statusResponse.json();
      updateSystemStatus(status);
    }

    const historyResponse = await fetch(HUD_CONFIG.commandEndpoint, { cache: "no-store" });

    if (historyResponse.ok) {
      const history = await historyResponse.json();
      updateCommandHistory(history);
    }
  } catch (error) {
    elements.serverStatus.textContent = "OFFLINE";
    elements.connectionStatus.textContent = "REST WAITING";
  }
}

function handleBackendMessage(rawMessage, source = "unknown") {
  try {
    const payload = JSON.parse(rawMessage);
    receiveCommand({
      source,
      command: payload.command || payload.text || payload.last_command || "Unknown command",
      intent: payload.intent || payload.action || "unknown",
      reply: payload.reply || payload.result || "",
      status: payload.status
    });
  } catch (error) {
    receiveCommand({
      source,
      command: rawMessage,
      intent: "raw_message",
      reply: ""
    });
  }
}

function receiveCommand(payload) {
  const command = payload.command || payload.text || "Unknown command";
  const intent = payload.intent || "command";

  state.lastCommand = command;
  elements.lastCommand.textContent = command;

  addCommandLog(`[${intent}] ${command}`, payload.source || "command");
  setHudMode("processing");

  setTimeout(() => {
    if (isListeningIntent(intent, command)) {
      setHudMode("listening");
    } else {
      setHudMode("standby");
    }
  }, 1400);
}

function updateSystemStatus(status) {
  elements.serverStatus.textContent = status.serverVersionName
    ? `SERVER ${status.serverVersionName}`
    : "ONLINE";

  elements.aiStatus.textContent = status.androidAppVersionName
    ? `APP ${status.androidAppVersionName}`
    : "ONLINE";

  if (typeof status.voiceCommandCount === "number") {
    elements.voiceStatus.textContent = `${status.voiceCommandCount} VOICE`;
  }

  if (status.lastVoiceCommand) {
    elements.lastCommand.textContent = status.lastVoiceCommand;
  }

  if (status.accessibility === true) {
    elements.accessibilityStatus.textContent = "ACTIVE";
  } else if (status.accessibility === false) {
    elements.accessibilityStatus.textContent = "INACTIVE";
  } else {
    elements.accessibilityStatus.textContent = "UNKNOWN";
  }

  elements.connectionStatus.textContent = "REST CONNECTED";
}

function updateCommandHistory(history) {
  if (!history || !Array.isArray(history.items)) return;

  const newest = history.items[0];
  if (!newest) return;

  const signature = `${newest.time}-${newest.text}`;
  if (signature === state.lastCommand) return;

  state.lastCommand = signature;
  elements.lastCommand.textContent = newest.text || "Unknown command";

  addCommandLog(
    `[${newest.intent || "server"}] ${newest.text || "Unknown command"}`,
    "server"
  );
}

function setHudMode(mode) {
  document.body.classList.remove("listening", "processing");

  if (mode === "listening") {
    document.body.classList.add("listening");
    elements.coreMode.textContent = "LISTENING";
    elements.listeningState.textContent = "ACTIVE";
    elements.listeningText.textContent = "Listening for voice command...";
    elements.voiceVisualizer.classList.add("active");
    elements.listeningOrb.classList.add("active");
    elements.processingBars.classList.remove("active");
    elements.processingState.textContent = "READY";
    elements.processingText.textContent = "Neural routing idle.";
    return;
  }

  if (mode === "processing") {
    document.body.classList.add("processing");
    elements.coreMode.textContent = "PROCESSING";
    elements.processingState.textContent = "ACTIVE";
    elements.processingText.textContent = "Analyzing command route...";
    elements.processingBars.classList.add("active");
    elements.voiceVisualizer.classList.remove("active");
    elements.listeningOrb.classList.remove("active");
    elements.listeningState.textContent = "IDLE";
    elements.listeningText.textContent = "Listening module standing by.";
    return;
  }

  elements.coreMode.textContent = "STANDBY";
  elements.listeningState.textContent = "IDLE";
  elements.processingState.textContent = "READY";
  elements.listeningText.textContent = "Listening module standing by.";
  elements.processingText.textContent = "Neural routing idle.";
  elements.voiceVisualizer.classList.remove("active");
  elements.listeningOrb.classList.remove("active");
  elements.processingBars.classList.remove("active");
}

function addCommandLog(message, type = "info") {
  state.commandCount += 1;
  elements.commandCount.textContent = String(state.commandCount);

  const row = document.createElement("div");
  row.className = `log-entry ${type}`;

  const time = document.createElement("span");
  time.className = "log-time";
  time.textContent = new Date().toLocaleTimeString("ro-RO", { hour12: false });

  const text = document.createElement("span");
  text.className = "log-text";
  text.textContent = message;

  row.appendChild(time);
  row.appendChild(text);
  elements.commandLog.prepend(row);

  while (elements.commandLog.children.length > 18) {
    elements.commandLog.removeChild(elements.commandLog.lastChild);
  }
}

function isListeningIntent(intent, command) {
  const value = `${intent} ${command}`.toLowerCase();
  return value.includes("listen") || value.includes("voice") || value.includes("hands free");
}

bootHud();
