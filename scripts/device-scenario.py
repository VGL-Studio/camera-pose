#!/usr/bin/env python3
"""Runs the scenario files of plans on a device or emulator, and checks them without one (L-085).

Usage:
  scripts/device-scenario.py check <file.txt> ...                 syntax, testTags and strings; no device needed
  scripts/device-scenario.py run --session <dir> --package <id>   run every scenario file, record the result in <dir>
Exit codes: check 0 ok, 1 a problem was found, 2 usage; run 0 (a failing scenario is recorded, not an exit code),
1 internal error (device-session.sh then writes `plan runner_error`).

A scenario file is a list of steps for the staging app. Elements are read with `uiautomator dump` and chosen by
selectors; a Compose testTag shows as resource-id because the app sets testTagsAsResourceId
(Modifier.exposeTestTagsForUiAutomator() at the root of MainActivity and of each dialog window).

Header (three lines, before any command):
  ticket: T-NNN
  criterion: what the scenario proves
  timeout: 240              seconds for the whole file; default 180, at most 300

The file name is letters, digits, '.', '_' or '-' and ends in .txt; a name that starts with T-<n>- has the same
ticket: line. A file that breaks a rule is a file error: check exits 1, run records it and sends it no adb command.

Selectors (one keyword, optional filters; several selectors separated by " | " match any of them):
  id=<testTag>  idp=<testTag prefix>  desc=<content-desc>  text=<text>  str=<name in res/values/strings.xml>
  A value may be quoted ("Hands in Pockets"): the quotes keep its spaces, a number after them is the seconds or count.
  An unquoted text= or desc= on a line that ends in a number is ambiguous, even one word: quote it (desc="Back" 10).
  filters: in=<testTag> (inside that element), index=<n> (nth match, from 1), selected=true, clickable=true
  selected=true (or checked) looks at the node itself; for a node that is not clickable also at up to two parents,
  stopping after the first clickable one (the text of a Compose chip is a child of the selected node).

Commands:
  fresh                         clear data, grant runtime permissions, set the language of the app to en (Android
                                13 and later; below 13 a device in another language skips the file), open the app
                                with am start -W and wait for its process (pidof); without one, fail when the crash
                                buffer names the app, otherwise start it once more ("mở lại" in the note); after the
                                device was given back or changed by a device command it waits
                                DEVICE_SCENARIO_STOP_SETTLE seconds (default 3) after stopping the app; before
                                opening the app it closes the system shade (cmd statusbar collapse), which monkey
                                may leave open
  consent decline [seconds]     tap "Do not consent" when the ad consent form shows (ADR-0013) and wait until it
                                closes, or see the app root (DEVICE_SCENARIO_FIRST_SCREENS) without a form; consent
                                is gathered asynchronously, so after the root it keeps looking for a late form for
                                DEVICE_SCENARIO_CONSENT_GRACE seconds (default 8). When the form shows even later, a
                                step that reads the screen declines it once ("hiện muộn" in the note); a form that
                                shows again after a decline is a failure, also inside need (the choice was not kept);
                                a second consent decline that sees the form after a decline fails
  wait <selector> [seconds]     wait until it shows (default 20)
  need <selector> [seconds]     like wait; when it never shows the file ends as "bỏ qua (môi trường)"
  tap <selector> | tap_if <selector>   tap the centre of the element; tap_if does nothing when it is absent
  scroll <selector> [times]     swipe up inside the element (default 3)
  expect <selector> | expect_no <selector> | count <selector> <n>
  network off|on                  device rotate landscape|portrait   device night on|off
  device unfold|fold              device locale <tag>                back        shot <name>
  After a device command the runner reads the screen until two dumps agree (DEVICE_SCENARIO_SETTLE seconds, default
  10); a screen that does not settle is noted, not failed. The last dump of consent decline and of a device command
  is used by the next step only when that step reads the screen.

Log (<session>/scenarios.txt), per file: `plan <file> ticket=... criterion=...`, one `plan <file> step=<n> <command>
ok|FAIL <reason>|SKIP <reason>|RETRY <reason>` per step, each after a `plan <file> note step=<n> t=<seconds from the start of the
file> <details>` line (a tap names its point and element), and `plan <file> result=<result> seconds=<n>`. The first
failing step also leaves fail.png and fail.xml in <session>/scenarios/<file without .txt>/.
A step that fails while logcat.txt of the session shows the system killing the app with a dying process (`Killing
<pid>:<package>/... depends on provider ... in dying proc ...`, after the marker of the file) is RETRY: the device is
given back and the file runs once more, its result is the one of the second run and the first evidence is fail-1.png
and fail-1.xml; killed again, or without time for a second run, the step is SKIP and the file skipped
(FB-20261025-03, plan T-054 of the app).

Deadlines: the whole run has DEVICE_SCENARIOS_BUDGET seconds (default 1800); a file has min(its timeout, what is left
- DEVICE_SCENARIO_RESET_LIMIT - DEVICE_SCENARIO_EVIDENCE_RESERVE (default 20, for the log marker and fail.png));
every adb call has DEVICE_SCENARIO_ADB_TIMEOUT seconds (default 30, at most what is left of the file). Giving the
device back takes at most DEVICE_SCENARIO_RESET_LIMIT seconds (default 60, turning the network on included); a file
ends that much before the budget so its reset fits in it. SIGTERM kills the running adb child, ends the current file
as failed, resets within 25 seconds, writes `plan runner_terminated` and exits 143.
Settings: DEVICE_SCENARIOS_GLOB (files to run; default feature/*/src/deviceScenarios/*.txt
app/src/deviceScenarios/*.txt), DEVICE_SCENARIO_POLL (seconds between two dumps, default 1),
DEVICE_SCENARIO_FIRST_SCREENS (selector that means "the app is on screen" for `consent decline`; default
id=app_root, the testTag of the root surface of MainActivity; an app may name its first screens instead),
DEVICE_SCENARIO_START_WAIT (limit of am start -W, default 15), DEVICE_SCENARIO_PROCESS_WAIT (seconds to wait for the
app process, default 5), DEVICE_SCENARIO_CONSENT_CLOSE (seconds to wait for the consent form to close, default 5),
DEVICE_SCENARIO_STOP_SETTLE (seconds fresh waits after stopping the app when the device was given back or changed by
a device command, default 3),
DEVICE_SCENARIO_SETTLE (seconds a device command waits for the screen to settle, default 10), DEVICE_SCENARIO_ROOT
(repository that check reads; default this one). check does not read Kotlin files under a test source set (src/test*, src/androidTest*).
Messages are in Vietnamese for the team; comments are in English.
"""

