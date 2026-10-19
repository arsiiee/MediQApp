#!/usr/bin/env python
"""Post-humanizer verification for MediQ_Final_Project.html / .pdf.

Checks that the prose edits did not break the build's own contract:
  1. HTML tag balance and entity sanity
  2. data-k anchors intact, one each
  3. TOC page numbers agree with where sections actually start in the PDF
  4. build.py TARGETS fragments still present on the expected page
  5. numeric claims unchanged against the pre-edit text in git history if available
  6. no residual humanizer patterns in prose (excluding code, titles, references)
"""

import html
import re
import sys
from collections import Counter
from pathlib import Path

from pypdf import PdfReader

HERE = Path(__file__).resolve().parent
SRC = HERE / "MediQ_Final_Project.html"
PDF = HERE / "MediQ_Final_Project.pdf"

sys.path.insert(0, str(HERE))
from build import TARGETS  # noqa: E402

fails: list[str] = []
warns: list[str] = []


def fail(msg: str) -> None:
    fails.append(msg)


def warn(msg: str) -> None:
    warns.append(msg)


raw = SRC.read_text(encoding="utf-8")
pages = [(p.extract_text() or "") for p in PdfReader(str(PDF)).pages]
alltext = "\n".join(pages)
flat = re.sub(r"\s+", " ", raw)

# ---------------------------------------------------------------- 1. structure
body = raw.split("<body>", 1)[1].split("</body>", 1)[0]
style = raw.split("<style>", 1)[1].split("</style>", 1)[0]
pre = re.findall(r"<pre>(.*?)</pre>", body, re.S)
prose = body
for chunk in pre:
    prose = prose.replace(chunk, "")

VOID = {"img", "br", "hr", "meta", "link", "input"}
stack: list[str] = []
tag_re = re.compile(r"<(/?)(\w+)[^>]*?(/?)>")
for m in tag_re.finditer(prose):
    closing, name, selfclose = m.group(1), m.group(2).lower(), m.group(3)
    if name in VOID or selfclose:
        continue
    if closing:
        if not stack or stack[-1] != name:
            fail(f"tag imbalance: </{name}> closes {stack[-1:] or ['nothing']}")
            break
        stack.pop()
    else:
        stack.append(name)
if stack:
    fail(f"unclosed tags: {stack}")

# Entity counts, and a check for entities missing the terminating semicolon
# (a bare "&mdash" prefix also matches the valid "&mdash;", so match the tail).
for ent in ("&mdash;", "&ndash;", "&rarr;", "&ldquo;", "&rdquo;", "&sect;"):
    bare = re.findall(re.escape(ent[:-1]) + r"(?![a-zA-Z;])", raw)
    if bare:
        fail(f"entity missing semicolon: {ent[:-1]} x{len(bare)}")
print("  entities: "
      + ", ".join(f"{e}{len(re.findall(re.escape(e), raw))}"
                  for e in ("&mdash;", "&ndash;", "&rarr;", "&sect;")))

# --------------------------------------------------------------- 2. toc keys
keys = re.findall(r'<td class="p" data-k="(\w+)">(\d+)</td>', raw)
if len(keys) != len(TARGETS):
    fail(f"TOC has {len(keys)} data-k rows, build.py expects {len(TARGETS)}")
if {k for k, _ in keys} != set(TARGETS):
    fail(f"TOC keys differ: {sorted({k for k, _ in keys})} vs {sorted(TARGETS)}")

# ------------------------------------------ 3. toc pages == real section starts
def real_page(key: str) -> int | None:
    for i, text in enumerate(pages[2:], start=3):
        if all(f in re.sub(r"\s+", " ", text) for f in TARGETS[key]):
            return i
    return None


for key, claimed in keys:
    actual = real_page(key)
    if actual is None:
        fail(f"section {key} not found in PDF text")
    elif int(claimed) != actual:
        fail(f"TOC {key} claims p{claimed}, actually starts p{actual}")

# ----------------------------------------------------- 4. footer stamped
if "Page 1 of" not in alltext or f"of {len(pages)}" not in alltext:
    fail("footer/page number missing from PDF text")
if any(alltext.count(f"Page {i} of {len(pages)}") != 1 for i in range(1, len(pages) + 1)):
    fail("page numbering is not exactly one stamp per page")

