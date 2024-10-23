# Modifying and reinserting graphics for Otogirisou
This document contains instructions for how to dump, modify, and reinsert graphics for Otogirisou for Super Famicom.

# Required tools
- Java installation, to run the custom tools for this project.
- [Tilemap Studio](https://github.com/Rangi42/tilemap-studio)
- A tile editor such as [YY-CHR.NET](https://www.romhacking.net/utilities/958/)
- An image editing software of your choice, that preferably supports layers and
  a single pixel brush tool (in this writeup, I use GIMP)
- [superfamiconv](https://github.com/Optiroc/SuperFamiconv), which can automate
  tileset generation for you.

# Necessary files from graphics dump
After you run either `DUMP graphics.bat` or `DUMP only necessary graphics.bat`, you will get a folder containing several groups of graphics data. The files you are interested in translating and editing are:
| GFX ID(s) | Tileset(s) | Tilemap(s) | Palette | Description |
| --------- | ---------- | ---------- | ------- | ----------- |
| 0x5A | $07CC6B | $07D4B5 | $07D553  | title logo |
| 0x5B | $07E637 | $07E855 | $07E9DE  | file select prompts|
| 0x5C | $07DD87 | $07E2F3 | $07E2CD  | name entry screen |
| 0x5D | $08AA0E | $08C288 | B/W 1bpp | credits letters, "supervisor" credit tilemap |
|  --  | $08CD54 | --      | B/W 1bpp | 終 graphic's tileset
|  --  | $08D086 | --      | B/W 1bpp | 完 graphic's tileset
| 0x66-0x72 | -- | each one | --      | tilemaps for rest of credits
| 0x73 | --      | $08CFE0 | --       | 終 graphic's tilemap
| 0x74 | --      | $08D2FE | --       | 完 graphic's tilemap
| 0x7B | $07A4EA | $07AA34 | $07AA26  | Nami graphic

I did not substantially edit any palettes for this project, but I did find them
helpful for the editing process when using a tile editor.

The modification processes, from easiest to hardest:
- The name entry buttons (only edit tileset)
- The title logo (edit tileset and tilemap)
- The file select prompts (edit tileset and tilemap)
- The Nami graphic (edit tileset and tilemap; special case)
- The credits (edit tilesets and tilemaps, with *lots* of repointing)

# General overview of process
Based on how much of the graphic you can or want to reuse, decide if you want to
create a new translated graphic from scratch, or reuse the original graphic and
overwrite unneeded tiles. My recommendations:
- New: the credits and the Nami graphic.
- Reuse: name entry and file select screens.
- For the title logo, up to you. I personally went for reuse.

Steps for how to translate and reinsert graphics:
- Use my graphics decompression tool and locate the tileset, tilemap, and
  palette for the graphic.
- Recommended: Use YY-CHR and Tilemap Studio to
  [generate a screenshot](#generating-screenshots-from-decompressed-data)
  from the applicable files.
- Create the new translated graphic (or edit the original) in a regular image
  editor. Save it as a PNG.
- Put this PNG through the `Image to Tiles` feature in Tilemap Studio.
  - This generates an image of all the unique tiles, and an SNES-format tilemap
    for your graphic.
  - The exact settings you need to use vary by graphic, but check the log for
    each graphics structure to know:
    - Format: Use `Plain tiles` if original JP tilemap has all [00] for tilemap
      high bytes, or `SNES tiles + attributes` if original JP tilemap has
      tilemap high bytes included.
    - Enable `Avoid extra blank tiles at end` for the credits, but disable it
      for all the other graphics.
    - Always disable the `Palette` section.
    - Always start at ID $000, which is to be set as the empty tile (note: I
      couldn't find a way to set tile 000 as an empty tile with superfamiconv).
- Create a binary-formatted tileset for the new graphic. You can either:
  - Open a tile editor and manually copy in all the tiles from your generated
    tileset so that they are in the proper graphics format (depends on graphic).
    This will be the tileset in the correct format expected by the game.
    - I couldn't find a way to "import" the tileset generated from Tilemap Studio
      as tiles in YY-CHR.NET.
  - Take the tileset generated from Tilemap Studio (*not* the assembled graphic)
    and have superfamiconv generate a binary tileset for you. This process is
    automatic but has some minor drawbacks:
    - It may not assign colors in the original order. You'd need to either move
      around color values in [`insert graphics.asm`](/asm/insert%20graphics.asm)
      based on the output, or open the binary-format tileset in YY-CHR and use
      the `Edit -> Replace Color...` function.
    - The resulting tileset binary file needs its bitplanes un-interleaved with
      `ConvertSNESTilesToInputForCompressor.java`.
- Compress the game format tileset and tilemap.
- See if the compressed files fit into the space used by the original graphic
  (see [`insert graphics.asm`](/asm/insert%20graphics.asm) for a list of sizes).
  - If they fit, you can simply overwrite the original graphic's data in-place.
  - If not, you will have to: find a larger, contiguous, empty space somewhere;
    insert the new data there; and update the pointer to it.
- Related: Your changes will likely use a different number of tiles than the
  original graphic. This is also handled in `insert graphics.asm`.

I have more detailed instructions for each particular graphic in their own
markdown files.

# Some things to keep in mind
You may notice that for dumped graphics data, each data type has two separate files.
- The tilesets have the raw decompressed data, and the tile data converted to
  "SNES format" (interleaved bitplanes) and padded to a pre-defined bit depth.
  - The SNES format tilesets were generated to be easier to pick the correct
    viewing format with YY-CHR.
  - As for which tileset file to use in editing, I will tell you which to use on
    a case by case basis.
- The tilemaps are similar, but with a file that contains the tilemap converted
  to the proper [SNES standard format](https://problemkaputt.de/fullsnes.htm#snesppuvideomemoryvram).
  - The SNES format tilemaps were generated to be compatible with Tilemap
    Studio's "SNES tiles + attributes" format.
- The palettes have a binary file with the raw 15-bit BGR color values, plus a
  text file with both the raw values and their normal 24-bit RGB conversions.

# Generating screenshots from decompressed data
Using YY-CHR and Tilemap Studio, it is possible to generate a screenshot of the
original graphics, without needing to use an emulator, mess around with VRAM
viewers, isolate graphics layers, etc.

Even if you want to use a new graphic, I still recommend doing this so you have an idea of the limitations and expected format for your translated graphic.
- Decompress and dump the graphic to be translated.
- Load its SNES-format tileset in YY-CHR, set the correct format (usually
  `4bpp SNES/PCE(CG)`), and save it as a .bmp image, with `File -> Save bitmap`.
  - This works for any graphic that is 0x100 tiles or smaller. In the case of
    the credits (0x331), see the applicable section (and use format `1bpp 8x8`).
- Open Tilemap Studio and load the SNES-format tilemap (use format `SNES tiles +
  attributes`) and this .bmp of the tileset. Adjust the width of the tilemap to
  match the width from the graphics dumping log.
- "Print" the assembled tiles as a .png file.
