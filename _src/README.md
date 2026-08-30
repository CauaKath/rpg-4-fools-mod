# Release notes site

The site at <https://cauakath.github.io/rpg-4-fools-mod/> is generated from here.

    python3 _src/build.py pages      # writes _src/dist, which is what this branch serves
    python3 _src/build.py artifact   # one self-contained file, every asset a data URI

`content/` holds one HTML fragment per version plus the index; `styles.css` is shared by
both targets. Sprites are read from `src/main/resources/assets/rpg4fools/textures` on the
mod branches, and the three fonts from the system font directory - both by absolute path
at the top of `build.py`, so adjust those if you build this somewhere else.

Adding a version: write `content/<version>.html`, add it to `VERSIONS` in `build.py`, add
its row to `content/index.html`, rebuild, and copy `dist/` over this branch's root.

`legacy/<version>/<kind>/<name>.png` freezes a sprite for one version's page, and wins over
the live texture directory when that page is built. Freeze the sprites a released page uses
before redrawing them on the mod branch - otherwise an old page silently adopts the new art,
and a page that shows a sprite the mod later deleted fails the build outright. Frozen sprites
are written to `<version>/sprites/` rather than the shared `assets/sprites/`, so two versions
can ship the same file name with different art.

Jekyll leaves `_`-prefixed directories out of the published site, so nothing here is served.
