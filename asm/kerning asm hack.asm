includefrom "asm/main.asm"

; decompresses Huffman code from script, puts it into A
GetScriptValue = $00ADE1

; relevant memory addresses
TextXPos = $1A7F
TextYPos = $1A81

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

org $00FAA5
; reasoning for this position: it is the start of a big block of FF bytes after
; a block of palette values from $00FA77 to $00FAA4

; add to text X position (kern RIGHT) 
KernRight:
    rep #$30
    jsr GetScriptValue
    clc
AddXOffset:
    adc TextXPos
    sta TextXPos
    rts

; subtract from text X position (kern LEFT)
KernLeft:
    rep #$30
    jsr GetScriptValue
    ; reverse subtraction because # pixels we want to shift left by is in A
    ; which is to say ['~' is binary NOT]: addr - A = addr + ~A + 1
    eor #$ffff
    sec
    bra AddXOffset

; add to text Y position (kern DOWN)
KernDown:
    rep #$30
    jsr GetScriptValue
    clc
AddYOffset:
    adc TextYPos
    sta TextYPos
    rts

; subtract from text Y position (kern UP)
KernUp:
    rep #$30
    jsr GetScriptValue
    ; same idea as kern left; do reverse subtraction on # pixels to shift up by
    eor #$ffff
    sec
    bra AddYOffset

pushpc

;;;;;;;;;;;;;;;;;;;;

; replace ptrs for ctrl codes 100A, 100B, 100D, 100E with the four kerning codes

; Reasoning for why I chose these particular slots: they are unused codes that
; normally would change the speed of the text. I felt they were safe to replace.

; Important: I initially tried using the slots for 1001, 1002, and 1006. When I
;   tested these in game, they adversely affected the game's control flow:
;   - Setting up/down to 1001/1002 made printed text randomly kern up or down.
;   - Setting 1006 to left made choice codes randomly print whatever character
;     is assigned to 0012, before the 'A' in the first choice option.
;     (Why 0012? $009F99 has uncomp. text [1E 10 12 00] = <SET X POS 1E><$0012>)

!CtrlCodeASMPointers = $00F390
!KernLeftNum = $0A
!KernRightNum = $0B
!KernUpNum = $0D
!KernDownNum = $0E

org (!CtrlCodeASMPointers+2*!KernUpNum)
    dw KernUp
org (!CtrlCodeASMPointers+2*!KernDownNum)
    dw KernDown
org (!CtrlCodeASMPointers+2*!KernLeftNum)
    dw KernLeft
org (!CtrlCodeASMPointers+2*!KernRightNum)
    dw KernRight

;;;;;;;;;;;;;;;;;;;;

; set data about arguments for each control code (what kind, how many)
; all 4 codes take one arg that is a regular script value; data should be 01 00

!CtrlCodeArgTable = $00F41E

org (!CtrlCodeArgTable+2*!KernUpNum)
    db $01,$00
org (!CtrlCodeArgTable+2*!KernDownNum)
    db $01,$00
org (!CtrlCodeArgTable+2*!KernLeftNum)
    db $01,$00
org (!CtrlCodeArgTable+2*!KernRightNum)
    db $01,$00

;;;;;;;;;;;;;;;;;;;;

; If the game gets a control code in text when printing a choice option, it will
; only execute the code if it is in a specific list at $00AC01. By default:
; - END CHOICE 1C, EC HELPER 1B, NAME-SAN 20, NAME 21, LINE 00, SET X POS 1E,
;   SET Y POS 1F, DELAY 17, WAIT 16
; Format: The code's value, with top 1 cleared out, e.g. 1019 -> 0019 -> [19 00]
; Purpose: Do not change graphics, start/stop sounds, or take JMP codes

; Purpose: When playtesting, I noticed that text would not kern when printing a
; choice but would kern once I picked it and had it print "normally."

; Of the codes listed, I felt that WAIT 16 was the best option to replace (this
; means the option can have WAITs but only let it trigger when the player picks
; the appropriate option). However, the SET X/Y POS codes are just as good.

; ASSUMPTION: no choice options in the script will contain kern up/down/right.
org $00AC11
    db !KernLeftNum

;;;;;;;;;;;;;;;;;;;;

; Similar issue for when the player presses X or Y to view previous pages.
; Game will check another list at $00E095. By default:
; - CHOICE 19, CHOICE 1A, END CHOICE 1C, LINE 00, EC HELPER 1B, SET X POS 1E,
;   SET Y POS 1F, NAME-SAN 20, NAME 21
; Same format: code's value, with top 1 cleared, e.g. 1019 -> 0019 -> [19 00]

; I had initially thought LINE 00 would be covered under some ASM near here
; ($00E054; directly run code if ID <= 1005, so LINE 00, JMP 03, JMP.cc 04).
; This is true if you scroll back/forth onto normal text, but not if you scroll
; up when the game gives you a choice. Specifically, replacing LINE 00 with
; KERN LEFT messes up the linebreaking for the options when you eventually
; scroll back down to the choice.

; Preferably, we want to keep the kerning consistent no matter if text appears
; character by character or one screen at a time. So this necessitates making
; a larger list at some other location in the ROM, and updating the pointer in
; the instruction at $00E05C appropriately.

; May as well insert the new list after the kerning code.
; Copy the old list, add the four kerning codes, and add the FFFF list ender.

; update pointer to new list of control code IDs
org $00E05D
    dw NewScrollingCodeList

!ScrollingCodeListStart = $06095
!ScrollingCodeListEnd = $060A7
pullpc
NewScrollingCodeList:
    incbin "rom/Otogirisou (Japan).sfc":!ScrollingCodeListStart..!ScrollingCodeListEnd
    dw !KernLeftNum, !KernRightNum, !KernUpNum, !KernDownNum
    dw $FFFF
