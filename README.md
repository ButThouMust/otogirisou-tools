# otogirisou-sfc-tools
Resources and tools for generating a translation patch for 弟切草 Otogirisou for Super Famicom.

# How to use

The custom tools for this project are coded in Java. You will need to install Java on your machine.

`BUILD patched game.bat`
- Generates an English patch for the game.

`DUMP jp font.bat`
- Decompresses the original Japanese font into a binary file (created in `font`
  folder). You can view it in YY-CHR.NET with the graphics format `1bpp 16x16`.

`DUMP jp script.bat`
- Decompresses the original Japanese script into an Atlas script file.

`DUMP graphics.bat`
- Decompresses and dumps all of the game's graphics except for the font and
  the "Chunsoft Presents" splash screen when you start up the game.

`DUMP only necessary graphics.bat`
- Decompresses and dumps only the graphics that need to be translated for the
  patch.

# Folder contents
`asm`
- Asar text files containing assembly code, "include file's binary contents here" directives, etc. for patch.

`font`
- Image file for font, as well as a modified table file containing the dimensions for each character for the font.

`gfx modification`
- TODO still working on this as of initial commit
- Feel free to use this folder as a space for modifying tilesets and tilemaps for the graphics.
- Instructions for creating your own graphics edits are included.

`graphics`: 
- Binary files for translated graphics (tiles, tilemaps) to be reinserted.
- NOTE: Any changed graphics that are to be added must be first recompressed into the game's expected format.

`patches`
- Patches for the game will be generated in BPS format in this directory.

`rom`
- Place an unmodified, unheadered Japanese ROM image of Otogirisou into this directory. Please do not ask me where to obtain it.
- "Hard-patched" versions of the ROM will be generated here.
- The specification in the No-Intro database for the source ROM is:
```
CRC32:   8e4befd0
MD5:     ae1e9c92d0b7e6dba6c6007d99c9c3f4
SHA-1:   2c27b89a244abe941b6a9fceb1a674dbefd1f734
SHA-256: d85b6764a35f4dcee3ab5843df1c467ebdfe5f02236043a4e466e6975a3f70ca
```

`script`
- Contains Atlas text files for the main translated script, and for updating text for things such as:
  - name entry screen characters
  - text on the file select screen
  - special characters for other things:
    - punctuation with inherent WAITs or DELAYs
    - choice option letters
    - digits for menus
    - honorifics
- After running the batch file, this directory will also include a handful of files with more details about the script insertion process.

`src`
- Java source code files that are specific to this translation project.
- The main purpose is for generating the font and script data in the format that the game expects.

`tables`
- When you first clone this repo, this will only contain the table files for the original Japanese game, and for the control codes in the translation.
- After you run `BUILD patched game.bat`, this will have the full table file for the translation.

`tools`
- Already-existing programs that aid in the translation process. They are Asar, Atlas, Floating IPS, and superfamicheck. xkas is also included but not really required for this particular patch.
