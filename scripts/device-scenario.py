#!/usr/bin/env python3
"""Runs the scenario files of plans on a device or emulator, and checks them without one (plan T-046, L-062).

Usage:
  scripts/device-scenario.py check <file.txt> ...                 syntax, testTags and strings; no device needed
  scripts/device-scenario.py run --session <dir> --package <id>   run every scenario file, record the result in <dir>
Exit codes: check 0 ok, 1 a problem was found, 2 usage; run 0 (a failing scenario is recorded, not an exit code),
1 internal error (device-session.sh then writes `plan runner_error`).

A scenario file is a list of steps for the staging app. Elements are read with `uiautomator dump` and chosen by
selectors; a Compose testTag shows as resource-id because the app sets testTagsAsResourceId (plan T-046).

Header (three lines, before any command):
  ticket: T-022
  criterion: what the scenario proves
  timeout: 240              seconds for the whole file; default 180, at most 300

Selectors (one keyword, optional filters; several selectors separated by " | " match any of them):
  id=<testTag>  idp=<testTag prefix>  desc=<content-desc>  text=<text>  str=<name in res/values/strings.xml>
  filters: in=<testTag> (inside that element), index=<n> (nth match, from 1), selected=true, clickable=true

Commands:
  fresh                         clear data, grant runtime permissions, open the app
  consent decline [seconds]     tap "Do not consent" when the ad consent form shows (ADR-0013), or see no form
  wait <selector> [seconds]     wait until it shows (default 20)
  need <selector> [seconds]     like wait; when it never shows the file ends as "bỏ qua (môi trường)"
  tap <selector> | tap_if <selector>   tap the centre of the element; tap_if does nothing when it is absent
  scroll <selector> [times]     swipe up inside the element (default 3)
  expect <selector> | expect_no <selector> | count <selector> <n>
  network off|on                  device rotate landscape|portrait   device night on|off
  device unfold|fold              device locale <tag>                back        shot <name>

Log (<session>/scenarios.txt), per file: `plan <file> ticket=... criterion=...`, one `plan <file> step=<n> <command>
ok|FAIL <reason>|SKIP <reason>` per step, and `plan <file> result=<result> seconds=<n>`. The first failing step also
leaves fail.png and fail.xml in <session>/scenarios/<file without .txt>/.

Deadlines: the whole run has DEVICE_SCENARIOS_BUDGET seconds (default 600); a file has min(its timeout, what is left);
every adb call has DEVICE_SCENARIO_ADB_TIMEOUT seconds (default 30, at most what is left of the file).
Settings: DEVICE_SCENARIOS_GLOB (files to run; default feature/*/src/deviceScenarios/*.txt
app/src/deviceScenarios/*.txt), DEVICE_SCENARIO_POLL (seconds between two dumps, default 1).
Messages are in Vietnamese for the team; comments are in English.
"""

import glob
import os
import re
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
ROOT = SCRIPTS.parent
SESSION_SCRIPT = SCRIPTS / "device-session.sh"
DEFAULT_GLOB = "feature/*/src/deviceScenarios/*.txt app/src/deviceScenarios/*.txt"
MARKER_TAG = "VGL.Session"
DEFAULT_TIMEOUT, MAX_TIMEOUT = 180, 300
CONSENT_DECLINE = "text=Do not consent | text=Không đồng ý"
FIRST_SCREENS = "id=onboarding_screen | id=camera_screen"
UNFOLDED_SIZE, UNFOLDED_DENSITY = "1766x2208", "420"  # 673 x 841 dp, the frame of the _unfolded screenshots
IDLE_ERROR = "could not get idle state"
RESULT_PASSED, RESULT_FAILED, RESULT_ENV = "đạt", "lỗi", "bỏ qua (môi trường)"
RESULT_TIME, RESULT_SYNTAX = "bỏ qua (hết thời gian)", "lỗi tệp"
SELECTOR = re.compile(r"^(id|idp|desc|text|str)=(.+?)((?:\s+(?:in|index|selected|clickable)=\S+)*)\s*$")
FILTER = re.compile(r"(in|index|selected|clickable)=(\S+)")
TRAILING_NUMBER = re.compile(r"^(.*\S)\s+(\d+)$")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]")
LOCALE_TAG = re.compile(r"[A-Za-z]{2,3}(-[A-Za-z0-9]+)*")


