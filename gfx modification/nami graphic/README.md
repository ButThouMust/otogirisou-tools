# Nami graphic
![Tilemap Studio screenshot](trimmed%20nami%20graphic%20tiles%20-%20tilemap%20studio.png)

| GFX ID | Tileset | Tilemap | Palette | Description |
| ------ | --------| --------| ------- | ----------- |
| 0x7B   | $07A4EA | $07AA34 | $07AA26 | Nami graphic |

This is a mild spoiler about one certain moment in the game. If you take an
original Japanese ROM and change nine bytes at 0x12627 from `[08 20 ... C5]`
to three copies of `[69 BA B5]`, you can (from a new save file) view a certain
image that contains two kanji in it. I personally felt it was important to
translate this particular moment into English.

It is the name of the protagonist's girlfriend, 奈美 "Nami". In the context of
the story, the name is written in big red letters on the door of a greenhouse in
the mansion's back yard.

For a while, translating this particular graphic was not as straightforward
because of a unique quirk with how the tileset is stored. In one sentence: the
tiles themselves are 2bpp but are stored in a 5bpp container (direct result of
RLE decompression) that is then padded to 8bpp. The 8bpp format tiles are then
converted to Mode 7 format, but for our purposes, that process does not concern us.

Thankfully, I was able to use my knowledge of the graphics system to figure out
a way to avoid this 5bpp container nonsense. Now, you no longer have to pad your
translated graphic to 5bpp before you can recompress and reinsert it. You just
need to un-interleave the 2bpp bitplanes, and recompress.

According to my notes, I had theorized this "fix" as late as June 2023 but
didn't try it out for real until filling out this readme on October 21, 2024,
after releasing v1.0 of the patch. This offered nothing new to the player and
didn't warrant a whole new patch release on its own. It was included in patch
version 1.1.

# Special steps for generating a screenshot
This is one case where I recommend just taking a screenshot of a tile viewer
instead of following the "generate screenshot" steps. If you do want to follow
them, however, there are some more things to do for this particular graphic:
- Decompress graphics data from ROM (taken care of in decompressor).
  The resulting file is `$07A4EA TILE.bin`.
- Pad and interleave the 5bpp container to 8bpp (taken care of in decompressor).
  The resulting file is `$07A4EA SNES format tiles.8bpp`.
- [NEW] Strip out the padding in the 8bpp container to 2bpp, by removing the
  last 0x30 `00` bytes of every group of 0x40 bytes.
  - I suggest you automate this in some way. The resulting file should be exactly
    1376 (0x560) bytes, one quarter of the 8bpp file's 5504 (0x1580).
- Open the 2bpp file in YY-CHR in the `2BPP GB` format.
- Enter in the three palette values from the palette log.

The result in YY-CHR should be:

![YY-CHR screenshot](trimmed%20nami%20graphic%20tiles%20-%20yychr.png)

From here, you can export the tiles and load it and the SNES format tilemap as
normal in Tilemap Studio.

# How to edit
Here, I recommend creating a new graphic from scratch, in a normal image editor,
that writes Nami's name in whatever language you are translating to.
- Because of the Mode 7 effect, try to preserve the dimensions of the graphic
  (0xB x 0x14). I have no promises for what will happen if you do not.
  If your graphic is smaller, you can just pad the empty space with blank tiles.
- With a black background, use the RGB24 colors $6B0000, $A50000, and $C60000.

# How to insert back into the game
Take the new graphic and put it through Tilemap Studio's Image to Tiles feature
with the following options:
- Input: your new graphic as a PNG
- Output: a PNG (name it whatever you like)
- Check ON `Avoid extra blank tiles at the end`
- Format: `Plain tiles`
- Start at ID `$000`
- Check OFF `Blank tiles use ID $___`
  - This is assuming the transparency color is not white.
- Check OFF `Palette`

You should now have a SNES-format binary tilemap file, and a tileset as a PNG.
Take the tileset PNG and run it through superfamiconv with the options `-B 2`
for 2bpp and `--color-zero 00000000` for setting the black background as color 0,
e.g. `.\superfamiconv.exe -i tileset.png -t tileset.bin -B 2 --color-zero 000000`
Open the resulting binary file in YY-CHR and use the `Change Color` tool to
confirm that the colors are correct according to the original palette.

Additionally, you must open this possibly modified binary tileset in a hex
editor, and remove the first 0x10 bytes (should be all [00]). This removes the
empty tile that Tilemap Studio generated for us, because the decompression ASM
code in the game will generate one for us.

Next, convert both the binary tilemap and tileset to their respective formats
expected by the compressor. Then compress, and insert into the game.
