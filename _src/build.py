"""Builds the release notes site, and the single file copy of the current notes.

Two targets share one set of content fragments and one stylesheet:

  pages     a multi page site - an index of versions, one page each - with the
            fonts and sprites written out as files the browser caches once.
  artifact  the current version's page alone, with every font and sprite inlined
            as a data URI, for hosts that refuse to fetch anything.
"""

import base64
import pathlib
import re
import shutil
import sys

HERE = pathlib.Path(__file__).parent
TEXTURES = pathlib.Path(
    "/home/kath/projects/rpg-4-fools-mod/src/main/resources/assets/rpg4fools/textures")
FONTS = {
    "display": pathlib.Path("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"),
    "body": pathlib.Path("/usr/share/fonts/truetype/ubuntu/UbuntuSans[wdth,wght].ttf"),
    "mono": pathlib.Path("/usr/share/fonts/truetype/ubuntu/UbuntuSansMono[wght].ttf"),
}
FONT_FILES = {"display": "almanac-display.ttf", "body": "field-sans.ttf", "mono": "field-mono.ttf"}

REPO = "https://github.com/CauaKath/rpg-4-fools-mod"
SITE = "https://cauakath.github.io/rpg-4-fools-mod/"

# Newest first, which is the order the index lists them in.
VERSIONS = [
    ("0.3.0", "Seasons decide what grows",
     "Six new plants with their own calendar, wild berry bushes, right click harvest, "
     "and fields that sow themselves again."),
    ("0.2.0", "The world takes on the season",
     "Seasonal grass, foliage, sky and fog, winter snowfall that melts again, and sub-seasons."),
    ("0.1.1", "The overlay knows when to leave",
     "The season overlay is hidden in creative and spectator."),
    ("0.1.0", "A calendar, and somewhere to read it",
     "Months mapped to seasons, kept per world and shown on the HUD, with holidays of their own."),
]
CURRENT = VERSIONS[0][0]

ASSET = re.compile(r"@@(font|block|item|gui)/([^@]+)@@")


def texture(kind, name):
    path = TEXTURES / kind / (name + ".png")

    if not path.exists():
        raise SystemExit("missing texture: " + str(path))

    return path


def inline(match):
    """Every asset as a data URI, for the single file build."""
    kind, name = match.group(1), match.group(2)

    if kind == "font":
        return "data:font/ttf;base64," + base64.b64encode(FONTS[name].read_bytes()).decode()

    data = texture(kind, name).read_bytes()

    return "data:image/png;base64," + base64.b64encode(data).decode()


def linked(prefix):
    """Every asset as a file the page points at, for the site build."""

    def replace(match):
        kind, name = match.group(1), match.group(2)

        if kind == "font":
            # The stylesheet itself lives in assets/, so its font paths are relative to that.
            return "fonts/" + FONT_FILES[name]

        return prefix + "assets/sprites/" + kind + "/" + name + ".png"

    return replace


def sprites_used(text):
    return {(m.group(1), m.group(2)) for m in ASSET.finditer(text) if m.group(1) != "font"}


def content(name):
    return (HERE / "content" / (name + ".html")).read_text().strip()


def footer(version):
    left = "RPG 4 Fools" if version is None else "RPG 4 Fools " + version
    notes = "" if version is None else "\n    <span>Block sprites shown at 4×, items at 2×</span>"

    return (
        '  <footer>\n'
        '    <span>' + left + '</span>\n'
        '    <span>Minecraft 1.20.6 · Fabric</span>' + notes + '\n'
        '    <span><a href="' + REPO + '">Source on GitHub</a></span>\n'
        '  </footer>'
    )


def page(title, description, body, css_href, version=None, prefix=""):
    topbar = ""

    if version is not None:
        topbar = ('  <div class="topbar">\n'
                  '    <a class="back" href="' + prefix + '">&larr; All versions</a>\n'
                  '  </div>\n\n')

    return (
        '<!doctype html>\n'
        '<html lang="en">\n'
        '<head>\n'
        '<meta charset="utf-8">\n'
        '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
        '<title>' + title + '</title>\n'
        '<meta name="description" content="' + description + '">\n'
        '<meta property="og:title" content="' + title + '">\n'
        '<meta property="og:description" content="' + description + '">\n'
        '<meta property="og:type" content="website">\n'
        '<link rel="stylesheet" href="' + css_href + '">\n'
        '</head>\n'
        '<body>\n'
        '<div class="page">\n\n' + topbar + '  ' + body + '\n\n' + footer(version) + '\n\n'
        '</div>\n'
        '</body>\n'
        '</html>\n'
    )


RESET = """*, *::before, *::after { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; }
body { margin: 0; }
img, svg { display: block; max-width: 100%; }
h1, h2, h3, p, figure, blockquote, ol, ul { margin: 0; }
table { border-collapse: collapse; }

"""


def build_pages(dist):
    if dist.exists():
        shutil.rmtree(dist)

    (dist / "assets" / "fonts").mkdir(parents=True)
    (dist / "assets" / "sprites").mkdir(parents=True)

    css = HERE.joinpath("styles.css").read_text()
    (dist / "assets" / "site.css").write_text(RESET + ASSET.sub(linked(""), css))

    for key, name in FONT_FILES.items():
        shutil.copy(FONTS[key], dist / "assets" / "fonts" / name)

    written = 0

    for version, title, summary in VERSIONS:
        body = content(version)

        for kind, name in sprites_used(body):
            target = dist / "assets" / "sprites" / kind
            target.mkdir(exist_ok=True)
            shutil.copy(texture(kind, name), target / (name + ".png"))
            written += 1

        out = dist / version / "index.html"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(page(
            "RPG 4 Fools " + version,
            summary,
            ASSET.sub(linked("../"), body),
            "../assets/site.css",
            version=version,
            prefix="../",
        ))

    index = content("index")
    (dist / "index.html").write_text(page(
        "RPG 4 Fools",
        "Release notes for RPG 4 Fools, a Minecraft mod that gives the world seasons.",
        ASSET.sub(linked(""), index),
        "assets/site.css",
    ))

    print("pages:", len(VERSIONS) + 1, "html,", written, "sprites ->", dist)


def build_artifact(out):
    css = ASSET.sub(inline, HERE.joinpath("styles.css").read_text())
    body = ASSET.sub(inline, content(CURRENT))

    out.write_text(
        "<title>RPG 4 Fools " + CURRENT + "</title>\n\n"
        "<style>\n" + css + "</style>\n\n"
        '<div class="page">\n\n'
        '  <div class="topbar">\n'
        '    <a class="back" href="' + SITE + '">&larr; All versions</a>\n'
        '  </div>\n\n  ' + body + "\n\n" + footer(CURRENT) + "\n\n</div>\n"
    )
    print("artifact:", len(out.read_text()) // 1024, "KB ->", out)


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "pages"

    if target == "pages":
        build_pages(HERE / "dist")
    elif target == "artifact":
        build_artifact(HERE / "artifact.html")
    else:
        raise SystemExit("usage: build.py [pages|artifact]")
