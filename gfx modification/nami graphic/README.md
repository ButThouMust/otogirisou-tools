# Nami graphic
[Insert Tilemap Studio screenshot here]

This is a mild spoiler about one certain moment in the game. If you take an
original Japanese ROM and change nine bytes at 0x12627 from `[08 20 ... C5]`
to three copies of `[69 BA B5]`, you can (from a new save file) view a certain
image that contains two kanji in it. I personally felt it was important to
translate this particular moment into English.

It is the name of the protagonist's girlfriend, 奈美 "Nami". In the context of
the story, the name is written in big red letters on the door of a greenhouse in
the mansion's back yard.

This particular graphic is not as straightforward to translate to English
because it is one of a handful of graphics that use Mode 7. To summarize the
situation in one sentence: the tiles themselves are 2bpp (black and three shades
of red) but are stored in a 5bpp container (direct result of RLE decompression)
that is then padded to 8bpp. For our purposes, we are not concerned with the
process of then converting the 8bpp format tiles to Mode 7 format.

# Special steps for generating a screenshot

This is one case where I recommend just taking a screenshot of a tile viewer
instead of following the "generate screenshot" steps. If you do want to follow
them, however, there are some more things to do for this particular graphic:
- Decompress graphics data from ROM (taken care of in decompressor).
  The resulting file is `$07A4EA TILE.bin`.
- Pad and interleave the 5bpp container to 8bpp (taken care of in decompressor).
  The resulting file is `$07A4EA SNES format tiles.8bpp`.
- (Optional) Strip out the padding in the 8bpp container to 2bpp, by removing
  the last 0x30 [00] bytes of every group of 0x40 bytes.
  - I suggest you automate this in some way. The resulting file should be exactly 1376 (0x560) bytes, one quarter of the 8bpp file's 5504 (0x1580).
- Open the 2bpp file in YY-CHR in the `2BPP GB` format.
- Enter in the three palette values from the palette log.

The result in YY-CHR should be:

[insert YY-CHR screenshot]

From here, you can export the tiles and load it and the SNES format tilemap as
normal in Tilemap Studio.

# How to edit

Here, I recommend creating a new graphic from scratch, in a normal image editor,
that writes Nami's name in whatever language you are translating to.
- Because of the Mode 7 effect, try to preserve the dimensions of the graphic
  (0xB x 0x14). I have no promises for what will happen if you do not.
  If your graphic is smaller, you can just pad the empty space with blank tiles.
- Not counting the transparent background, you must only use the three colors
  $6B0000, $A50000, and $C60000 in the new graphic.

Take the new graphic and put it through Tilemap Studio's Image to Tiles feature
with the following options:
- Input: your new graphic as a PNG or BMP
- Output: whatever you like
- Check ON `Avoid extra blank tiles at the end`
- Format: `Plain tiles`
- Start at ID `$000`
- Check OFF `Blank tiles use ID $___`
  - This is assuming the transparency color is not white.
- Check OFF `Palette`


