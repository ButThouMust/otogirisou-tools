# Generating a font for the game
- `FontImage`, `FontInfo`, `FontInfoDimensionComparator`, `FontInserter`,
  `FontInserterDriver`, `KerningPunctPairs`
  - Given a new font, generate the font data for the game and a table file for
    the font, with entries for "automatic" kerning and punctuation combinations.
  - This assumes that the original font compression has been bypassed in favor
    of the uncompressed format I came up with.

# Graphics decompression
- `OtogirisouGraphicsDumper`, `GraphicsStructureList`, `GraphicsStructure`,
  - Decompress and dump the game's background graphics (tilesets, tilemaps, palettes).
- `ChunsoftPresentsDumper`
  - Decompress and dump the 8bpp graphics data for the "Chunsoft Presents"
    splash screen when you boot up the game.

# Huffman script dumping and generation
- `HuffmanFromReinsertedScript`
  - Given a ROM file with a script inserted in uncompressed format by Atlas,
    generate a Huffman coding for it, and generate a compressed script using
    that Huffman coding.
- `DumpHuffmanScript`, `HuffScriptPointer`
  - Given an Otogirisou game script compressed in Huffman coding format, dump it
    out into an Atlas script file.
  - Can be for either the original Japanese script, or scripts generated with
    this project.
- `DumpHuffmanCodesFromROM`
  - Given the original Japanese game, dump out the Huffman codes and the Huffman
    tree structure.

# Misc. data dumper(s)
- `OtogirisouFontDumper`
  - Given the original Japanese game, dump out the compressed font data.

# Header file
- `HelperMethods`
  - "Header file" with useful constants and pointers to useful things.