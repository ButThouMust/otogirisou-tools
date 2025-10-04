asar 1.90
check title "OTOGIRISOU           "
lorom

; pad the ROM from 1 MB to 1.5 MB, filling with 00 bytes
org $2FFFFF
    db $00

; update ROM header to specify a 1.5 MB (2 MB) ROM instead of a 1 MB ROM
org $00FFD7
;   db $0a
    db $0b

MostRecentChar = $1A7D
CharWidth = $1A83

!JProm = "rom/Otogirisou (Japan).sfc"

; these four need to come in this particular order
incsrc "asm/inject new ctrl codes into jump table.asm"
incsrc "asm/kerning ctrl code asm hack.asm"
incsrc "asm/improve linebreaking.asm"
incsrc "asm/honorifics asm hack.asm"

incsrc "asm/insert graphics.asm"
incsrc "asm/update highlight box dimensions.asm"

incsrc "asm/move sound data.asm"
incsrc "asm/insert font.asm"

incsrc "asm/name entry hacks.asm"
incsrc "asm/set default name on name entry.asm"
incsrc "asm/insert huffman script.asm"

; optional, feel free to leave out if you so desire
incsrc "asm/modify font shadowing.asm"
