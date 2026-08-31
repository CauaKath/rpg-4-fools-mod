"""Derives the sticked tomato's section sprites from the plain tomato's.

A tomato growing up a trellis is one plant, not a stack of bushes, so its sections have to meet: the
stalk leaving the top of one block has to line up with the stalk entering the bottom of the next.
Drawing three sprites by hand and hoping they line up is how that goes wrong, so they are derived
instead - every section is the plain tomato of the same age with a small, deliberate edit.

Row 0 of a block sits against row 15 of the block above it, because the models span y -1 to 15. That
is the whole trick: a section that continues upward gets the original's own bottom two rows copied
into its top two, and the joins then line up pixel for pixel at every age, for free.

    dead     the adult recoloured to straw and thinned out, for a plant a season killed. Three of
             them - foot, middle, crown - so a dead column is not one picture repeated, for the
             reason the living sections are not. The crown is cut back at the top the way the living
             one is, and stands in for a lone dead plant too
    bottom   roots at the foot, stalk carried out of the top
    middle   stalk carried through both ends, foliage mirrored so it is not the same picture twice
    top      the crown cleared back, so the plant tapers to a tip instead of being cut off

The top section also gets young ages, because a plant climbing a stick puts up a shoot and fills it
in rather than arriving whole. Those are the plain young sprites, which already grow from the bottom
of their block upward, grafted onto the mature stalk the section below ends on - so the shoot reads
as coming out of the plant rather than floating above it.

Bottom and middle need only ages 4 to 7. Neither exists until something is growing above it, and
nothing grows above a plant that is not yet an adult.

Run from the repository root:

    python3 tools/derive_sticked_tomato.py
"""

import pathlib
import struct
import zlib

BLOCKS = pathlib.Path("src/main/resources/assets/rpg4fools/textures/block")

SOURCE = "tomato_crop_stage{age}.png"
OUTPUT = "tomato_crop_stick_{part}_stage{age}.png"
DEAD_OUTPUT = "crop_stick_dead{suffix}.png"

AGES = (4, 5, 6, 7)

# The ages a new section passes through while it fills in. Only the top is ever this young.
SHOOT_AGES = (0, 1, 2, 3)

# The age whose foot every section joins on to, which is the age the plant below is held at while
# the shoot above it grows.
JOIN_AGE = 4

CLEAR = (0, 0, 0, 0)
STEM_LIGHT = (0x58, 0x40, 0x19, 255)
STEM_DARK = (0x3A, 0x2A, 0x12, 255)
ROOT = (0x4A, 0x35, 0x16, 255)

# The plant's own greens and browns. Fruit and flowers are never painted over, so a root flare cannot
# eat a tomato that happened to be growing low on the plant.
FOLIAGE = {
    (0x2F, 0x63, 0x29), (0x24, 0x49, 0x1F), (0x43, 0x7F, 0x33),
    (0x82, 0xBC, 0x57), (0x5C, 0x9C, 0x40), (0x1D, 0x3C, 0x1B),
    (0x58, 0x40, 0x19), (0x3A, 0x2A, 0x12),
}

# Row 15 is inside the farmland and never seen, so the roots go on the two rows above it.
ROOT_ROWS = (13, 14)
ROOT_SPREAD = (3, 4, 11, 12)

# How much of the crown the top section loses. Five rows leaves the plant's own taper as its tip.
CROWN = 5

# The straw the mod's dead crop is drawn in, so a withered trellis matches a withered field.
STRAW_PALE = (0xD0, 0xBA, 0x80, 255)
STRAW_LIGHT = (0xAC, 0x92, 0x62, 255)
STRAW_MID = (0x7C, 0x63, 0x3F, 255)
STRAW_DARK = (0x56, 0x43, 0x2B, 255)

# Green by green, brightest to darkest. Withering is a recolour, not a redraw: the plant that died
# should be recognisably the plant that was there.
WITHERED = {
    (0x82, 0xBC, 0x57): STRAW_PALE,
    (0x5C, 0x9C, 0x40): STRAW_LIGHT,
    (0x43, 0x7F, 0x33): STRAW_LIGHT,
    (0x2F, 0x63, 0x29): STRAW_MID,
    (0x24, 0x49, 0x1F): STRAW_DARK,
    (0x1D, 0x3C, 0x1B): STRAW_DARK,
    (0x58, 0x40, 0x19): STRAW_MID,
    (0x3A, 0x2A, 0x12): STRAW_DARK,
}

# Roughly a third of the foliage is dropped, so what is left reads as a plant that has thinned out
# rather than one that merely changed colour. Scattered by position rather than by counting pixels:
# dropping every third one leaves a regular mesh, which reads as a texture rather than as decay.
#
# Fruit and flowers go entirely. They are the first thing a dying plant loses, and the wrong thing to
# leave hanging on a bare trellis.
THINNING = 10
THINNED_BELOW = 3

# A second scatter for the crown, so mirroring alone does not leave it identical to the middle.
CROWN_SALT = 4


