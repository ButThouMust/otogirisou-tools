
# File select screen prompts
![Tilemap Studio screenshot](file%20select%20prompts%20-%20Tilemap%20Studio.png)

```
+------------+------------------+--------------+
|  Original  | Lit. translation | Patch uses:  |
+------------+------------------+--------------+
| はじめる　　| Begin            | Start        | Existing file
| しおりをけす| Delete bookmark  | Delete       |
| やめる　　　| Quit             | Cancel       |
+------------+------------------+--------------+
| はじめる　　| Begin            | Start        | New file
| やめる　　　| Quit             | Cancel       |
+------------+------------------+--------------+
| どこから？　| From where?      | Which?       | Pick "Start"
| つづき　　　| Continue         | Resume       | on existing
| はじめ　　　| Beginning        | Restart      | file
+------------+------------------+--------------+
| いいですか？| Are you sure?    | Delete?      | Pick "Delete"
| いいえ　　　| No               | Cancel       |
| はい　　　　| Yes              | Confirm      |
+------------+------------------+--------------+
```

| GFX ID | Tileset | Tilemap | Palette | Description |
| ------ | ------- | ------- | ------- | ----------- |
| 0x5B   | $07E637 | $07E855 | $07E9DE | file select prompts|

This concerns the prompts on the file select screen, which are listed above.
Note that the text directly on the bookmarks like the player's name, the page
counter, the 回目, etc. are a separate change.

Because this particular graphic's true internal representation is not visible
in game, I strongly suggest you follow the "generate screenshot" instructions
for this graphic (set background color to white) before you start editing or
creating tiles.

# How to edit
Once you have generated a screenshot of the tilemap, you can open it in a
normal image editor like GIMP. Clear out the text and try to translate them in
such a way as to minimize tile usage. The original tileset uses 0x2B (43)
non-empty tiles, of which 24 are replaceable in that they contain kana or,
interestingly, the tops of the dakuten marks for づ, で, and ど.

When you are done, put it through Tilemap Studio to generate the binary tilemap
and the PNG tileset. Use the settings:
- Enable `Avoid extra blank tiles at the end`
- Tilemap Format is `SNES tiles + attributes`
- Start at ID $000
- Blank tiles use ID $000
- Disable the palette section

Ensure that the PNG tileset has the blank tile at the start, and run that PNG
through superfamiconv to generate the tileset in 2bpp binary format. Next, remove
the first 0x10 bytes from the binary tileset (should be all [00]) to delete the
empty tile that the in-game decompressor will add on its own.

Convert both the binary tileset and tilemap to the format expected by the compressor,
and compress them.

# Some history with this translation edit

For a long time, I had used a tileset that I and another user on the RHDN
Discord, MLagaffe, had created to fit within exactly as many tiles as the
original. It was not perfect in terms of character spacing but did fit into 43
tiles (this was harder than you'd think it'd be) and fit into the space for the
original compressed graphics.

Later, when I was about to release v1.0 of the patch, I wanted to try to improve
this tileset. Personally, I found fitting in the new compressed tileset (in
terms of compressed bytes, not tiles) to be somewhat troublesome. I wanted to
use superfamiconv to see if it would help at all with the translation process
for this project. Since it did, I rewrote the graphics translation documentation
to incorporate it.

This being said, I was slightly annoyed that superfamiconv assigned different
palette indices to the colors than the original palette in the Japanese game.
Moreover, when I changed it to fit the "correct" palette, the tileset would not
fit into the original space.

This was when I discovered, quite by accident, that if I took the unmodified
binary tileset that superfamiconv generated (with the "wrong" palette) and
compressed it, it took *less* space than the tileset with the "correct" palette.
More surprisingly, it *did* fit into the original space. So I decided to go with
this new palette.
