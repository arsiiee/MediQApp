#!/usr/bin/env python
"""
Two-pass build for MediQ_Final_Project.pdf.

Pass 1  render the HTML to PDF
        read the PDF back and work out which page each section starts on
        write those page numbers into the table of contents
Pass 2  render again, now with a real table of contents
        stamp a running footer and page numbers onto every page

Chromium cannot number printed pages from CSS (@page margin boxes are
unsupported and position:fixed does not repeat reliably), so the footer is
drawn with reportlab afterwards.

    python build.py
"""

import re
import subprocess
import sys
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas

HERE = Path(__file__).resolve().parent
HTML = HERE / "MediQ_Final_Project.html"
PDF = HERE / "MediQ_Final_Project.pdf"
RAW = HERE / "_pass1.pdf"
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
PROFILE = r"C:\Users\You\AppData\Local\Temp\opencode\edge-pdf-profile"

FOOTER_LEFT = "MediQ  \u00b7  Final Project  \u00b7  Integrative Programming and Technologies 1"

# Running header/footer on every page. Off: the submission carries neither.
# Set True to get back the rule line, the title on the left and "Page n of m"
# on the right. The table of contents keeps its own page numbers either way.
ADD_FOOTER = False

# Each TOC target needs every fragment present on the page that starts it.
# Matching on more than one fragment keeps the Contents page from matching a
# section it merely lists.
TARGETS = {
    "s3":  ["Introduction and Problem Statement", "The situation"],
    "s4":  ["Objectives of the Project", "Each objective is written"],
    "s5":  ["Target Users", "Primary user"],
    "s6":  ["System Features and Functionalities", "How the system is put together"],
    "s7":  ["User Interface Designs", "Principles"],
    "s8":  ["Database Design", "Entity relationship diagram"],
    "s9":  ["API Integrations and Their Purpose", "REST API"],
    "s10": ["Expected Benefits and Impact", "What kind of claim these are"],
    "s11": ["Conclusion", "MediQ was built to remove"],
    "sa":  ["Appendix A", "System Architecture and Technology Stack"],
    "sb":  ["Appendix B", "Running and Verifying the System"],
    "sc":  ["Appendix C", "Source Code Map"],
    "sr":  ["References", "OWASP Foundation"],
}


def render(src_html: Path, out_pdf: Path) -> None:
    if out_pdf.exists():
        out_pdf.unlink()
    subprocess.run(
        [
            EDGE,
            "--headless=new",
            "--disable-gpu",
            "--no-sandbox",
            f"--user-data-dir={PROFILE}",
            "--no-pdf-header-footer",
            f"--print-to-pdf={out_pdf}",
            src_html.resolve().as_uri(),
        ],
        check=True,
        capture_output=True,
    )
    if not out_pdf.exists():
        sys.exit(f"render failed: {out_pdf} was not written")


def locate_sections(pdf: Path) -> dict[str, int]:
    """Map each TOC key to the 1-based page its section starts on."""
    texts = [(p.extract_text() or "") for p in PdfReader(str(pdf)).pages]
    found = {}
    # Skip the cover and the contents page.
    for key, fragments in TARGETS.items():
        for i, text in enumerate(texts[2:], start=3):
            if all(f in text for f in fragments):
                found[key] = i
                break
        else:
            sys.exit(f"could not locate section {key!r}")
    return found


def write_toc(pages: dict[str, int]) -> None:
    html = HTML.read_text(encoding="utf-8")
    missing = [k for k in TARGETS if k not in pages]
    if missing:
        sys.exit(f"no page found for {missing}")

    for key, page in pages.items():
        pattern = re.compile(
            r'(<td class="p" data-k="%s">)(.*?)(</td>)' % re.escape(key)
        )
        html, n = pattern.subn(r"\g<1>%d\g<3>" % page, html)
        if n != 1:
            sys.exit(f"TOC entry {key!r} matched {n} times, expected 1")
    HTML.write_text(html, encoding="utf-8")
    print("  TOC page numbers written: " + ", ".join(f"{k}={v}" for k, v in sorted(pages.items())))


def stamp(src: Path, dst: Path) -> int:
    """Draw a running footer and page number onto every page."""
    from io import BytesIO

    reader = PdfReader(str(src))
    width, height = A4
    left = 15 * 72 / 25.4            # left page margin
    right = width - 15 * 72 / 25.4   # right page margin
    baseline = 20                   # ~7 mm above the bottom edge
    total = len(reader.pages)
    writer = PdfWriter()

    for i, page in enumerate(reader.pages, start=1):
        buf = BytesIO()
        c = canvas.Canvas(buf, pagesize=A4)
        c.setFont("Helvetica", 7.4)
        c.setFillGray(0.62)
        c.setStrokeGray(0.88)
        c.setLineWidth(0.4)
        c.line(left, baseline + 10, right, baseline + 10)
        c.drawString(left, baseline, FOOTER_LEFT)
        c.drawRightString(right, baseline, f"Page {i} of {total}")
        c.save()
        buf.seek(0)
        page.merge_page(PdfReader(buf).pages[0])
        writer.add_page(page)

    with open(dst, "wb") as fh:
        writer.write(fh)
    return total


def main() -> None:
    print("pass 1: render")
    render(HTML, RAW)

    print("pass 1: locate sections")
    pages = locate_sections(RAW)

    print("pass 1: write the table of contents")
    write_toc(pages)

    print("pass 2: render")
    if ADD_FOOTER:
        render(HTML, RAW)
        print("pass 2: stamp the footer")
        total = stamp(RAW, PDF)
        RAW.unlink(missing_ok=True)
    else:
        render(HTML, PDF)
        total = len(PdfReader(str(PDF)).pages)

    print(f"done: {PDF.name}, {total} pages, "
          f"{PDF.stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
