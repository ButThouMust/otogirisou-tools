:: turn off printing the full path to current directory
prompt $g

@set srcPath=".\src"

@javac .\src\huffman\*.java .\src\generate_font\*.java .\src\header_files\*.java

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: generate files related to the font:
:: - font in the game's expected format
:: - table files for the font (both big and little endian) 
:: - a binary file w/ character encodings that should trigger auto line breaks

java -classpath %srcPath% generate_font/FontInserterDriver "./font/otogirisou font dimensions left align.tbl" "./font/otogirisou font.png" 000000 16 16

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: Set several variables to hopefully keep the batch file easier to understand
:: and maintain/edit.

@set jpROM=".\rom\Otogirisou (Japan).sfc"
@set uncompScriptROM=".\rom\Otogirisou - EN uncompress.sfc"
@set huffScriptROM=".\rom\Otogirisou - huff script test.sfc"
@set huffScriptROMOldVersion=".\rom\Otogirisou - huff script test - Copy.sfc"
@set honorificsOffROM=".\rom\Otogirisou - huff script test, honorifics off.sfc"

@set scriptTranslationFile=".\script\script translation.txt"
@set translationTableFile=".\tables\uncomp font.tbl"
@set redumpedScriptTranslation=".\script\DUMP - inserted script translation.txt"

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: use the new table file with Atlas to get the script in uncompressed format
:: @copy /y "Otogirisou (Japan).sfc" "Otogirisou - EN uncompress.sfc"
@copy /y %jpROM% %uncompScriptROM%
tools\atlas.exe %uncompScriptROM% %scriptTranslationFile% > logs\log_atlas_uncomp_translation.txt

:: The starter pack on RHDN's Abandoned Projects page had an ASM hack to play
:: the game while using an uncompressed encoding, i.e. just use Atlas and you're
:: done. However, the space saved from using Huffman encoding is significant
:: enough (~31% of uncompressed size) that I decided to go the extra mile and
:: recompress it.
::
:: tools\xkas.exe "expand_ptrs.asm" %uncompScriptROM% > logs\log_xkas.txt

@echo(
@echo Script has been inserted in uncompressed format by Atlas

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: take the uncompressed script and convert it into Huffman format with files:
:: - Huffman script itself that encodes: text, control codes, pointers
:: - Huffman tree structure "flattened out" in the correct format for the game
:: - a 9 byte file containing the 3 start points for the story

:: Important: Whenever you update %scriptTranslationFile%, you must open
:: %uncompScriptROM% in a hex editor and get the hexadecimal offset for the end*
:: of the file (if over 2 MB). The batch file will prompt you for this.
:: *: If this file happens to be 2 MB, use the offset of the last non-zero byte.
:: @echo Please update the address in the batch file for the end of the uncompressed script...
:: @pause
:: @set uncompScriptEndPos=2132dd

:: I prefer manually updating the script offset like above (don't need to change
:: if testing a change to something other than the script), but you can enter
:: it in as user input if you prefer.
:askforinput1
@set /P uncompScriptEndPos="Enter the hex offset of the end of the uncompressed script in 'rom/Otogirisou - EN uncompress.sfc': "

:: Be sure to delete the previous Huffman script so that the new script gets
:: written into a completely fresh, blank file. Avoids instances where the
:: new script is smaller than previous, and the remaining bytes from the old
:: script remain after the end of the new script.
del ".\script\huffman script.bin"

java -classpath %srcPath% huffman/GenerateHuffmanScript %uncompScriptROM% %translationTableFile% aeba0 %uncompScriptEndPos%

:: if %errorlevel% neq 0 pause exit /b %errorlevel%
@if %errorlevel% neq 0 @echo( & goto :askforinput1

del %uncompScriptROM%

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: with the files correctly generated, insert their data into the right places
:: and insert assembly hacks for kerning and English linebreaking

:: keep a copy of previous version
:: copy /y %huffScriptROM% "Otogirisou - huff script test - Copy.sfc"
copy /y %huffScriptROM% %huffScriptROMOldVersion%
del %huffScriptROM%

copy /y %jpROM% %huffScriptROM%
tools\asar.exe "asm\main.asm" %huffScriptROM%

:: update the text for the file select and name entry screen (what characters,
:: plus their positions on screen), as well as text for choice options
tools\atlas.exe %huffScriptROM% ".\script\update name entry, file select.txt" > logs\log_atlas_huff_script.txt

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: for playtesting purposes, dump generated Huffman script back into a text file

:: the redumped script will contain useful data for playtesting the translation
:: write a sequence of 3 bytes three times at $02A627 (linear 0x12627) to view
:: a specific screen of text from a new save file or picking "Restart"/"はじめ"

:: Similarly, the batch file will prompt you again to change the end offset for
:: the Huffman script in the ROM. Go to offset 0x4539B and scroll down until you
:: find a big wall of [00] bytes (it should be in the first MB, before offset
:: 0xFFFFF). Change this variable to the position of the last non-zero byte.
:: @echo Please update the address in the batch file for the end of the Huffman script...
:: @pause
:: @set huffScriptEndPos=b45a7

:: Same as before, allow for user input if the end user prefers.
:askforinput2
@set /P huffScriptEndPos="Enter the hex offset of the end of the Huffman script in 'rom/Otogirisou - huff script test.sfc': "

java -classpath %srcPath% huffman/DumpHuffmanScript %huffScriptROM% %translationTableFile% %redumpedScriptTranslation% 4539b %huffScriptEndPos%

@if %errorlevel% neq 0 @echo( & goto :askforinput2

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: Create the patch as a .bps file. The BPS format is important because part of
:: how the hack works is that it moves a big block of data out of the first MB
:: into the extra half MB, to make space for the script within the first MB.
:: An IPS patch would encode the entire (copyrighted) block within the patch.

tools\superfamicheck.exe %huffScriptROM% --fix --silent
tools\flips.exe --create --bps-delta %jpROM% %huffScriptROM% "patches\otogirisou_en_beta_latest.bps"

:: Do a dead simple assembly hack, at least with the way I set things up, to
:: create a patch to disable the honorifics.

copy /y %huffScriptROM% %honorificsOffROM%
tools\asar.exe "asm\disable honorifics.asm" %honorificsOffROM%
tools\superfamicheck.exe %honorificsOffROM% --fix --silent
tools\flips.exe --create --bps-delta %jpROM% %honorificsOffROM% "patches\otogirisou_en_beta_latest_honorifics_off.bps"

pause