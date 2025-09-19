asar 1.90
check title "OTOGIRISOU           "
lorom

MostRecentChar = $1A7D
CharWidth = $1A83

; these three need to come in this particular order
incsrc "asm/kerning asm hack.asm"
incsrc "asm/improve linebreaking.asm"
incsrc "asm/honorifics asm hack.asm"

incsrc "asm/insert graphics.asm"

; these next two need to come in this particular order
incsrc "asm/expand ROM and move sound data.asm"
incsrc "asm/insert font.asm"

incsrc "asm/name entry hacks.asm"
incsrc "asm/insert huffman script.asm"

; optional, feel free to leave out if you so desire
incsrc "asm/modify font shadowing.asm"
