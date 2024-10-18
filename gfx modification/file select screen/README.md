
# File select screen prompts
[insert Tilemap Studio screenshot]

This concerns the prompts on the file select screen such as はじめ, どこから？,
etc. Note that the text directly on the bookmarks like the player's name, the
page counter, the 回目, etc. are a separate change.

Because this particular graphic's true internal representation is not visible
in game, I strongly suggest you follow the "generate screenshot" instructions
for this graphic before you start editing tiles.

| GFX ID(s) | Tileset(s) | Tilemap(s) | Description |
| --------- | ---------- | ---------- | ----------- |
| 0x5B | $07E637 | $07E855 | file select prompts|

In terms of how to edit:
- Open the raw file for $07E637 in YY-CHR.
- Set format to `2bpp NES`.
- Enter the 3 palette values from the log for $07E2CD into the palette row of
  60-6F, stating from color number 61.
  - Right click on the number, and fill in the window with the 24-bit value.
  - I recommend setting color 60 to something other than black (#000000). For
    this writeup, I will use a color of green (#00FF00).