def read_png(path):
    data = path.read_bytes()
    position, pixels, header = 8, b"", None

    while position < len(data):
        length = struct.unpack(">I", data[position:position + 4])[0]
        kind = data[position + 4:position + 8]
        body = data[position + 8:position + 8 + length]

        if kind == b"IHDR":
            header = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            pixels += body

        position += 12 + length

    width, height, depth, colour = header[0], header[1], header[2], header[3]

    if (depth, colour) != (8, 6):
        raise ValueError(f"{path} is not 8 bit RGBA")

    raw = zlib.decompress(pixels)
    stride = width * 4
    rows, previous, cursor = [], bytearray(stride), 0

    for _ in range(height):
        filtering = raw[cursor]
        cursor += 1
        line = bytearray(raw[cursor:cursor + stride])
        cursor += stride

        for i in range(stride):
            left = line[i - 4] if i >= 4 else 0
            up = previous[i]
            corner = previous[i - 4] if i >= 4 else 0

            if filtering == 1:
                line[i] = (line[i] + left) & 0xFF
            elif filtering == 2:
                line[i] = (line[i] + up) & 0xFF
            elif filtering == 3:
                line[i] = (line[i] + (left + up) // 2) & 0xFF
            elif filtering == 4:
                a, b, c = abs(up - corner), abs(left - corner), abs(left + up - 2 * corner)
                line[i] = (line[i] + (left if a <= b and a <= c else up if b <= c else corner)) & 0xFF

        rows.append([tuple(line[x * 4:(x + 1) * 4]) for x in range(width)])
        previous = line

    return rows


def write_png(path, rows):
    height, width = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in rows)

    def chunk(kind, body):
        payload = kind + body
        return struct.pack(">I", len(body)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)

    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def copy(rows):
    return [list(row) for row in rows]


def is_plant(pixel):
    return pixel[3] != 0 and pixel[:3] in FOLIAGE


def continue_upward(rows, source):
    """Carries the stalk out of the top of the block, aligned with whatever sits above."""
    rows[0] = list(source[15])
    rows[1] = list(source[14])
    return rows


def add_roots(rows):
    """Thickens the foot into roots, leaving fruit alone."""
    for x in range(16):
        if is_plant(rows[ROOT_ROWS[1]][x]):
            rows[ROOT_ROWS[1]][x] = ROOT if x in ROOT_SPREAD else STEM_DARK

    for x in (3, 11):
        if rows[ROOT_ROWS[1]][x][3] == 0:
            rows[ROOT_ROWS[1]][x] = ROOT

    for x in (4, 5, 12, 13):
        if is_plant(rows[ROOT_ROWS[0]][x]):
            rows[ROOT_ROWS[0]][x] = STEM_LIGHT

    return rows


def clear_crown(rows):
    """Ends the plant, so the top section reads as a tip rather than as more plant cut off."""
    for y in range(CROWN):
        rows[y] = [CLEAR] * 16

    return rows


def mirror_body(rows):
    """Flips the foliage so a three section plant is not one picture repeated.

    The two join rows and the two rows above the foot stay where they are, which is what keeps the
    stalk lined up where the sections meet.
    """
    for y in range(2, 14):
        rows[y] = list(reversed(rows[y]))

    return rows


def graft(rows, join):
    """Stands a young shoot on the stalk the section below ends on.

    A shoot is the plain sprite of the same age, which already grows from the bottom of its block
    upward. Only its foot is replaced, so it rises out of the mature stalk instead of hanging over
    a gap.
    """
    rows[14] = list(join[14])
    rows[15] = list(join[15])
    return rows


def scatter(x, y, salt):
    """A fixed, unlovely hash. Enough to break up a grid, and the same every time it is run."""
    return ((x * 29 + y * 17) ^ (x * 13 + y * 7) ^ (x * y) ^ salt) % THINNING


def dry(rows):
    """Recolours the plant to straw and drops anything it fruited, without thinning it yet."""
    for y in range(16):
        for x in range(16):
            pixel = rows[y][x]

            if pixel[3] == 0:
                continue

            rows[y][x] = WITHERED.get(pixel[:3], CLEAR)

    return rows


def thin(rows, salt):
    """Scatters gaps through what is left, so it reads as a plant that has thinned rather than one
    that merely changed colour."""
    for y in range(16):
        for x in range(16):
            if rows[y][x][3] != 0 and scatter(x, y, salt) < THINNED_BELOW:
                rows[y][x] = CLEAR

    return rows


def join_rows(rows, source):
    """Gives every dead section the same foot and the same crown.

    A section joins the one above it by its top two rows meeting that one's bottom two, so the three
    variants have to agree about those four rows exactly. Only the body between them varies.
    """
    rows[14] = list(source[14])
    rows[15] = list(source[15])
    rows[0] = list(rows[15])
    rows[1] = list(rows[14])
    return rows


def main():
    join = read_png(BLOCKS / SOURCE.format(age=JOIN_AGE))

    dried = dry(copy(join))

    foot = join_rows(thin(copy(dried), 0), thin(copy(dried), 0))
    middle = mirror_body(copy(foot))
    # Cut back at the top like the living crown, because nothing grows above it. That also drops the
    # two rows that carry the stalk upward, which is right: there is nothing up there to carry it to.
    crown = clear_crown(mirror_body(join_rows(thin(copy(dried), CROWN_SALT), foot)))

    for suffix, rows in (("", foot), ("_middle", middle), ("_top", crown)):
        target = BLOCKS / DEAD_OUTPUT.format(suffix=suffix)
        write_png(target, rows)
        print(f"wrote {target}")

    for age in SHOOT_AGES:
        shoot = graft(copy(read_png(BLOCKS / SOURCE.format(age=age))), join)
        target = BLOCKS / OUTPUT.format(part="top", age=age)
        write_png(target, shoot)
        print(f"wrote {target}")

    for age in AGES:
        source = read_png(BLOCKS / SOURCE.format(age=age))

        sections = {
            "bottom": add_roots(continue_upward(copy(source), source)),
            "middle": mirror_body(continue_upward(copy(source), source)),
            "top": clear_crown(copy(source)),
        }

        for part, rows in sections.items():
            target = BLOCKS / OUTPUT.format(part=part, age=age)
            write_png(target, rows)
            print(f"wrote {target}")


if __name__ == "__main__":
    main()