class AdbTimeout(Exception):
    """An adb call did not return within its deadline."""


class Fail(Exception):
    """The step failed; the message is the reason in the log."""


class Skip(Exception):
    """The environment lacks what the scenario needs; the file ends as skipped."""


# ---------------------------------------------------------------------------------------------------------------
# Parsing and checking (no device)

def parse_selector(text):
    """A list of alternatives {kind, value, filters}; raises ValueError with the reason."""
    alternatives = []
    for part in re.split(r"\s+\|\s+", text.strip()):
        match = SELECTOR.match(part)
        if not match:
            raise ValueError(f"bộ chọn '{part}' sai cú pháp (id=, idp=, desc=, text= hay str=, rồi in=, index=, "
                             "selected=true, clickable=true)")
        filters = dict(FILTER.findall(match.group(3)))
        if "index" in filters and not (filters["index"].isdigit() and int(filters["index"]) >= 1):
            raise ValueError(f"index={filters['index']} phải là số từ 1")
        for key in ("selected", "clickable"):
            if key in filters and filters[key] != "true":
                raise ValueError(f"{key}= chỉ nhận true")
        alternatives.append({"kind": match.group(1), "value": match.group(2).strip(), "filters": filters})
    return alternatives


def split_number(rest, required=False):
    """(selector text, n): n is the integer after the selector, or None."""
    match = TRAILING_NUMBER.match(rest)
    if match and "=" in match.group(1):
        return match.group(1), int(match.group(2))
    if required:
        raise ValueError("thiếu số ở cuối")
    return rest, None


def parse_command(command, rest):
    """Validates one command; returns its arguments or raises ValueError."""
    if command in ("fresh", "back"):
        if rest:
            raise ValueError(f"{command} không có tham số")
        return {}
    if command == "consent":
        words = rest.split()
        if not words or words[0] != "decline" or len(words) > 2 or (len(words) == 2 and not words[1].isdigit()):
            raise ValueError("cú pháp: consent decline [giây]")
        return {"seconds": int(words[1]) if len(words) == 2 else 20}
    if command in ("wait", "need"):
        text, seconds = split_number(rest)
        return {"selector": parse_selector(text), "seconds": 20 if seconds is None else seconds}
    if command in ("tap", "tap_if", "expect", "expect_no"):
        return {"selector": parse_selector(rest)}
    if command == "scroll":
        text, times = split_number(rest)
        return {"selector": parse_selector(text), "times": 3 if times is None else times}
    if command == "count":
        text, number = split_number(rest, required=True)
        return {"selector": parse_selector(text), "number": number}
    if command == "network":
        if rest not in ("off", "on"):
            raise ValueError("cú pháp: network off|on")
        return {"state": rest}
    if command == "device":
        words = rest.split()
        valid = ((words[:1] == ["rotate"] and words[1:] in (["landscape"], ["portrait"]))
                 or (words[:1] == ["night"] and words[1:] in (["on"], ["off"]))
                 or words in (["unfold"], ["fold"])
                 or (words[:1] == ["locale"] and len(words) == 2 and LOCALE_TAG.fullmatch(words[1])))
        if not valid:
            raise ValueError("cú pháp: device rotate landscape|portrait, night on|off, unfold, fold, locale <thẻ>")
        return {"words": words}
    if command == "shot":
        if not re.fullmatch(r"[A-Za-z0-9_.-]+", rest):
            raise ValueError("shot cần một tên gồm chữ, số, gạch dưới, gạch ngang")
        return {"name": rest}
    raise ValueError(f"lệnh lạ '{command}'")


