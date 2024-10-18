# otogirisou-sfc-tools
Resources and tools for generating a translation patch for 弟切草 Otogirisou for Super Famicom.

# How to use

The custom tools for this project are coded in Java and use batch files. You will need to install Java on your machine.

`BUILD patched game.bat`
- Generates an English patch for the game.

`DUMP jp font.bat`
- Decompresses the original Japanese font into a binary file (created in `font`
  folder). You can view it in YY-CHR.NET with the graphics format `1bpp 16x16`.

`DUMP jp script.bat`
- Decompresses the original Japanese script into an Atlas script file.

`DUMP graphics.bat`
- Decompresses and dumps all of the game's graphics except the font, which is
  covered as mentioned above.

`DUMP only necessary graphics.bat`
- Decompresses and dumps only the graphics that need to be translated for the
  patch.

`generate patch view unused knife graphic.bat`
- Generate a modified version of the Japanese ROM that allows you to view an
  unused graphic of a knife in the game's data. Simply start a new game and read
  the text.

# Folder contents
`asm`
- Asar text files containing assembly code, "include file's binary contents here" directives, etc. for patch.
- Also included: A [DiztinGUIsh](https://github.com/IsoFrieze/DiztinGUIsh)
  project file that includes a mostly complete disassembly of the game.
  - Not everything is filled in, but I tried to document everything that was
  either relevant to the patch or was interesting enough to look into.

`font`
- Image file for font, as well as a modified table file containing the dimensions for each character for the font.

`gfx modification`
- TODO still working on this as of initial commit
- Feel free to use this folder as a space for modifying tilesets and tilemaps for the graphics.
- Instructions for creating your own graphics edits will be included.

`graphics`: 
- Binary files for translated graphics (tiles, tilemaps) to be reinserted.
- NOTE: Any changed graphics that are to be added must be first recompressed into the game's expected format.

`patches`
- Patches for the game will be generated in BPS format in this directory.
- Included is a sample BPS patch to get the ROM that you would get from running
  `generate patch view unused knife graphic.bat`.

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
- Java source code files that are specific to this translation project. See the
  readme in there for more information.

`tables`
- When you first clone this repo, this will only contain the table files for the original Japanese game, and for the control codes in the translation.
- After you run `BUILD patched game.bat`, this will have the full table file for the translation.

`tools`
- Already-existing programs that aid in the translation process. They are
  [Asar](https://github.com/RPGHacker/asar),
  [Atlas](https://www.romhacking.net/utilities/224/),
  [Floating IPS](https://www.romhacking.net/utilities/1040/), and
  [superfamicheck](https://github.com/Optiroc/SuperFamicheck).
  [xkas]https://www.romhacking.net/utilities/269/) is also included but not really required for this particular patch.