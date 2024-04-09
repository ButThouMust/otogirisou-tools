prompt $g

@set newROM=".\rom\Otogirisou (view unused knife gfx).sfc"

copy /y ".\rom\Otogirisou (Japan).sfc" %newROM%
tools\asar.exe "asm\view unused knife gfx.asm" %newROM%
tools\superfamicheck.exe %newROM% --fix --silent