import glob
import os
import re
import shlex
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
# Some WebViews of the consent form (Xiaomi HyperOS) label the buttons only in content-desc (L-122).
CONSENT_DECLINE = "text=Do not consent | text=Không đồng ý | desc=Do not consent | desc=Không đồng ý"
FIRST_SCREENS = os.environ.get("DEVICE_SCENARIO_FIRST_SCREENS", "id=app_root")
# MainActivity calls ads.gatherConsent() asynchronously, so the form may show a few seconds after the app root.
CONSENT_GRACE = float(os.environ.get("DEVICE_SCENARIO_CONSENT_GRACE", "8"))
# Time budget of one reset_device call. Per-file resets are counted inside DEVICE_SCENARIOS_BUDGET (a file ends this
# much before the budget), so only the final reset of the run runs past the budget, and the outer
# `timeout -k 30 <budget + 120>` of device-session.sh has room for it. After SIGTERM the reset must fit in the 30
# seconds before SIGKILL, hence the shorter limit.
RESET_LIMIT = float(os.environ.get("DEVICE_SCENARIO_RESET_LIMIT", "60"))
TERMINATED_RESET_LIMIT = 25.0
NIGHT_MODES = ("yes", "no", "auto")
UNFOLDED_SIZE, UNFOLDED_DENSITY = "1766x2208", "420"  # 673 x 841 dp, the frame of the _unfolded screenshots
IDLE_ERROR = "could not get idle state"
SYSTEM_UI = "com.android.systemui"
SHADE_NOTE = "bảng của hệ thống che app, đã đóng (cmd statusbar collapse)"
RESULT_PASSED, RESULT_FAILED, RESULT_ENV = "đạt", "lỗi", "bỏ qua (môi trường)"
RESULT_TIME, RESULT_SYNTAX = "bỏ qua (hết thời gian)", "lỗi tệp"
RETRY = "chạy lại"  # not a result in the log: the first run of a file whose app the system killed
EVIDENCE_SHOT = 10  # seconds keep_evidence may take for fail.png
SELECTOR = re.compile(r'^(id|idp|desc|text|str)=("[^"]*"|.+?)((?:\s+(?:in|index|selected|clickable)=\S+)*)\s*$')
QUOTED = re.compile(r'"[^"]*"')
SAFE_NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*\.txt")
TICKET_NAME = re.compile(r"(T-\d+)-")
SCREEN_STEPS = {"wait", "need", "tap", "tap_if", "scroll", "expect", "expect_no", "count"}
CONSENT_AGAIN = "hộp thoại đồng ý hiện lại sau khi đã từ chối"
CONSENT_OPEN = "hộp thoại đồng ý không đóng sau khi chạm từ chối"
FILTER = re.compile(r"(in|index|selected|clickable)=(\S+)")
TRAILING_NUMBER = re.compile(r"^(.*\S)\s+(\d+)$")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]")
LOCALE_TAG = re.compile(r"[A-Za-z]{2,3}(-[A-Za-z0-9]+)*")
SCENARIO_LANGUAGE = "en"  # str= reads res/values, which is English in every module (T-050, T-051)
KILLED_WITH = re.compile(r"Killing \d+:(?P<package>[^/\s]+)/\S+ .*depends on provider \S+ in dying proc (?P<proc>\S+)")


def unfolded_display(physical):
    """(size, density) for 673 x 841 dp: 1766x2208 at 420, or smaller when Android would cut 2208 down to three
    times the longer side of the screen (a 320x640 emulator allows 1920)."""
    limit = 3 * max(physical) if physical else None
    if limit is None or 2208 <= limit:
        return UNFOLDED_SIZE, UNFOLDED_DENSITY
    density = 160 * limit // 841
    return f"{-(-673 * density // 160)}x{-(-841 * density // 160)}", str(density)


class AdbTimeout(Exception):
    """An adb call did not return within its deadline."""


class Fail(Exception):
    """The step failed; the message is the reason in the log."""


class Skip(Exception):
    """The environment lacks what the scenario needs; the file ends as skipped."""


class ConsentFail(Exception):
    """The consent form came back or did not close: a failure that need does not turn into a skip (ADR-0013)."""


class Terminated(BaseException):
    """SIGTERM arrived (the outer timeout of device-session.sh); raised so the finally blocks reset the device."""


