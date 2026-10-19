import re

svgs = ['architecture.svg', 'ui-01-signin.svg', 'ui-02-register-otp.svg', 'ui-03-doctors.svg',
        'ui-04-doctor-slots.svg', 'ui-05-booking.svg', 'ui-06-appointment.svg', 'erd.svg']
for svg in svgs:
    path = f'assets/{svg}'
    content = open(path, encoding='utf-8').read()
    colors = re.findall(r'fill="([^"]+)"', content)
    stroke_colors = re.findall(r'stroke="([^"]+)"', content)
    style_fills = re.findall(r'fill:([^;}\s"]+)', content)
    style_colors = re.findall(r'(?<![a-z-])color:([^;}\s"]+)', content)
    all_colors = set(colors + stroke_colors + style_fills + style_colors)
    non_none = sorted([c for c in all_colors if c.lower() not in ('none', 'transparent', 'inherit', 'currentcolor')])
    print(f'{svg}:')
    print(f'  Colors: {non_none}')
