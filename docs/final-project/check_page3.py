import fitz

doc = fitz.open('MediQ_Final_Project.pdf')

# Compare colors: page 3 has intro text - check a typical paragraph span
page = doc[2]  # page 3 (0-indexed)
blocks = page.get_text('dict')['blocks']
for block in blocks:
    if block.get('type') == 0:
        for line in block.get('lines', []):
            for span in line.get('spans', []):
                color = span.get('color', 0)
                text = span.get('text', '').strip()
                if text:
                    r = (color >> 16) & 0xFF
                    g = (color >> 8) & 0xFF
                    b = color & 0xFF
                    print(f"rgb({r},{g},{b}) hex=#{color:06x}: {repr(text[:60])}")