def on_sigterm(signum, frame):
    # Raise only once: a second SIGTERM must not interrupt the reset that the first one started.
    signal.signal(signal.SIGTERM, signal.SIG_IGN)
    raise Terminated()


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
        value = match.group(2)
        value = value[1:-1] if QUOTED.fullmatch(value) else value.strip()
        alternatives.append({"kind": match.group(1), "value": value, "filters": filters})
    return alternatives


def ambiguous(rest):
    """True when an unquoted text= or desc= value has a space once the number at the end of the line is read as part
    of it, so even one word is ambiguous: `wait desc=Back 10` could mean "Back 10" or 10 seconds."""
    for part in re.split(r"\s+\|\s+", rest.strip()):
        match = SELECTOR.match(part)
        if match and match.group(1) in ("text", "desc") and not match.group(2).startswith('"') \
                and re.search(r"\s", match.group(2).strip()):
            return True
    return False


def selector_and_number(rest, required=False):
    """(alternatives, n) of `<selector> [n]`; ValueError when the number could belong to the value."""
    text, number = split_number(rest, required)
    if number is not None and ambiguous(rest):
        raise ValueError("mọi text= hay desc= không ngoặc trên dòng kết thúc bằng một số là mơ hồ, kể cả một từ: "
                         'ghi giá trị trong ngoặc kép, ví dụ desc="Back" 10')
    return parse_selector(text), number


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
        selector, seconds = selector_and_number(rest)
        return {"selector": selector, "seconds": 20 if seconds is None else seconds}
    if command in ("tap", "tap_if", "expect", "expect_no"):
        return {"selector": parse_selector(rest)}
    if command == "scroll":
        selector, times = selector_and_number(rest)
        return {"selector": selector, "times": 3 if times is None else times}
    if command == "count":
        selector, number = selector_and_number(rest, required=True)
        return {"selector": selector, "number": number}
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
    name = Path(path).name
    named = TICKET_NAME.match(name)
    if not SAFE_NAME.fullmatch(name):
        errors.insert(0, (0, "tên tệp chỉ gồm chữ, số, '.', '_', '-' và có đuôi .txt"))
    elif named and named.group(1) != header.get("ticket", ""):
        errors.insert(0, (0, f"tên tệp bắt đầu bằng {named.group(1)} nhưng ticket là '{header.get('ticket', '')}'"))
    return header, steps, errors


def source_files(root, name):
    """Files called `name` (or matching a glob) under app/, feature/ and core/, outside build folders."""
    for folder in ("app", "feature", "core"):
        base = root / folder
        for path in sorted(base.rglob(name)) if base.is_dir() else []:
            if "build" not in path.relative_to(root).parts:
                yield path


def is_test_source(path, root):
    """True under a test source set (src/test, src/androidTest, src/testStaging...): a tag that only a test still
    names is gone from the app, so it must not count as present."""
    parts = path.relative_to(root).parts
    return any(parts[i] == "src" and re.match(r"(test|androidTest)", parts[i + 1]) for i in range(len(parts) - 1))


def kotlin_text(root):
    return "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in source_files(root, "*.kt")
                     if not is_test_source(path, root))


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

    def __init__(self, root, strings, xml=""):
        self.nodes = list(root.iter("node"))
        self.strings = strings
        self.xml = xml
        self.parents = {id(child): node for node in self.nodes for child in node if child.tag == "node"}

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
            ok = self.selected(node)
        if ok and filters.get("clickable") == "true":
            ok = node.get("clickable") == "true"
        return ok

    def selected(self, node):
        """The node is selected or checked; a node that is not clickable may get it from up to two parents, up to the
        first clickable one (the text of a chip sits in the selected node)."""
        chain, current = [node], node
        if node.get("clickable") != "true":
            for _ in range(2):
                current = self.parents.get(id(current))
                if current is None:
                    break
                chain.append(current)
                if current.get("clickable") == "true":
                    break
        return any(item.get("selected") == "true" or item.get("checked") == "true" for item in chain)

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

def kill_group(process):
    try:
        os.killpg(process.pid, signal.SIGKILL)
    except (ProcessLookupError, PermissionError):
        pass  # macOS answers EPERM for a group whose leader is a zombie: nothing is left to kill


def run_process(command, timeout, binary=False, env=None):
    """(exit code, output). At the deadline the whole process group is killed, so no child keeps the pipe open."""
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL if binary else subprocess.STDOUT,
                               start_new_session=True, env=env)
    try:
        output, _ = process.communicate(timeout=timeout)
    except subprocess.TimeoutExpired:
        kill_group(process)
        process.communicate()
        raise AdbTimeout(" ".join(command[:4])) from None
    except BaseException:
        # SIGTERM (Terminated) or another error while waiting: the child runs in its own session, so it would outlive
        # the runner; kill its group and reap it.
        kill_group(process)
        process.wait()
        raise
    return process.returncode, (output if binary else output.decode("utf-8", errors="replace"))


def env_seconds(name, default):
    return float(os.environ.get(name, str(default)))


def seconds_text(value):
    return f"{value:g}" if float(value).is_integer() else f"{value:.1f}"


