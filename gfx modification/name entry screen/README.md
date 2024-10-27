# Name entry screen
![Tilemap Studio screenshot](name%20entry%20screen%20-%20Tilemap%20Studio.png)

| GFX ID | Tileset | Tilemap | Palette | Description |
| ------ | ------- | ------- | ------- | ----------- |
| 0x5C   | $07DD87 | $07E2F3 | $07E2CD | name entry screen |

You will need to translate the buttons in the bottom right of the screen.
The two in the middle each read セレクト "select" and スペース "space".

Because they all use unique tiles, I was able to just edit the tiles for the
buttons "in-place", directly in a tile editor. Because of the limited screen
space, I decided to abbreviate them as `SEL` and `SPC`. I also decided to change
the backspace arrow into the text `DEL`.

In my case, I was actually able to get away with just modifying the tileset in
YY-CHR (use the raw decompression output, not the SNES format tiles) and leaving
the tilemap alone.

One tip with YY-CHR here: you can use the Pattern Editor (click `Edit...` below
the tileset) to make the 2x2 buttons line up correctly.

![YY-CHR pattern editor example](name%20entry%20screen%20-%20YY-CHR%2010,2H%20pattern.png)

# Translating `NAME` in the top left
If you want to translate the game into another language besides English, you may
also want to translate `NAME` at the top left. Anticipated difficulties:
- Keeping the correct dithering below the `N` tile
- Using the correct "shadowing" effects and colors on the letters
- Extending the space right if the translated word is longer, like `NOMBRE` for Spanish
  - You get four tiles of space to the right of the `E` in `NAME`.

Here, you may want to just generate a screenshot of this screen, draw the
replacement for `NAME` over it, and use Tilemap Studio and superfamiconv to get
a new tileset and tilemap, which you can then compress. See the other folders'
readmes for more info.
