# Name entry screen
[insert Tilemap Studio screenshot]

| GFX ID(s) | Tileset(s) | Tilemap(s) | Description |
| --------- | ---------- | ---------- | ----------- |
| 0x5C | $07DD87 | $07E2F3 | name entry screen |

You will need to translate the buttons in the bottom right of the screen.
The two in the middle each read セレクト "select" and スペース "space".

Because they all use unique tiles, I was able to just edit the tiles for the
buttons "in-place", directly in a tile editor. Because of the limited space,
however, I decided to abbreviate them as `SEL` and `SPC`. I also decided to
change the backspace arrow into the text `DEL`.

In my case, I was actually able to get away with just modifying the tileset
(use the raw decompression output, not the SNES format tiles) and leaving the
tilemap alone.

If you want to translate the game into another language besides English, you may
also want to translate the `NAME` at the top left. Anticipated difficulties:
- Keeping the correct dithering below the `N`
- Using the correct "shadowing" effects and colors on the letters
- Extending the space right if the translated word is longer, like `NOMBRE` for Spanish