class Runner:
    def __init__(self, session, package):
        self.session = Path(session)
        self.package = package
        self.started = time.monotonic()
        self.budget = env_seconds("DEVICE_SCENARIOS_BUDGET", 1800)
        self.adb_timeout = env_seconds("DEVICE_SCENARIO_ADB_TIMEOUT", 30)
        self.poll = env_seconds("DEVICE_SCENARIO_POLL", 1)
        self.settle = env_seconds("DEVICE_SCENARIO_SETTLE", 10)
        self.consent_close = env_seconds("DEVICE_SCENARIO_CONSENT_CLOSE", 5)
        self.start_wait = env_seconds("DEVICE_SCENARIO_START_WAIT", 15)
        self.process_wait = env_seconds("DEVICE_SCENARIO_PROCESS_WAIT", 5)
        self.evidence_reserve = env_seconds("DEVICE_SCENARIO_EVIDENCE_RESERVE", 20)
        self.stop_settle = env_seconds("DEVICE_SCENARIO_STOP_SETTLE", 3)
        # The device was given back (or changed by a device command) since the last fresh, so fresh waits after
        # stopping the app.
        self.device_reset = False
        self.sdk = None  # API level of the device, read once at the start of the run
        self.log_path = self.session / "scenarios.txt"
        self.file_deadline = 0.0
        self.file_dir = self.session
        self.last_xml = None
        self.activity = None
        self.strings = string_values(ROOT)
        self.original_size = self.original_density = None
        self.original_settings = {"accelerometer_rotation": "1", "user_rotation": "0"}
        self.original_night = "no"
        self.physical_size = None  # (width, height) of the screen from `wm size`, for device unfold
        self.network_touched = False
        self.network_used = False  # any file of the run, so the last reset turns the network on
        self.terminated = False
        self.terminated_at = None  # monotonic time of the first SIGTERM: every reset after it shares one limit
        # False from the moment a file may change the device until a reset_device call runs to its end, so run() can
        # give the device back when SIGTERM cut the reset of a file short.
        self.reset_complete = True
        self.cached_screen = None  # last dump of consent decline or device, for the next step that reads the screen
        self.pending_screen = None  # that dump, handed to the step that runs now
        self.consent_state = None  # None, "late" (the app showed before the form) or "declined"
        self.notes = []

    def log(self, line):
        with open(self.log_path, "a", encoding="utf-8") as handle:
            handle.write(line.replace("\n", " ") + "\n")

    def note(self, text):
        self.notes.append(text)

    def left(self):
        return self.file_deadline - time.monotonic()

    def budget_left(self):
        return self.budget - (time.monotonic() - self.started)

    def pause(self, end=None):
        time.sleep(self.poll if end is None else min(self.poll, max(0.0, end - time.monotonic())))

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

    def helper_output(self, name):
        """The last line of a `device-session.sh helper` function that prints a value; empty when it fails."""
        limit = min(max(self.adb_timeout, 45.0), max(self.left(), 1.0))
        code, output = run_process([str(SESSION_SCRIPT), "helper", name], limit,
                                   env=dict(os.environ, APP_PACKAGE=self.package))
        lines = output.replace("\r", "").strip().splitlines()
        return lines[-1].strip() if code == 0 and lines else ""

    def dump(self):
        """(screen, None) or (None, reason). A dump of the system UI only (the notification shade or quick settings
        over the app, camera-pose FB-20261027-02) closes the shade once and reads the screen again."""
        screen, reason = self.dump_once()
        if screen is None or not screen.nodes or any(node.get("package") != SYSTEM_UI for node in screen.nodes):
            return screen, reason
        self.collapse_shade()
        if SHADE_NOTE not in self.notes:
            self.note(SHADE_NOTE)
        return self.dump_once()

    def collapse_shade(self):
        """Closes the notification shade and quick settings; a device without the command keeps what it shows."""
        self.adb("shell", "cmd", "statusbar", "collapse")

    def dump_once(self):
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
        return Screen(root, self.strings, text), None

    def next_screen(self):
        """The dump kept by the step before (once), or a new one."""
        if self.pending_screen is not None:
            screen, self.pending_screen = self.pending_screen, None
            return screen, None
        return self.dump()

    def wait_consent_closed(self, end):
        """Dumps until no decline button shows; the first screen without it, or None at the end."""
        decline = parse_selector(CONSENT_DECLINE)
        while time.monotonic() < end:
            self.pause(end)
            screen, _ = self.dump()  # a failed or busy dump is read again, never counted as closed
            if screen is not None and screen.find(decline) is None:
                return screen
        return None

    def consent_rule(self, screen, end):
        """A screen without what the step looks for: when it is the consent form, decline it once after the app showed
        first, or fail when it is back after a decline. Returns the screen after closing it, or None."""
        node = screen.find(parse_selector(CONSENT_DECLINE))
        if node is None or self.consent_state is None:
            return None
        if self.consent_state == "declined":
            raise ConsentFail(CONSENT_AGAIN)
        point = self.tap_node(node, noted=False)
        self.note(f"hộp thoại đồng ý hiện muộn, đã từ chối (chạm {point})")
        closed = self.wait_consent_closed(min(time.monotonic() + self.consent_close, end))
        if closed is None:
            raise ConsentFail(CONSENT_OPEN)
        self.consent_state = "declined"
        return closed

    def dump_ready(self, alternatives, seconds=5.0):
        """A dump, tried again while the app is not idle (an endless animation); Fail with the reason otherwise."""
        end = min(time.monotonic() + seconds, self.file_deadline)
        while True:
            screen, reason = self.next_screen()
            if screen is not None:
                if screen.find(alternatives) is None:
                    return self.consent_rule(screen, end) or screen
                return screen
            if reason != IDLE_ERROR or time.monotonic() >= end:
                raise Fail(reason)
            self.pause()

    def wait_for(self, alternatives, seconds):
        """The first matching node; Fail with the reason of the last dump when it never shows."""
        end = min(time.monotonic() + seconds, self.file_deadline)
        while True:
            screen, reason = self.next_screen()
            if screen is not None:
                node = screen.find(alternatives)
                if node is None:
                    closed = self.consent_rule(screen, self.file_deadline)
                    node = closed.find(alternatives) if closed is not None else None
                if node is not None:
                    return node
            if time.monotonic() >= end:
                raise Fail(reason or "không thấy phần tử")
            self.pause(end)

    def tap_node(self, node, noted=True):
        x, y = centre(node)
        self.adb("shell", "input", "tap", str(x), str(y))
        if noted:
            self.note(f"tap {x},{y} {label(node)}")
        return f"{x},{y}"

    # -- commands
    def step_fresh(self, arguments):
        self.cached_screen = None
        self.consent_state = None
        self.adb("logcat", "-b", "crash", "-c")
        self.adb("shell", "am", "force-stop", self.package)
        stopped = time.monotonic()
        self.adb("shell", "pm", "clear", self.package)
        if self.helper("grant-permissions") != 0:
            raise Fail("không cấp được quyền của app")
        if self.activity is None:
            # The same pick as the standard scenarios: a staging build may have several LAUNCHER activities (the ad
            # preview screen), where resolve-activity returns the system chooser.
            self.activity = self.helper_output("launcher-activity")
        if "/" not in self.activity:
            raise Fail(f"không tìm thấy màn hình khởi động của {self.package}")
        self.set_language()
        self.collapse_shade()  # monkey runs before the plan scenarios and may leave the shade open over the app
        if self.device_reset:
            # Stopping an app whose Activity the reset is still rebuilding makes the system kill the next start
            # about a second later ("Destroy timeout of remove-task", camera-pose FB-20261025-02): start after that.
            self.device_reset = False
            # The shorter of the two, not (stopped + settle) - stopped, which can be 1.9999 and print as "2.0".
            wait = max(0.0, min(self.stop_settle, self.file_deadline - stopped))
            self.note(f"fresh: chờ {seconds_text(wait)} giây sau khi dừng app (máy vừa trả về gốc)")
            time.sleep(max(0.0, stopped + wait - time.monotonic()))
        problem = self.start_app()
        if problem is None:
            return
        self.note(f"fresh: lần mở 1 không có tiến trình ({problem}), mở lại")
        _, crashes = self.adb("logcat", "-b", "crash", "-d")
        if self.package in crashes:
            raise Fail("app crash ở lần mở đầu")  # not started again, so a crash at start-up is not hidden
        self.adb("shell", "am", "force-stop", self.package)
        problem = self.start_app()
        if problem is not None:
            self.note(f"fresh: lần mở 2 không có tiến trình ({problem})")
            raise Fail("app không chạy sau 2 lần mở")

    def set_language(self):
        """The app shows English, the language str= reads: from Android 13 its own language is set; below 13 the
        language of the device must be English, otherwise the file is skipped (plan T-054)."""
        if self.sdk is not None and self.sdk < 33:
            _, output = self.adb("shell", "getprop", "persist.sys.locale")
            if not output.strip():  # a device never set to another language keeps its factory one
                _, output = self.adb("shell", "getprop", "ro.product.locale")
            language = output.strip() or "?"
            if not language.lower().startswith(SCENARIO_LANGUAGE):
                raise Skip(f"máy để ngôn ngữ {language}; dưới Android 13 trình chạy không đặt được ngôn ngữ của app, "
                           "đặt máy về tiếng Anh")
            return
        code, output = self.adb("shell", "cmd", "locale", "set-app-locales", self.package, "--locales",
                                SCENARIO_LANGUAGE)
        if code != 0 or re.search(r"Exception|Error|Unknown", output):
            raise Skip(f"không đặt được ngôn ngữ tiếng Anh cho app ({(output.strip().splitlines() or [str(code)])[0]})")

    def start_app(self):
        """None when the app has a process after am start -W, otherwise why not. A start that outlives its limit
        still counts when the process is there (an app that starts slowly but runs)."""
        limit = min(self.start_wait, self.left())
        if limit <= 0:
            raise Fail("hết hạn của tệp")
        timed_out = None
        try:
            self.adb("shell", "am", "start", "-W", "-n", self.activity, timeout=limit)
        except AdbTimeout:
            timed_out = f"am start -W hết {seconds_text(limit)} giây"
        end = min(time.monotonic() + self.process_wait, self.file_deadline)
        while True:
            _, output = self.adb("shell", "pidof", self.package)
            if re.fullmatch(r"\s*\d+(\s+\d+)*\s*", output):
                if timed_out:
                    self.note(f"fresh: {timed_out}, có tiến trình")
                return None
            if time.monotonic() >= end:
                return timed_out or "pidof rỗng"
            self.pause(end)

    def step_consent(self, arguments):
        decline, first = parse_selector(CONSENT_DECLINE), parse_selector(FIRST_SCREENS)
        end = min(time.monotonic() + arguments["seconds"], self.file_deadline)
        app_seen = False
        while True:
            screen, _ = self.dump()
            if screen is not None:
                node = screen.find(decline)
                if node is not None:
                    if self.consent_state == "declined":
                        raise ConsentFail(CONSENT_AGAIN)
                    self.note(f"đã chạm từ chối {self.tap_node(node, noted=False)}")
                    closed = self.wait_consent_closed(min(time.monotonic() + self.consent_close, self.file_deadline))
                    if closed is None:
                        raise ConsentFail(CONSENT_OPEN)
                    self.consent_state = "declined"
                    self.cached_screen = closed
                    return
                if not app_seen and screen.find(first) is not None:
                    if self.consent_state == "declined":
                        return  # keep the decline: a form after it is a failure, not a late form
                    # The app is on screen, but the form may still come: keep looking for a short, bounded while.
                    app_seen = True
                    end = min(time.monotonic() + CONSENT_GRACE, self.file_deadline)
            if time.monotonic() >= end:
                if app_seen:
                    # No form on this start yet; a step that reads the screen declines a later one, once.
                    self.consent_state = "late"
                    self.note("chưa thấy hộp thoại đồng ý")
                    return
                raise Fail("không thấy hộp thoại đồng ý hay màn hình đầu của app")
            self.pause(end)

    def step_wait(self, arguments):
        self.wait_for(arguments["selector"], arguments["seconds"])

    def step_need(self, arguments):
        try:
            self.wait_for(arguments["selector"], arguments["seconds"])
        except Fail as problem:
            raise Skip(f"thiếu điều kiện của môi trường ({problem})") from None

    def step_tap(self, arguments):
        node = self.dump_ready(arguments["selector"]).find(arguments["selector"])
        if node is None:
            raise Fail("không thấy phần tử")
        self.tap_node(node)

    def step_tap_if(self, arguments):
        node = self.dump_ready(arguments["selector"]).find(arguments["selector"])
        if node is not None:
            self.tap_node(node)

    def step_scroll(self, arguments):
        node = self.dump_ready(arguments["selector"]).find(arguments["selector"])
        if node is None:
            raise Fail("không thấy phần tử")
        x1, y1, x2, y2 = bounds_of(node)
        x = (x1 + x2) // 2
        start, end = y1 + (y2 - y1) * 4 // 5, y1 + (y2 - y1) // 5
        self.note(f"scroll {x},{start} -> {x},{end} {label(node)}")
        for _ in range(arguments["times"]):
            self.adb("shell", "input", "swipe", str(x), str(start), str(x), str(end), "300")

    def step_expect(self, arguments):
        if self.dump_ready(arguments["selector"]).find(arguments["selector"]) is None:
            raise Fail("không thấy phần tử")

    def step_expect_no(self, arguments):
        if self.dump_ready(arguments["selector"]).find(arguments["selector"]) is not None:
            raise Fail("phần tử vẫn còn")

    def step_count(self, arguments):
        found = len(self.dump_ready(arguments["selector"]).find_all(arguments["selector"]))
        if found != arguments["number"]:
            raise Fail(f"có {found} phần tử, cần {arguments['number']}")

    def step_network(self, arguments):
        self.network_touched = self.network_used = True
        if arguments["state"] == "on":
            if self.helper("network-on") != 0:
                raise Skip("bật lại mạng nhưng máy chưa có mạng mặc định")
        elif self.helper("network-off") != 0:
            raise Skip("không tắt được mạng (máy vẫn có mạng mặc định)")

    def step_device(self, arguments):
        self.device_reset = True  # a fresh later in the file stops an app whose Activity was rebuilt, like after a reset
        words = arguments["words"]
        if words[0] == "rotate":
            self.adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
            self.adb("shell", "settings", "put", "system", "user_rotation", "1" if words[1] == "landscape" else "0")
        elif words[0] == "night":
            self.adb("shell", "cmd", "uimode", "night", "yes" if words[1] == "on" else "no")
        elif words[0] == "unfold":
            # The first tap after a letterboxed unfold only closes the education dialog of the system, which
            # uiautomator does not see (camera-pose FB-20261025-01); the reset turns it back to the default.
            self.adb("shell", "wm", "set-letterbox-style", "--isEducationEnabled", "false")
            size, density = unfolded_display(self.physical_size)
            self.adb("shell", "wm", "size", size)
            self.adb("shell", "wm", "density", density)
        elif words[0] == "fold":
            self.adb("shell", "wm", "size", self.original_size or "reset")
            self.adb("shell", "wm", "density", self.original_density or "reset")
        else:
            self.adb("shell", "cmd", "locale", "set-app-locales", self.package, "--locales", words[1])
        self.wait_settled()

    def wait_settled(self):
        """Dumps until two in a row are the same (the Activity was rebuilt); the last one goes to the next step."""
        began = time.monotonic()
        end = min(began + self.settle, self.file_deadline)
        previous = last = None
        while True:
            screen, _ = self.dump()  # a failed or busy dump is read again
            if screen is not None:
                last = screen
                if previous is not None and screen.xml == previous.xml:
                    self.cached_screen = screen
                    self.note(f"ổn định sau {time.monotonic() - began:.1f} giây")
                    return
            previous = screen
            if time.monotonic() >= end:
                self.cached_screen = last
                self.note(f"màn hình không ổn định sau {time.monotonic() - began:.1f} giây")
                return
            self.pause(end)

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
                _, output = run_process(["adb", "shell", "wm", what], 5)
            except AdbTimeout:
                continue
            physical = re.search(r"Physical size: *(\d+)x(\d+)", output)
            if physical:
                self.physical_size = (int(physical.group(1)), int(physical.group(2)))
            override = re.search(r"Override (?:size|density): *(\S+)", output)
            if override:
                setattr(self, f"original_{what}", override.group(1))

    def read_settings(self):
        """Remembers the rotation settings and the night mode of the device, so the reset gives them back. A value
        that cannot be read keeps the usual default."""
        for name in self.original_settings:
            try:
                code, output = run_process(["adb", "shell", "settings", "get", "system", name], 10)
            except AdbTimeout:
                continue
            value = output.strip()
            if code == 0 and value.isdigit():
                self.original_settings[name] = value
        try:
            code, output = run_process(["adb", "shell", "cmd", "uimode", "night"], 10)
        except AdbTimeout:
            return
        night = re.search(r"Night mode: *(\w+)", output)
        if code == 0 and night and night.group(1) in NIGHT_MODES:
            self.original_night = night.group(1)

    def reset_device(self, network=False):
        """Gives the device back as the run found it. Each call has its own short limit and may fail alone; the
        whole reset stays within RESET_LIMIT (TERMINATED_RESET_LIMIT after SIGTERM)."""
        self.device_reset = True  # the next fresh waits after stopping the app (see step_fresh)
        if self.terminated:
            self.terminated_at = self.terminated_at or time.monotonic()
            end = self.terminated_at + TERMINATED_RESET_LIMIT
        else:
            end = time.monotonic() + RESET_LIMIT
        commands = [
            ["shell", "wm", "size", self.original_size or "reset"],
            ["shell", "wm", "density", self.original_density or "reset"],
            # device unfold turned the letterbox education dialog off; back to the default of the device.
            ["shell", "wm", "reset-letterbox-style", "isEducationEnabled"],
            ["shell", "settings", "put", "system", "accelerometer_rotation", self.original_settings["accelerometer_rotation"]],
            ["shell", "settings", "put", "system", "user_rotation", self.original_settings["user_rotation"]],
            ["shell", "cmd", "uimode", "night", self.original_night],
            ["shell", "cmd", "locale", "set-app-locales", self.package],  # without --locales: back to the system language
        ]
        for command in commands:
            limit = min(10.0, end - time.monotonic())
            if limit <= 0:
                return
            try:
                run_process(["adb", *command], limit)
            except AdbTimeout:
                pass
        limit = min(45.0, end - time.monotonic())
        if network and limit > 0:
            # network-on waits (bounded) until the device has a default network again.
            try:
                run_process([str(SESSION_SCRIPT), "helper", "network-on"], limit, env=dict(os.environ, APP_PACKAGE=self.package))
            except AdbTimeout:
                pass
        self.reset_complete = True

    # -- files
    def run_file(self, path):
        name = re.sub(r"[^A-Za-z0-9._-]", "_", Path(path).name)
        self.file_dir = self.session / "scenarios" / Path(name).stem
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
        first_began, attempt, first_run = time.monotonic(), 1, 0.0
        while True:
            # The reset after the file is part of the budget (see RESET_LIMIT), and so is the time to keep the evidence.
            available = self.budget_left() - RESET_LIMIT - self.evidence_reserve
            if attempt > 1 and available < min(self.timeout_of(header), first_run):
                self.log(f"plan {name} step=0 retry SKIP app bị hệ thống giết; không còn thời gian chạy lại")
                available = 0.0
            if available <= 0:
                result = RESULT_TIME if attempt == 1 else RESULT_ENV
                self.log(f"plan {name} result={result} seconds={int(round(time.monotonic() - first_began))}")
                return
            result = self.attempt_file(name, header, steps, available, attempt, first_began)
            if result != RETRY:
                return
            first_run = time.monotonic() - first_began
            attempt += 1

    def attempt_file(self, name, header, steps, available, attempt, first_began):
        """One run of the steps, its result logged (except RETRY) and the device given back; RETRY when the system
        killed the app on the first run and there is time to run the file once more (FB-20261025-03)."""
        began = time.monotonic()
        self.file_deadline = began + min(self.timeout_of(header), available)
        self.last_xml = None
        self.cached_screen = self.pending_screen = None
        self.consent_state = None
        self.network_touched = False
        self.reset_complete = False  # from here on the file may change the device
        result = RESULT_FAILED
        text = f"plan {name} step=0 start"
        try:
            try:
                marker = shlex.quote(f"scenario=plan file={name}")
                self.adb("shell", "log", "-t", MARKER_TAG, marker, timeout=10)
            except AdbTimeout:
                pass
            result = RESULT_PASSED
            for index, (_, command, rest, arguments) in enumerate(steps, start=1):
                text = f"plan {name} step={index} {command}{' ' + rest if rest else ''}"
                at = time.monotonic() - began
                # A kept dump is for the next step only, and only when it reads the screen; every other step drops it.
                self.pending_screen = self.cached_screen if command in SCREEN_STEPS else None
                self.cached_screen = None
                self.notes = []
                outcome, failed = f"{text} ok", False
                try:
                    getattr(self, "step_" + command)(arguments)
                except Skip as skipped:
                    outcome, result = f"{text} SKIP {skipped}", RESULT_ENV
                except (AdbTimeout, Fail, ConsentFail) as problem:
                    reason = "adb_timeout" if isinstance(problem, AdbTimeout) else problem
                    outcome, result, failed = f"{text} FAIL {reason}", RESULT_FAILED, True
                    killer = self.killed_by(name, attempt)
                    if killer is not None:
                        why = f"app bị hệ thống giết theo {killer}"
                        # A second run that has only a few seconds would end as a failure of its own: run again only
                        # when there is the time this run took (at most the timeout of the file), its evidence, and
                        # two returns of the device.
                        needed = min(self.timeout_of(header), time.monotonic() - began) + EVIDENCE_SHOT \
                            + self.evidence_reserve + 2 * RESET_LIMIT
                        if attempt == 1 and self.budget_left() >= needed:
                            outcome, result = f"{text} RETRY {why}", RETRY
                            self.note(f"{why} (logcat.txt); chạy lại tệp từ đầu, mở lại app")
                        else:
                            late = "" if attempt > 1 else "; không còn thời gian chạy lại"
                            outcome, result = f"{text} SKIP {why}{late}", RESULT_ENV
                self.pending_screen = None
                self.log(" ".join([f"plan {name} note step={index} t={at:.1f}", "; ".join(self.notes)]).rstrip())
                self.log(outcome)
                if failed:
                    self.keep_evidence("fail-1" if result == RETRY else "fail")
                if result != RESULT_PASSED:
                    break
        except Terminated:
            self.terminated = True  # the reset below then uses the shorter limit
            self.terminated_at = self.terminated_at or time.monotonic()
            self.log(f"{text} FAIL trình chạy bị dừng (SIGTERM, hết thời gian của cả lượt)")
            result = RESULT_FAILED
            raise
        except Exception as error:
            # An internal error of the runner: the file fails, the device is still given back, the run stops.
            self.log(f"{text} FAIL lỗi của trình chạy ({type(error).__name__}: {error})")
            result = RESULT_FAILED
            raise
        finally:
            if result != RETRY:
                self.log(f"plan {name} result={result} seconds={int(round(time.monotonic() - first_began))}")
            self.reset_device(network=self.network_touched or self.terminated)
        return result

    def timeout_of(self, header):
        return int(header.get("timeout", DEFAULT_TIMEOUT))

    def killed_by(self, name, attempt=1):
        """Who the system killed the app (or one of its processes, <package>:<name>) with, from logcat.txt of the
        session after the marker of this run of the file: "Google Play services (<process>)" or the dying process;
        None without such a line, or while logcat.txt does not have the marker of this run yet."""
        try:
            lines = (self.session / "logcat.txt").read_text(encoding="utf-8", errors="replace").splitlines()
        except OSError:
            return None
        marker = f"scenario=plan file={name}"
        starts = [number for number, line in enumerate(lines) if marker in line]
        if len(starts) < attempt:
            return None  # the marker of this run is not written yet: a kill after an older marker is not this run
        for line in lines[starts[-1] + 1:]:
            found = KILLED_WITH.search(line)
            if found and found.group("package").split(":")[0] == self.package:
                proc = found.group("proc")
                return f"Google Play services ({proc})" if proc.startswith("com.google.android.gms") else proc
        return None

    def keep_evidence(self, stem="fail"):
        """<stem>.xml (the last dump) and <stem>.png of the first failing step (fail-1 for a run that is run again)."""
        if self.last_xml:
            self.file_dir.mkdir(parents=True, exist_ok=True)
            (self.file_dir / f"{stem}.xml").write_text(self.last_xml, encoding="utf-8")
        try:
            self.save_screenshot(f"{stem}.png", timeout=10)
        except (AdbTimeout, Fail, OSError):
            pass

    def run(self, files):
        self.session.mkdir(parents=True, exist_ok=True)
        # Read once before the first file, so a reset that failed in one file cannot become the "original" state.
        self.read_display()
        self.read_settings()
        try:
            _, output = run_process(["adb", "shell", "getprop", "ro.build.version.sdk"], 5)
            self.sdk = int(output.strip())
        except (AdbTimeout, ValueError):
            self.sdk = None  # unknown: fresh tries to set the language of the app
        try:
            for path in files:
                self.run_file(path)
        except Terminated:
            self.terminated = True  # also when SIGTERM came between two files or during the reset of a file
            self.terminated_at = self.terminated_at or time.monotonic()
            raise
        finally:
            # After SIGTERM the file's own finally has usually reset the device already; SIGTERM during that reset
            # cuts it short, so give the device back here, within what is left of TERMINATED_RESET_LIMIT. SIGTERM is
            # ignored from the first one on (on_sigterm), so this reset is not interrupted again.
            if not self.terminated:
                self.reset_device(network=self.network_used)
            elif not self.reset_complete:
                self.reset_device(network=True)
        print(f"Kịch bản theo plan: {len(files)} tệp, kết quả trong {self.log_path}")