def parse_file(path):
    """(header, steps, errors): steps are (line number, command, rest, arguments); errors are (line number, reason)."""
    header, steps, errors = {}, [], []
    for number, raw in enumerate(Path(path).read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        field = re.match(r"^(ticket|criterion|timeout):\s*(.*)$", line)
        if field:
            if steps:
                errors.append((number, f"dòng '{field.group(1)}:' phải ở phần đầu, trước mọi lệnh"))
            header[field.group(1)] = field.group(2).strip()
            continue
        command, _, rest = line.partition(" ")
        try:
            steps.append((number, command, rest.strip(), parse_command(command, rest.strip())))
        except ValueError as problem:
            errors.append((number, str(problem)))
    if not re.fullmatch(r"T-\d+", header.get("ticket", "")):
        errors.append((1, "thiếu dòng 'ticket: T-NNN' ở phần đầu"))
    if not header.get("criterion"):
        errors.append((1, "thiếu dòng 'criterion:' ở phần đầu"))
    timeout = header.get("timeout", str(DEFAULT_TIMEOUT))
    if not (timeout.isdigit() and 1 <= int(timeout) <= MAX_TIMEOUT):
        errors.append((1, f"timeout phải từ 1 đến {MAX_TIMEOUT} giây"))
    if not steps and not errors:
        errors.append((1, "tệp không có lệnh nào"))
    return header, steps, errors


def source_files(root, name):
    """Files called `name` (or matching a glob) under app/, feature/ and core/, outside build folders."""
    for folder in ("app", "feature", "core"):
        base = root / folder
        for path in sorted(base.rglob(name)) if base.is_dir() else []:
            if "build" not in path.relative_to(root).parts:
                yield path


def kotlin_text(root):
    return "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in source_files(root, "*.kt"))


def unescape_android(value):
    return re.sub(r"\\(.)", lambda m: "\n" if m.group(1) == "n" else m.group(1), value)


def string_values(root):
    """name -> values, from the res/values/strings.xml of every module (the default language only)."""
    values = {}
    for path in source_files(root, "strings.xml"):
        if path.parent.name != "values":
            continue
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        for element in tree.getroot().iter("string"):
            values.setdefault(element.get("name", ""), []).append(unescape_android("".join(element.itertext())).strip())
    return values


def check_files(paths, root=ROOT):
    """Messages '<file> dòng <n>: <reason>' for every problem of the files; empty when they are fine."""
    messages = []
    cache = {}

    def kotlin():
        cache.setdefault("kotlin", kotlin_text(root))
        return cache["kotlin"]

    def strings():
        cache.setdefault("strings", string_values(root))
        return cache["strings"]

    for path in paths:
        try:
            _, steps, errors = parse_file(path)
        except (OSError, UnicodeDecodeError) as problem:
            messages.append(f"{path} dòng 0: không đọc được tệp ({problem})")
            continue
        messages += [f"{path} dòng {number}: {reason}" for number, reason in errors]
        for number, _, _, arguments in steps:
            for alternative in arguments.get("selector", []):
                kind, value = alternative["kind"], alternative["value"]
                if kind == "id" and f'"{value}"' not in kotlin():
                    messages.append(f"{path} dòng {number}: id={value} không có trong mã nguồn (*.kt dưới app/, feature/, "
                                    "core/); đổi tên testTag thì sửa kịch bản.")
                if kind == "idp" and f'"{value}' not in kotlin():
                    messages.append(f"{path} dòng {number}: idp={value} không là tiền tố của testTag nào trong mã nguồn.")
                if kind == "str" and value not in strings():
                    messages.append(f"{path} dòng {number}: str={value} không có trong res/values/strings.xml của module nào.")
                container = alternative["filters"].get("in")
                if container and f'"{container}"' not in kotlin():
                    messages.append(f"{path} dòng {number}: in={container} không có trong mã nguồn.")
    return messages


# ---------------------------------------------------------------------------------------------------------------
# Matching the elements of a dump

def bounds_of(node):
    numbers = [int(v) for v in re.findall(r"\d+", node.get("bounds", ""))]
    return numbers if len(numbers) == 4 else [0, 0, 0, 0]


def centre(node):
    x1, y1, x2, y2 = bounds_of(node)
    return (x1 + x2) // 2, (y1 + y2) // 2


class Screen:
    """One parsed dump."""

    def __init__(self, root, strings):
        self.nodes = list(root.iter("node"))
        self.strings = strings

    @staticmethod
    def tag(node):
        return node.get("resource-id", "").split("/")[-1]

    def string_matches(self, name, node):
        for value in self.strings.get(name, []):
            pattern = ".*".join(re.escape(part) for part in PLACEHOLDER.split(value))
            for actual in (node.get("text", ""), node.get("content-desc", "")):
                if actual and re.fullmatch(pattern, actual.strip(), re.S):
                    return True
        return False

    def matches(self, node, alternative):
        kind, value, filters = alternative["kind"], alternative["value"], alternative["filters"]
        if kind == "id":
            ok = self.tag(node) == value
        elif kind == "idp":
            ok = self.tag(node).startswith(value)
        elif kind == "desc":
            ok = node.get("content-desc", "") == value
        elif kind == "text":
            ok = node.get("text", "") == value
        else:
            ok = self.string_matches(value, node)
        if ok and filters.get("selected") == "true":
            ok = node.get("selected") == "true" or node.get("checked") == "true"
        if ok and filters.get("clickable") == "true":
            ok = node.get("clickable") == "true"
        return ok

    def inside(self, tag):
        """Nodes inside an element that has this testTag, in document order."""
        inner = set()
        for node in self.nodes:
            if self.tag(node) == tag:
                inner.update(id(child) for child in node.iter("node") if child is not node)
        return [node for node in self.nodes if id(node) in inner]

    def find_all(self, alternatives):
        found = []
        for alternative in alternatives:
            pool = self.inside(alternative["filters"]["in"]) if "in" in alternative["filters"] else self.nodes
            matched = [node for node in pool if self.matches(node, alternative)]
            if "index" in alternative["filters"]:
                position = int(alternative["filters"]["index"])
                matched = matched[position - 1:position]
            found += matched
        return found

    def find(self, alternatives):
        found = self.find_all(alternatives)
        return found[0] if found else None


# ---------------------------------------------------------------------------------------------------------------
# The device

def run_process(command, timeout, binary=False, env=None):
    """(exit code, output). At the deadline the whole process group is killed, so no child keeps the pipe open."""
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL if binary else subprocess.STDOUT,
                               start_new_session=True, env=env)
    try:
        output, _ = process.communicate(timeout=timeout)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.communicate()
        raise AdbTimeout(" ".join(command[:4])) from None
    return process.returncode, (output if binary else output.decode("utf-8", errors="replace"))


