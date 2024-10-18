# Credits

For me, changing the credits was by far and away the most extensive graphics
change. For some context, let's examine how the game stores these graphics. From
a graphics dump, take the data from graphics IDs 0x55 and 0x66-0x74. You should
get three tilesets and 16 tilemaps.

The simple explanation is that there are 14 screens of credits like "Supervisor
/ Author: Shukei Nagasaka". These 14 screens each have their own tilemap but
share a single combined tileset. Then there are two more screens that serve as
"The End" after displaying the equivalent of `(c) Chunsoft Co. Ltd.`.
One is for normal endings (終), and another is for a special ending (完).

| GFX ID(s) | Tileset(s) | Tilemap(s) | Description |
| --------- | ---------- | ---------- | ----------- |
| 0x5D | $08AA0E | $08C288 | credits letters, "supervisor" credit tilemap |
|  --  | $08CD54 | --      | 終 graphic
|  --  | $08D086 | --      | 完 graphic
| 0x66-0x72 | -- | each one | tilemaps for rest of credits
| 0x73 | --      | $08CFE0 | 終 graphic
| 0x74 | --      | $08D2FE | 完 graphic

Working with the 終 and 完 are like all the previous graphics. I personally
decided to translate them as "END" and "FIN" respectively. This writeup mainly
focuses on the "main" credits.

To view the other credits graphics, follow these steps:
- Take the raw graphics file (`$08AA0E TILE.bin`) and change the file extension
  to `.1bpp`.
- Load this as a tileset in Tilemap Studio, and use the option `Tools -> Shift
  Tileset` by +1.
- Load any of the SNES format tilemaps from graphics structures 0x5D and
  0x66-0x72.
- Adjust the width accordingly, and you will see the constructed credit.

# How to create translated credits

I found the best way to translate the credits is to just use the text tool in an
image editor like GIMP. Specifically, I suggest these things:
- If your image editor supports it, set up an 8x8 pixel grid ([instructions for
  GIMP](https://docs.gimp.org/2.10/en/gimp-image-configure-grid.html)) for the
  image view. This lets you see at a glance if you can optimize the number of
  tiles or the width of a particular credit.
- Create a separate image for each credit.
- Save the image editor project file (for GIMP, the `.xcf`) where text is as an
  editable layer.

In terms of fonts, the original credits are in a calligraphy style.
- Lazermutt4 recommended the open source font
  [Otsutome](https://www.freejapanesefont.com/otsutome-font-download/) to
  replicate the look. However, you can use whatever font you like.
- Turn off the text anti-aliasing, because the credits are 1bpp (black/white).
- Set the text size so that the max distance from the top of letters to the
  bottom of letters is 24 pixels (3 tiles). Because I did not need to use
  accented letters like `É`, a height around 26 worked for Otsutome.

Considering the lack of anti-aliasing, I personally found that using a mix of
font sizes (yeah), manual text kerning, and having a layer for individually
editing pixels (clean up letters) helped make the text look better.

The original credits use a total of 0x331 tiles. Some tips for trying to
optimize your tileset to stay smaller than this:
- Start new words on the left of tile boundaries. You can reuse tiles for the
  capital letters this way.
- The top of lowercase letters like `a` or `r` (but not like `i` or `t`) should
  be at the top of tile boundaries. This is so that they can occupy two tiles
  vertically instead of three, like below.
- If possible, try to align text so that there are many empty tiles.

Generally, I would recommend keeping the indentation as consistent as possible,
unless one credit approaches the SNES's horizontal resolution of 256 pixels.
This happened with me on the second page of support credits, with the line
`Atsushi Sugimoto (Fancy)`. Ultimately, I decided to move the category line
`Support` two tiles left, and the name line itself one tile left. This left one
tile of space to the left of `Support`, and one to the right of `(Fancy)`.

When you finish up all the credits, you can combine them together into one image
to create a new tileset. Create a new image like this, where each credit is left
aligned and has a one tile vertical gap between each. Again, save as both a
regular image file and as an image editor project.

# Converting to expected game format

Open Tilemap Studio and use the Image to Tiles functionality.

Tileset:
- Input: use the combined image.
- Output: set to whatever .bmp or .png file you like.
- Enable `Avoid extra blank tiles at the end`.

Tilemap:
- Format: `SNES tiles + attributes`.
- Start at ID $000.
- Blank tiles use ID $000.
Disable the Palette option, and click OK.

This will generate two files for you. I recommend you specifically mark which is
which once they are generated:
- The tileset as a .bmp or .png file
- The SNES tilemap for the entire image.

Next, run the batch file `GENERATE credits data.bat` (modify the filename
variables in it as appropriate for your files). This accomplishes:
- Convert the tileset to a 1bpp binary file format.
- Extract the 14 individual tilemaps for each credit from the overall tilemap.
- Convert the extracted tilemaps from standard SNES format to the format
  expected by the RLE compressor.

Open the binary format tileset in a hex editor and delete the first 8 bytes
(should be all `00`). Although Tilemap Studio generated an empty tile for us, we
have to remove it because the game will generate one on its own when
decompressing the graphics.

After this, you will then have to copy the RLE format tilemaps into whatever
folder contains the `RecompressGraphics.java` RLE compressor. You can simply
copy all the .bin files into it and run the batch file `COMPRESS binary gfx
files.bat` to compress all of them in one fell swoop.

# Reinserting the credits graphics

The graphics data for the credits all exist in a contiguous block from $08AA0A
to $08D39A, so I created a spreadsheet in order to determine what could fit. To
have it work with your graphics changes, all you have to do is enter in the
sizes of the recompressed files.

For the tileset and tilemaps I generated, most of the credits' tilemaps would
not fit on a file by file basis (e.g. EN 301 vs JP 223). This was simply because
the tilemaps' dimensions were larger across the board. However, the English
tileset being much smaller (EN 4820 vs JP 6260, over 1400 bytes saved!) more
than made up for this. I could fit everything in the original space, provided
that I repointed all the tilemaps.

One thing to keep in mind: You may or may not have to move and repoint the
palette for the 終 and 完. If you do, I left in a few assembly directives to do
this as a comment in `insert graphics.asm`.

Note: the tileset image generated by Tilemap Studio is not compatible with
importing directly into YY-CHR.NET. Even after following some instructions I
found [here](https://projectpokemon.org/home/forums/topic/58325-saving-a-png-as-8bpp/),
the tileset did not import in the format I wanted it to be in.