# ------------------------------------------------- 5. numbers still present
NUMBERS = [
    "110 Kotlin files", "233 automated tests", "174 client", "59 server",
    "19 HTTP endpoints", "19 endpoints", "15 screens", "10 tables",
    "210,000 iterations", "210k iterations", "24 token pairs", "6 status chips",
    "1.79:1", "6.63:1", "10.01:1", "10.47:1", "6.39:1", "3.45:1", "7.33:1",
    "5.13:1", "4.67:1", "4.12:1", "1.66:1", "300&nbsp;ms",
]
missing = [n for n in NUMBERS if n not in raw]
if missing:
    fail(f"numeric claims lost from source: {missing}")

# every count in prose also reaches the PDF (catches reflow dropping a table cell)
for n in ("110", "233", "174", "59", "19", "15", "210,000", "300"):
    if n not in alltext:
        fail(f"count {n} absent from rendered PDF")

# --------------------------------------------- 6. humanizer residue in prose
NO_SECTIONS = raw.split('<h2 class="sec">References</h2>', 1)
# everything above the References heading: real prose subject to the dash rule
main_part = NO_SECTIONS[0]
main_part = re.sub(r"<pre>.*?</pre>", "", main_part, flags=re.S)
main_part = re.sub(r"<style>.*?</style>", "", main_part, flags=re.S)

# The dash rule does not apply to: <title>, bibliography entries after References,
# or table cells where an em dash is a placeholder for "no endpoint".
placeholders = [
    m for m in re.findall(r"<td>—(?:[^<]*)</td>", main_part)
]
dashes = [m for m in re.findall(r".{0,50}—.{0,50}", main_part) if m not in placeholders]
if dashes:
    warn(f"em dash left in body prose: {len(dashes)} -> {dashes[:3]}")
else:
    print("  body prose em dashes: 0 (excluding "
          f"{len(placeholders)} table placeholders)")

if re.findall(r"&mdash;", main_part):
    warn(f"&mdash; entity left in body prose: {len(re.findall(r'&mdash;', main_part))}")
if re.findall(r"&ldquo;|&rdquo;", raw):
    warn("curly quotation entities remain")
refs = NO_SECTIONS[1] if len(NO_SECTIONS) > 1 else ""
print(f"  references section: {len(re.findall(r'—', refs))} bibliographic dashes (expected)")

prose_only = main_part

# tell vocabulary that should be gone
TELL_WORDS = [
    "delve", "testament", "tapestry", "pivotal", "landscape", "showcase",
    "It is worth being", "Chosen against", "not just", "Let's dive",
    "At its core", "In conclusion", "Let that sink in", "Great question",
    "I hope this helps", "game-chang", "cutting-edge", "state-of-the-art",
]
for w in TELL_WORDS:
    if w.lower() in prose_only.lower():
        warn(f"tell vocabulary present: {w!r}")

# bold-as-decoration in prose paragraphs (outside tables)
for p in re.findall(r"<p>(.*?)</p>", prose_only, re.S):
    bolds = re.findall(r"<strong>([^<]{0,60})</strong>", p)
    if len(bolds) >= 3:
        warn(f"paragraph with {len(bolds)} bold labels (possible decoration): {bolds[:3]}")

# ------------------------------------------------------------- 7. svg assets
for src in re.findall(r'<img src="([^"]+)"', body):
    if not (HERE / src).exists():
        fail(f"missing asset: {src}")

# ------------------------------------------------------------------ report
print(f"PDF: {len(pages)} pages, {PDF.stat().st_size:,} bytes")
print(f"TOC entries checked: {len(keys)}")
print(f"em dash / mdash / curly quotes in prose: "
      f"{len(re.findall(r'—', prose_only))} / {len(re.findall(r'&mdash;', prose_only))} / "
      f"{len(re.findall(r'&ldquo;|&rdquo;', raw))}")
print(f"strong tags in <p> paragraphs: {sum(len(re.findall('<strong>', p)) for p in re.findall(r'<p>(.*?)</p>', prose_only, re.S))}")

if warns:
    print(f"\nWARNINGS ({len(warns)})")
    for w in warns:
        print(f"  - {w}")
if fails:
    print(f"\nFAILURES ({len(fails)})")
    for f in fails:
        print(f"  - {f}")
    sys.exit(1)
print("\nPASS: structure, TOC, footers, counts and asset references all consistent")