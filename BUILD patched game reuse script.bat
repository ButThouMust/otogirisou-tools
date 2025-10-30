:: If you want to quickly test ASM hacks without sitting through generating the
:: Huffman script all over again, have I got the batch file for you!
:: note that this assumes you have already generated it at least once

:: turn off printing the full path to current directory
prompt $g

:: Set several variables to hopefully keep the batch file easier to understand
:: and maintain/edit.

@set jpROM=".\rom\Otogirisou (Japan).sfc"
@set huffScriptROM=".\rom\Otogirisou - huff script test.sfc"
@set huffScriptROMOldVersion=".\rom\Otogirisou - huff script test - Copy.sfc"
@set honorificsOffROM=".\rom\Otogirisou - huff script test, honorifics off.sfc"

:: #############################################################################
:: #############################################################################
:: #############################################################################

tools\Atlas.exe ".\script\default name.bin" ".\script\default name - atlas file.txt"

@javac src\GenerateKerningData.java
java -classpath ".\src" GenerateKerningData

:: keep a copy of previous patched version
copy /y %huffScriptROM% %huffScriptROMOldVersion%
del %huffScriptROM%

:: do all the necessary data insertion and ASM hacks
copy /y %jpROM% %huffScriptROM%
tools\asar.exe "asm\main.asm" %huffScriptROM%

:: update the text for the file select and name entry screen (what characters,
:: plus their positions on screen), as well as text for choice options
tools\atlas.exe %huffScriptROM% ".\script\update name entry, file select.txt" > logs\log_atlas_huff_script.txt

:: #############################################################################
:: #############################################################################
:: #############################################################################

:: Create the patch as a .bps file. The BPS format is important because part of
:: how the hack works is that it moves a big block of data out of the first MB
:: into the extra half MB, to make space for the script within the first MB.
:: An IPS patch would encode the entire (copyrighted) block within the patch.

tools\superfamicheck.exe %huffScriptROM% --fix --silent
tools\flips.exe --create --bps-delta %jpROM% %huffScriptROM% "patches\otogirisou_english_latest.bps"

:: Do a dead simple assembly hack, at least with the way I set things up, to
:: create a patch to disable the honorifics.

copy /y %huffScriptROM% %honorificsOffROM%
tools\asar.exe "asm\disable honorifics.asm" %honorificsOffROM%
tools\superfamicheck.exe %honorificsOffROM% --fix --silent
tools\flips.exe --create --bps-delta %jpROM% %honorificsOffROM% "patches\otogirisou_english_latest_honorifics_off.bps"

pause
