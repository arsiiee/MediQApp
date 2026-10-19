import re

# Check the architecture SVG for its text colors
content = open('assets/architecture.svg', encoding='utf-8').read()
# Find text elements with their colors
print("=== architecture.svg ===")
# Find all text or tspan elements
for match in re.finditer(r'<(?:text|tspan)([^>]*)>([^<]*)', content):
    attrs = match.group(1)
    text = match.group(2).strip()
    if not text:
        continue
    # Extract fill from attrs or style
    fill = None
    fill_m = re.search(r'fill="([^"]+)"', attrs)
    if fill_m:
        fill = fill_m.group(1)
    style_m = re.search(r'style="[^"]*fill:([^;"\s]+)', attrs)
    if style_m:
        fill = style_m.group(1)
    if fill and fill.lower() not in ('#000', '#000000', 'black', 'none', 'inherit', 'currentcolor'):
        print(f"  [{fill}] {text[:60]}")

print("\nTotal: done")
