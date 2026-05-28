import json
import atexit
import os
import re
import signal
import subprocess
import sys
import threading
import time
import uuid
from pathlib import Path

try:
    import webview
except ImportError:
    print("pywebview is not installed.")
    print("Install it with: py -m pip install pywebview")
    raise


ROOT = Path(__file__).resolve().parent
GRADLE_PROPERTIES = ROOT / "gradle.properties"
GRADLEW = ROOT / "gradlew.bat"
PLAYER_COUNT = 4


def split_player(value):
    value = value.strip()
    if not value:
        return {"name": "", "uuid": ""}
    if "," in value:
        name, player_uuid = value.split(",", 1)
        return {"name": name.strip(), "uuid": player_uuid.strip()}
    return {"name": value, "uuid": ""}


def format_player(name, player_uuid):
    name = name.strip()
    player_uuid = player_uuid.strip()
    if not name:
        return ""
    if player_uuid:
        return f"{name}, {player_uuid}"
    return name


class LauncherApi:
    def __init__(self):
        self.processes = []
        self.log_lines = []
        self.lock = threading.Lock()
        self.cancel_launch = threading.Event()

    def load_players(self):
        values = {f"local_player_{i}": "" for i in range(1, PLAYER_COUNT + 1)}
        if GRADLE_PROPERTIES.exists():
            for line in GRADLE_PROPERTIES.read_text(encoding="utf-8").splitlines():
                match = re.match(r"^\s*#?\s*(local_player_[1-4])\s*=\s*(.*)\s*$", line)
                if match:
                    values[match.group(1)] = match.group(2).strip()

        players = []
        for index in range(1, PLAYER_COUNT + 1):
            parsed = split_player(values[f"local_player_{index}"])
            players.append({"index": index, **parsed})
        return {"players": players, "log": self._log_text()}

    def save_players(self, players):
        normalized = {}
        for player in players:
            index = int(player.get("index", 0))
            if 1 <= index <= PLAYER_COUNT:
                normalized[f"local_player_{index}"] = format_player(player.get("name", ""), player.get("uuid", ""))

        existing = GRADLE_PROPERTIES.read_text(encoding="utf-8").splitlines() if GRADLE_PROPERTIES.exists() else []
        seen = set()
        output = []

        for line in existing:
            match = re.match(r"^\s*#?\s*(local_player_[1-4])\s*=.*$", line)
            if match:
                key = match.group(1)
                output.append(f"{key}={normalized.get(key, '')}")
                seen.add(key)
            else:
                output.append(line)

        if output and output[-1].strip():
            output.append("")

        for index in range(1, PLAYER_COUNT + 1):
            key = f"local_player_{index}"
            if key not in seen:
                output.append(f"{key}={normalized.get(key, '')}")

        GRADLE_PROPERTIES.write_text("\n".join(output) + "\n", encoding="utf-8")
        self._log("Saved gradle.properties")
        return {"ok": True, "log": self._log_text()}

    def generate_uuid(self):
        return str(uuid.uuid4())

    def launch(self, players, delay_seconds=4):
        self.save_players(players)
        selected = [int(player["index"]) for player in players if player.get("enabled") and player.get("name", "").strip()]
        if not selected:
            return {"ok": False, "message": "Select at least one player with a nickname.", "log": self._log_text()}

        self.cancel_launch.clear()
        thread = threading.Thread(target=self._launch_many, args=(selected, max(0, int(delay_seconds))), daemon=True)
        thread.start()
        return {"ok": True, "message": f"Launching {len(selected)} client(s): {selected}", "log": self._log_text()}

    def stop_tracked(self):
        stopped = self.stop_all_processes()
        self._log(f"Stop requested for {stopped} tracked process(es).")
        return {"ok": True, "log": self._log_text()}

    def get_log(self):
        return self._log_text()

    def stop_all_processes(self):
        self.cancel_launch.set()
        stopped = 0
        with self.lock:
            processes = list(self.processes)

        for process in processes:
            if process.poll() is None:
                if self._stop_process_tree(process):
                    stopped += 1

        with self.lock:
            self.processes = [process for process in self.processes if process.poll() is None]
        return stopped

    def _launch_many(self, indexes, delay_seconds):
        for position, index in enumerate(indexes):
            if self.cancel_launch.is_set():
                self._log("Launch queue cancelled.")
                return
            self._launch_one(index)
            if position < len(indexes) - 1 and delay_seconds > 0:
                if self.cancel_launch.wait(delay_seconds):
                    self._log("Launch queue cancelled.")
                    return

    def _launch_one(self, index):
        command = [str(GRADLEW), "--no-daemon", "runClient", f"-Plocal_player_index={index}"]
        self._log("Starting: " + " ".join(command))
        creationflags = subprocess.CREATE_NEW_CONSOLE if os.name == "nt" else 0
        process = subprocess.Popen(command, cwd=ROOT, creationflags=creationflags)
        with self.lock:
            self.processes.append(process)

    def _stop_process_tree(self, process):
        try:
            if os.name == "nt":
                subprocess.run(
                    ["taskkill", "/PID", str(process.pid), "/T", "/F"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    check=False,
                    creationflags=subprocess.CREATE_NO_WINDOW,
                )
            else:
                process.terminate()

            try:
                process.wait(timeout=8)
            except subprocess.TimeoutExpired:
                process.kill()
            return True
        except Exception as exception:
            self._log(f"Failed to stop process {process.pid}: {exception}")
            return False

    def _log(self, message):
        with self.lock:
            stamp = time.strftime("%H:%M:%S")
            self.log_lines.append(f"[{stamp}] {message}")
            self.log_lines = self.log_lines[-200:]

    def _log_text(self):
        with self.lock:
            return "\n".join(self.log_lines)


HTML = r"""
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <title>Codenames Minecraft Launcher</title>
  <style>
    :root {
      color-scheme: dark;
      --bg: #101820;
      --panel: #172431;
      --panel-2: #203244;
      --text: #e8f0f5;
      --muted: #99aebd;
      --accent: #f4b942;
      --green: #61d394;
      --red: #ef626c;
      --line: #345064;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Segoe UI", sans-serif;
      background: radial-gradient(circle at top left, #28465c, var(--bg) 46%);
      color: var(--text);
    }
    main {
      width: 900px;
      max-width: 100vw;
      margin: 0 auto;
      padding: 24px;
    }
    h1 {
      margin: 0 0 6px;
      color: var(--accent);
      font-size: 28px;
    }
    p {
      margin: 0 0 18px;
      color: var(--muted);
    }
    .panel {
      background: linear-gradient(180deg, rgba(255,255,255,.04), rgba(255,255,255,.01)), var(--panel);
      border: 1px solid var(--line);
      border-radius: 18px;
      padding: 16px;
      box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .grid {
      display: grid;
      grid-template-columns: 46px 1fr 2fr 116px;
      gap: 10px;
      align-items: center;
    }
    .head {
      color: var(--muted);
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: .08em;
      padding-bottom: 6px;
    }
    input[type="text"], input[type="number"] {
      width: 100%;
      background: var(--panel-2);
      border: 1px solid var(--line);
      border-radius: 10px;
      color: var(--text);
      padding: 10px 11px;
      outline: none;
    }
    input[type="text"]:focus, input[type="number"]:focus {
      border-color: var(--accent);
    }
    input[type="checkbox"] {
      width: 20px;
      height: 20px;
      accent-color: var(--accent);
    }
    button {
      border: 0;
      border-radius: 12px;
      background: var(--accent);
      color: #1b1b1b;
      padding: 11px 14px;
      font-weight: 700;
      cursor: pointer;
    }
    button.secondary {
      background: var(--panel-2);
      color: var(--text);
      border: 1px solid var(--line);
    }
    button.danger {
      background: var(--red);
      color: white;
    }
    .actions {
      display: flex;
      gap: 10px;
      align-items: center;
      margin-top: 16px;
      flex-wrap: wrap;
    }
    .actions label {
      color: var(--muted);
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .actions input {
      width: 70px;
    }
    pre {
      min-height: 120px;
      max-height: 220px;
      overflow: auto;
      margin: 16px 0 0;
      padding: 12px;
      background: #071018;
      border: 1px solid var(--line);
      border-radius: 14px;
      color: #b9d7e8;
      white-space: pre-wrap;
    }
  </style>
</head>
<body>
<main>
  <h1>Codenames Minecraft Launcher</h1>
  <p>Настрой игроков и запусти несколько dev-клиентов Minecraft одной кнопкой.</p>

  <section class="panel">
    <div class="grid head">
      <div>run</div>
      <div>Ник</div>
      <div>UUID, можно пустым</div>
      <div></div>
    </div>
    <div id="players"></div>
    <div class="actions">
      <button onclick="launch()">Запустить выбранных</button>
      <button class="secondary" onclick="save()">Сохранить</button>
      <button class="danger" onclick="stopTracked()">Остановить процессы</button>
      <label>Задержка между окнами <input id="delay" type="number" min="0" max="60" value="4"> сек.</label>
    </div>
    <pre id="log"></pre>
  </section>
</main>

<script>
let players = [];

function row(player) {
  return `
    <div class="grid" style="margin-bottom: 10px">
      <input type="checkbox" id="enabled_${player.index}" ${player.name ? "checked" : ""}>
      <input type="text" id="name_${player.index}" value="${escapeHtml(player.name)}" placeholder="Player${player.index}">
      <input type="text" id="uuid_${player.index}" value="${escapeHtml(player.uuid)}" placeholder="optional">
      <button class="secondary" onclick="fillUuid(${player.index})">UUID</button>
    </div>
  `;
}

function escapeHtml(value) {
  return String(value || "").replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function collect() {
  return [1, 2, 3, 4].map(index => ({
    index,
    enabled: document.getElementById(`enabled_${index}`).checked,
    name: document.getElementById(`name_${index}`).value.trim(),
    uuid: document.getElementById(`uuid_${index}`).value.trim()
  }));
}

async function load() {
  const data = await pywebview.api.load_players();
  players = data.players;
  document.getElementById("players").innerHTML = players.map(row).join("");
  document.getElementById("log").textContent = data.log || "Ready.";
}

async function fillUuid(index) {
  document.getElementById(`uuid_${index}`).value = await pywebview.api.generate_uuid();
}

async function save() {
  const result = await pywebview.api.save_players(collect());
  document.getElementById("log").textContent = result.log;
}

async function launch() {
  const delay = document.getElementById("delay").value || 4;
  const result = await pywebview.api.launch(collect(), delay);
  document.getElementById("log").textContent = (result.message ? result.message + "\n" : "") + result.log;
}

async function stopTracked() {
  const result = await pywebview.api.stop_tracked();
  document.getElementById("log").textContent = result.log;
}

setInterval(async () => {
  if (window.pywebview && pywebview.api) {
    document.getElementById("log").textContent = await pywebview.api.get_log();
  }
}, 1500);

window.addEventListener("pywebviewready", load);
</script>
</body>
</html>
"""


def main():
    api = LauncherApi()
    atexit.register(api.stop_all_processes)

    def stop_children(*_args):
        api.stop_all_processes()

    signal.signal(signal.SIGINT, stop_children)
    signal.signal(signal.SIGTERM, stop_children)

    webview.create_window("Codenames Minecraft Launcher", html=HTML, js_api=api, width=940, height=680)
    try:
        webview.start(debug=False)
    finally:
        api.stop_all_processes()


if __name__ == "__main__":
    main()