def label(node):
    """resource-id, else text, else content-desc of a node, for the note of a tap (at most 40 characters)."""
    text = Screen.tag(node) or node.get("text", "") or node.get("content-desc", "")
    return " ".join(text.split())[:40]


def scenario_files():
    files = []
    for pattern in os.environ.get("DEVICE_SCENARIOS_GLOB", DEFAULT_GLOB).split():
        files += glob.glob(pattern if os.path.isabs(pattern) else str(ROOT / pattern))
    return sorted(dict.fromkeys(files))


def main(argv):
    if len(argv) >= 3 and argv[1] == "check":
        root = Path(os.environ["DEVICE_SCENARIO_ROOT"]) if os.environ.get("DEVICE_SCENARIO_ROOT") else ROOT
        messages = check_files(argv[2:], root=root)
        for message in messages:
            print(message)
        return 1 if messages else 0
    if len(argv) == 6 and argv[1] == "run" and argv[2] == "--session" and argv[4] == "--package":
        files = scenario_files()
        if files:
            signal.signal(signal.SIGTERM, on_sigterm)
            try:
                Runner(argv[3], argv[5]).run(files)
            except Terminated:
                Path(argv[3]).mkdir(parents=True, exist_ok=True)
                with open(Path(argv[3]) / "scenarios.txt", "a", encoding="utf-8") as handle:
                    handle.write("plan runner_terminated\n")
                print("Trình chạy kịch bản bị dừng (SIGTERM); đã dừng lệnh adb đang chạy, máy đã được trả về gốc.",
                      file=sys.stderr)
                return 143
        return 0
    print("Cách dùng: scripts/device-scenario.py check <tệp.txt>... | run --session <thư mục> --package <gói>",
          file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