class Runner:
    def __init__(self, session, package):
        self.session = Path(session)
        self.package = package
        self.started = time.monotonic()
        self.budget = float(os.environ.get("DEVICE_SCENARIOS_BUDGET", "600"))
        self.adb_timeout = float(os.environ.get("DEVICE_SCENARIO_ADB_TIMEOUT", "30"))
        self.poll = float(os.environ.get("DEVICE_SCENARIO_POLL", "1"))
        self.log_path = self.session / "scenarios.txt"
        self.file_deadline = 0.0
        self.file_dir = self.session
        self.last_xml = None
        self.activity = None
        self.strings = string_values(ROOT)
        self.original_size = self.original_density = None
        self.network_touched = False

    def log(self, line):
        with open(self.log_path, "a", encoding="utf-8") as handle:
            handle.write(line.replace("\n", " ") + "\n")

    def left(self):
        return self.file_deadline - time.monotonic()

    def budget_left(self):
        return self.budget - (time.monotonic() - self.started)

    # -- adb
    def adb(self, *args, binary=False, timeout=None):
        """adb within the deadline of the step: the adb limit, or what is left of the file."""
        limit = min(self.adb_timeout, self.left()) if timeout is None else timeout
        if limit <= 0:
            raise Fail("hết hạn của tệp")
        return run_process(["adb", *args], limit, binary=binary)

    def helper(self, name):
        """Exit code of a `device-session.sh helper` function, so there is one way to cut the network and grant."""
        limit = min(max(self.adb_timeout, 45.0), max(self.left(), 1.0))
        return run_process([str(SESSION_SCRIPT), "helper", name], limit, env=dict(os.environ, APP_PACKAGE=self.package))[0]

    def dump(self):
        """(screen, None) or (None, reason)."""
        code, output = self.adb("shell", "uiautomator", "dump", "/sdcard/ds.xml")
        if IDLE_ERROR in output:
            return None, IDLE_ERROR
        if code != 0 or "ERROR" in output:
            return None, (output.strip().splitlines() or ["uiautomator dump lỗi"])[0]
        _, text = self.adb("exec-out", "cat", "/sdcard/ds.xml")
        if not text.strip():
            return None, "dump rỗng"
        try:
            root = ET.fromstring(text)
        except ET.ParseError:
            return None, "dump không đọc được"
        self.last_xml = text
        return Screen(root, self.strings), None

    def dump_ready(self, seconds=5.0):
        """A dump, tried again while the app is not idle (an endless animation); Fail with the reason otherwise."""
        end = min(time.monotonic() + seconds, self.file_deadline)
        while True:
            screen, reason = self.dump()
            if screen is not None:
                return screen
            if reason != IDLE_ERROR or time.monotonic() >= end:
                raise Fail(reason)
            time.sleep(self.poll)

    def wait_for(self, alternatives, seconds):
        """The first matching node; Fail with the reason of the last dump when it never shows."""
        end = min(time.monotonic() + seconds, self.file_deadline)
        while True:
            screen, reason = self.dump()
            if screen is not None:
                node = screen.find(alternatives)
                if node is not None:
                    return node
            if time.monotonic() >= end:
                raise Fail(reason or "không thấy phần tử")
            time.sleep(min(self.poll, max(0.0, end - time.monotonic())))

    def tap_node(self, node):
        x, y = centre(node)
        self.adb("shell", "input", "tap", str(x), str(y))

    # -- commands
    def step_fresh(self, arguments):
        self.adb("shell", "pm", "clear", self.package)
        if self.helper("grant-permissions") != 0:
            raise Fail("không cấp được quyền của app")
        if self.activity is None:
            _, output = self.adb("shell", "cmd", "package", "resolve-activity", "--brief", "-c",
                                 "android.intent.category.LAUNCHER", self.package)
            lines = output.replace("\r", "").strip().splitlines()
            self.activity = lines[-1].strip() if lines else ""
        if "/" not in self.activity:
            raise Fail(f"không tìm thấy màn hình khởi động của {self.package}")
        self.adb("shell", "am", "start", "-n", self.activity)

    def step_consent(self, arguments):
        decline, first = parse_selector(CONSENT_DECLINE), parse_selector(FIRST_SCREENS)
        end = min(time.monotonic() + arguments["seconds"], self.file_deadline)
        while True:
            screen, _ = self.dump()
            if screen is not None:
                node = screen.find(decline)
                if node is not None:
                    self.tap_node(node)
                    return
                if screen.find(first) is not None:
                    return  # no consent form on this start
            if time.monotonic() >= end:
                raise Fail("không thấy hộp thoại đồng ý hay màn hình đầu của app")
            time.sleep(min(self.poll, max(0.0, end - time.monotonic())))

    def step_wait(self, arguments):
        self.wait_for(arguments["selector"], arguments["seconds"])

    def step_need(self, arguments):
        try:
            self.wait_for(arguments["selector"], arguments["seconds"])
        except Fail as problem:
            raise Skip(f"thiếu điều kiện của môi trường ({problem})") from None

    def step_tap(self, arguments):
        node = self.dump_ready().find(arguments["selector"])
        if node is None:
            raise Fail("không thấy phần tử")
        self.tap_node(node)

    def step_tap_if(self, arguments):
        node = self.dump_ready().find(arguments["selector"])
        if node is not None:
            self.tap_node(node)

    def step_scroll(self, arguments):
        node = self.dump_ready().find(arguments["selector"])
        if node is None:
            raise Fail("không thấy phần tử")
        x1, y1, x2, y2 = bounds_of(node)
        x = (x1 + x2) // 2
        start, end = y1 + (y2 - y1) * 4 // 5, y1 + (y2 - y1) // 5
        for _ in range(arguments["times"]):
            self.adb("shell", "input", "swipe", str(x), str(start), str(x), str(end), "300")

    def step_expect(self, arguments):
        if self.dump_ready().find(arguments["selector"]) is None:
            raise Fail("không thấy phần tử")

    def step_expect_no(self, arguments):
        if self.dump_ready().find(arguments["selector"]) is not None:
            raise Fail("phần tử vẫn còn")

    def step_count(self, arguments):
        found = len(self.dump_ready().find_all(arguments["selector"]))
        if found != arguments["number"]:
            raise Fail(f"có {found} phần tử, cần {arguments['number']}")

    def step_network(self, arguments):
        self.network_touched = True
        if arguments["state"] == "on":
            self.helper("network-on")
        elif self.helper("network-off") != 0:
            raise Skip("không tắt được mạng (máy vẫn có mạng mặc định)")

    def step_device(self, arguments):
        words = arguments["words"]
        if words[0] == "rotate":
            self.adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
            self.adb("shell", "settings", "put", "system", "user_rotation", "1" if words[1] == "landscape" else "0")
        elif words[0] == "night":
            self.adb("shell", "cmd", "uimode", "night", "yes" if words[1] == "on" else "no")
        elif words[0] == "unfold":
            self.adb("shell", "wm", "size", UNFOLDED_SIZE)
            self.adb("shell", "wm", "density", UNFOLDED_DENSITY)
        elif words[0] == "fold":
            self.adb("shell", "wm", "size", self.original_size or "reset")
            self.adb("shell", "wm", "density", self.original_density or "reset")
        else:
            self.adb("shell", "cmd", "locale", "set-app-locales", self.package, "--locales", words[1])

    def step_back(self, arguments):
        self.adb("shell", "input", "keyevent", "4")

    def step_shot(self, arguments):
        self.save_screenshot(arguments["name"] + ".png")

    def save_screenshot(self, name, timeout=None):
        code, data = self.adb("exec-out", "screencap", "-p", binary=True, timeout=timeout)
        if code != 0:
            raise Fail("chụp màn hình lỗi")
        self.file_dir.mkdir(parents=True, exist_ok=True)
        (self.file_dir / name).write_bytes(data)

    # -- device state
    def read_display(self):
        """Remembers an overridden size or density the device had, so a scenario gives it back."""
        for what in ("size", "density"):
            try:
                _, output = run_process(["adb", "shell", "wm", what], 10)
            except AdbTimeout:
                continue
            override = re.search(r"Override (?:size|density): *(\S+)", output)
            if override:
                setattr(self, f"original_{what}", override.group(1))

    def reset_device(self, network=False):
        """Gives the device back as the scenario found it. Each call has its own short limit and may fail alone."""
        commands = [
            ["shell", "wm", "size", self.original_size or "reset"],
            ["shell", "wm", "density", self.original_density or "reset"],
            ["shell", "settings", "put", "system", "accelerometer_rotation", "1"],
            ["shell", "settings", "put", "system", "user_rotation", "0"],
            ["shell", "cmd", "uimode", "night", "no"],
            ["shell", "cmd", "locale", "set-app-locales", self.package],  # without --locales: back to the system language
        ]
        for command in commands:
            try:
                run_process(["adb", *command], 10)
            except AdbTimeout:
                pass
        if network:
            try:
                run_process([str(SESSION_SCRIPT), "helper", "network-on"], 45, env=dict(os.environ, APP_PACKAGE=self.package))
            except AdbTimeout:
                pass

    # -- files
    def run_file(self, path):
        name = Path(path).name
        self.file_dir = self.session / "scenarios" / Path(path).stem
        try:
            header, steps, errors = parse_file(path)
        except (OSError, UnicodeDecodeError) as problem:
            self.log(f"plan {name} step=0 check FAIL không đọc được tệp ({problem})")
            self.log(f"plan {name} result={RESULT_SYNTAX} seconds=0")
            return
        self.log(f"plan {name} ticket={header.get('ticket', '?')} criterion={header.get('criterion', '?')}")
        if errors:
            number, reason = errors[0]
            self.log(f"plan {name} step=0 check FAIL dòng {number}: {reason}")
            self.log(f"plan {name} result={RESULT_SYNTAX} seconds=0")
            return
        if self.budget_left() <= 0:
            self.log(f"plan {name} result={RESULT_TIME} seconds=0")
            return
        began = time.monotonic()
        self.file_deadline = began + min(int(header.get("timeout", DEFAULT_TIMEOUT)), self.budget_left())
        self.last_xml = None
        self.network_touched = False
        result = RESULT_PASSED
        try:
            self.adb("shell", "log", "-t", MARKER_TAG, f"'scenario=plan file={name}'", timeout=10)
        except AdbTimeout:
            pass
        for index, (_, command, rest, arguments) in enumerate(steps, start=1):
            text = f"plan {name} step={index} {command}{' ' + rest if rest else ''}"
            try:
                getattr(self, "step_" + command)(arguments)
                self.log(f"{text} ok")
            except Skip as skipped:
                self.log(f"{text} SKIP {skipped}")
                result = RESULT_ENV
                break
            except (AdbTimeout, Fail) as failed:
                self.log(f"{text} FAIL {'adb_timeout' if isinstance(failed, AdbTimeout) else failed}")
                self.keep_evidence()
                result = RESULT_FAILED
                break
        self.log(f"plan {name} result={result} seconds={int(round(time.monotonic() - began))}")
        self.reset_device(network=self.network_touched)

    def keep_evidence(self):
        """fail.xml (the last dump) and fail.png of the first failing step."""
        if self.last_xml:
            self.file_dir.mkdir(parents=True, exist_ok=True)
            (self.file_dir / "fail.xml").write_text(self.last_xml, encoding="utf-8")
        try:
            self.save_screenshot("fail.png", timeout=10)
        except (AdbTimeout, Fail, OSError):
            pass

    def run(self, files):
        self.session.mkdir(parents=True, exist_ok=True)
        self.read_display()
        for path in files:
            self.run_file(path)
        self.reset_device(network=True)
        print(f"Kịch bản theo plan: {len(files)} tệp, kết quả trong {self.log_path}")


def scenario_files():
    files = []
    for pattern in os.environ.get("DEVICE_SCENARIOS_GLOB", DEFAULT_GLOB).split():
        files += glob.glob(pattern if os.path.isabs(pattern) else str(ROOT / pattern))
    return sorted(dict.fromkeys(files))


def main(argv):
    if len(argv) >= 3 and argv[1] == "check":
        messages = check_files(argv[2:])
        for message in messages:
            print(message)
        return 1 if messages else 0
    if len(argv) == 6 and argv[1] == "run" and argv[2] == "--session" and argv[4] == "--package":
        files = scenario_files()
        if files:
            Runner(argv[3], argv[5]).run(files)
        return 0
    print("Cách dùng: scripts/device-scenario.py check <tệp.txt>... | run --session <thư mục> --package <gói>",
          file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
