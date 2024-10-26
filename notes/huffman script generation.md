# Overview of translation patch generation
At a high level, the process for generating a translation patch involves:
- Generate the script font, formatted for the game, and its encoding order
- Insert the script into the game with this new encoding
- Analyze the script with the Huffman coding text compression algorithm
- Generate the script, encoded in Huffman format
- Insert the Huffman-encoded script, and move around data to make room
- Dump the Huffman-encoded script back into a text file (helps with playtesting)

See [BUILD patched game.bat](/BUILD%20patched%20game.bat). For the purposes of
this writeup, I will focus on script reinsertion and editing only. Graphics
translation is its own separate topic.

##Script font generation
You need:
- An image consisting of a new font to be inserted into the game.
- An accompanying table file of encoding values with their heights and widths.

This will generate both the font in the format that the game will expect with my
"bypass compression" ASM hack, as well as a table file that includes:
- The game's default space character with encoding value 0000.
- The set of control codes to be used in the patch (kerning, and "print
  honorific" if honorifics are enabled).
- Table file entries that attempt to keep the base script simple, such as:
  - No regexing instances of `AV` in the script itself to `A<KERN LEFT>(2)V`.
  - `?!"` will only trigger an automatic "wait for player input" after the `"`.

##Uncompressed script generation
You need:
- An original ROM image of Otogirisou
- A text file containing a translated script

Using Atlas, the script will be inserted in the uncompressed two-byte encoding
that was generated with the script font. Additionally, the game's three start
points will be updated. This creates a new ROM (exists only temporarily - will
not work in an SNES emulator!), `Otogirisou - EN uncompress.sfc`.

##Huffman compression analysis, and Huffman script generation
You need to know:
- The hex address of the end of the uncompressed script

In the original Japanese game, the script is compressed using Huffman coding.
It is stored as one gigantic string with pointers interspersed throughout.
I found that if I were to generate a Huffman encoding for the translated script
and calculate the size of the corresponding Huffman compressed script, it would
be about *31%* of the size of the uncompressed script. With v1.0 of the patch,
the difference was nearly 1 MB: 0x163FC7 bytes vs. 0x6EFFD bytes and 7 bits.
For context, the original ROM itself is 1 MB!

Analyzing the script with the Huffman compression algorithm involves counting
all the occurrences of each encoding value (letter, control code, control code
argument) in the script. It must know where the script starts (pre-determined by
Atlas directives) and ends (depends on script itself).

The best way I could think of to handle this, was to have the batch file prompt
the user for where the script ends. Open `Otogirisou - EN uncompress.sfc` in a
hex editor. Depending on its size, either input:
- If over 2MB, the hexadecimal address of the end of the file. This happened
  with my English scripts.
- If under 2MB, the hexadecimal address of the last non-zero byte in the file.
  I found this to happen if you try this with the original Japanese script.

![uncomp hex editor screenshot](/images/example%20for%20end%20of%20uncompressed%20script.png)

This step covers both script analysis and Huffman script generation.

##Re-dumping the Huffman compressed script
You need to know:
- The hex address of the end of the Huffman compressed script

Once the Huffman compressed script has been generated, the batch file and Asar
will create a new copy of the ROM (`Otogirisou - huff script test.sfc`), move
around data in it to make room for the script, and insert it.

Open that copy of the ROM in a hex editor, go to offset 0x4539B, and scroll down
until the start of a big wall of `[00]` bytes before offset 0x100000. Enter the
hex address of the last non-zero byte in this range.

![huffman script hex editor screenshot](/images/example%20for%20end%20of%20huffman script.png)

This serves two purposes:
- Ensure that the Huffman script was generated correctly.
- Make the script playtesting process easier (more details below).

One note: I felt that printing `<KERN LEFT>` in the redumped script greatly
hindered readability (it gets used a lot!), so I coded the script dumper to not
print it or its input value.

After this, the batch file will correct the internal checksum and generate two
BPS format patches for you: one with honorifics on, and one with honorifics off.
