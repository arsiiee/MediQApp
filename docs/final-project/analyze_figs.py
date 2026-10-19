import re

content = open('MediQ_Final_Project.html', encoding='utf-8').read()
# Find all figure elements
figures = re.findall(r'<figure[^>]*>.*?</figure>', content, re.S)
print(f'Total figures: {len(figures)}')
for i, fig in enumerate(figures):
    src_match = re.search(r'src="([^"]+)"', fig)
    cap_match = re.search(r'<figcaption>(.*?)</figcaption>', fig, re.S)
    src = src_match.group(1) if src_match else 'N/A'
    cap = cap_match.group(1).strip()[:80] if cap_match else 'N/A'
    print(f'  Fig {i+1}: {src} | {cap}')
