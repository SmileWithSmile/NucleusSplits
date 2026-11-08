"""Draw a 512x512 pixel-art stopwatch surrounded by the five Nucleus crystals.

The art is authored on a 64x64 grid and enlarged without smoothing. Pure standard library:
python art/icon.py
"""
import struct
import zlib
from pathlib import Path

N, SCALE = 64, 8
px = [[(0, 0, 0, 0)] * N for _ in range(N)]


def put(x, y, color):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = color


def rect(x1, y1, x2, y2, color):
    for y in range(y1, y2):
        for x in range(x1, x2):
            put(x, y, color)


def diamond(cx, cy, radius, light, base, dark):
    for y in range(cy - radius, cy + radius + 1):
        width = radius - abs(y - cy)
        for x in range(cx - width, cx + width + 1):
            color = light if x + y < cx + cy - 1 else dark if x + y > cx + cy + 2 else base
            put(x, y, color)
    put(cx - radius, cy, (30, 30, 40, 255))
    put(cx + radius, cy, (30, 30, 40, 255))
    put(cx, cy - radius, (30, 30, 40, 255))
    put(cx, cy + radius, (30, 30, 40, 255))


def circle(cx, cy, radius, color):
    for y in range(cy - radius, cy + radius + 1):
        for x in range(cx - radius, cx + radius + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= radius ** 2:
                put(x, y, color)


# Stopwatch crown and side button.
GOLD = (240, 165, 25, 255)
GOLD_LIGHT = (255, 205, 60, 255)
GOLD_DARK = (130, 75, 10, 255)
rect(27, 3, 37, 7, GOLD_DARK)
rect(29, 2, 35, 4, GOLD_LIGHT)
rect(29, 7, 35, 11, GOLD)
rect(49, 13, 54, 18, GOLD_DARK)
rect(48, 14, 52, 20, GOLD)

# Five gemstone crystals.
diamond(11, 20, 7, (230, 130, 255, 255), (174, 72, 218, 255), (86, 28, 130, 255))
diamond(53, 20, 7, (130, 205, 255, 255), (55, 140, 235, 255), (25, 65, 150, 255))
diamond(8, 44, 7, (255, 215, 100, 255), (242, 150, 35, 255), (145, 75, 15, 255))
diamond(32, 56, 7, (255, 255, 135, 255), (240, 215, 45, 255), (145, 120, 10, 255))
diamond(56, 44, 7, (150, 255, 155, 255), (55, 205, 90, 255), (20, 105, 45, 255))

# Stopwatch shadow, rim and face.
circle(34, 33, 22, (0, 0, 0, 100))
circle(32, 31, 22, GOLD_DARK)
circle(32, 31, 20, GOLD_LIGHT)
circle(32, 31, 17, (27, 31, 43, 255))
circle(32, 31, 15, (43, 50, 67, 255))

# Pixel ticks.
for x, y in ((32, 18), (32, 44), (19, 31), (45, 31), (23, 22), (41, 22), (23, 40), (41, 40)):
    rect(x - 1, y - 1, x + 2, y + 2, (225, 230, 240, 255))

# Clock hands: minute hand up, second hand towards the finish.
rect(31, 21, 34, 33, (245, 245, 250, 255))
for i in range(11):
    rect(32 + i, 31 + i // 2, 35 + i, 33 + i // 2, (90, 235, 125, 255))
circle(32, 31, 3, GOLD_LIGHT)


def write_png(path: Path):
    size = N * SCALE
    rows = bytearray()
    for y in range(size):
        rows.append(0)
        for x in range(size):
            rows.extend(px[y // SCALE][x // SCALE])

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(rows), 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


out = Path(__file__).resolve().parent.parent / "src/main/resources/assets/nucleussplits/icon.png"
write_png(out)
print(out)
