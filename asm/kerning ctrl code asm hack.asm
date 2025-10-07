includefrom "asm/main.asm"

; decompresses Huffman code from script, puts it into A
GetScriptValue = $00ADE1

; relevant memory addresses
TextXPos = $1A7F
TextYPos = $1A81
FlagPrintingChoice = $1ab7

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

org $00FAA5
; reasoning for this position: it is the start of a big block of FF bytes after
; a block of palette values from $00FA77 to $00FAA4

; subtract from text X position (kern LEFT)
KernLeft:
    rep #$30
    jsr GetScriptValue
  ; rts
    ; reverse subtraction because # pixels we want to shift left by is in A
    ; which is to say ['~' is binary NOT]: addr - A = addr + ~A + 1
    eor #$ffff
    sec
    bra AddXOffset

; add to text X position (kern RIGHT) 
KernRight:
    rep #$30
    jsr GetScriptValue
    clc
AddXOffset:
    adc TextXPos
    sta TextXPos
    rts

; subtract from text Y position (kern UP)
KernUp:
    rep #$30
    jsr GetScriptValue
    ; same idea as kern left; do reverse subtraction on # pixels to shift up by
    eor #$ffff
    sec
    bra AddYOffset

; add to text Y position (kern DOWN)
KernDown:
    rep #$30
    jsr GetScriptValue
    clc
AddYOffset:
    adc TextYPos
    sta TextYPos
    rts

pushpc

;;;;;;;;;;;;;;;;;;;;

; If the game gets a control code in text when printing a choice option, it will
; only execute the code if it is in a specific list at $00AC01. In JP game:
; - END CHOICE 1C, EC HELPER 1B, NAME-SAN 20, NAME 21, LINE 00, SET X POS 1E,
;   SET Y POS 1F, DELAY 17, WAIT 16
; Format: The code's value, with top 1 cleared out, e.g. 1019 -> 0019 -> [19 00]
; Purpose: Do not change graphics, start/stop sounds, or take JMP codes

; Purpose: When playtesting, I noticed that text would not kern when printing a
; choice but would kern once I picked it and had it print "normally."

; Deprecated easy solution that assumes no choice options in the script will use
; kern up/down/right. Replace WAIT 16 with KERN LEFT; choice options can have
; WAIT codes that only trigger once the player selects them from the list.
; !CtrlCodeIdsToRunForChoiceText = $00ac01
; org !CtrlCodeIdsToRunForChoiceText+2*$8
    ; db !KernLeftNum

; More comprehensive solution: Repurpose some useless control flow at $00ABA0.
; org $00aba0
    ; jsr $ac8f
    ; beq $0c
; check if END CHOICE 1C or EC HELPER 1B
    ; lda $57
    ; cmp #$101c
    ; beq $05
    ; cmp #$101b
; if no, just read the control code args for the code ID
    ; bne ReadArgsForCtrlCodeInChoice

; The JSR to $00AC8F goes to code that only does this:
; org $00ac8f
    ; lda $19ff     ; value is related to which options to ignore for a choice
    ; and #$0000
    ; rts
; But because the AND #$0000 always sets the Z flag, the five instructions after
; the JSR and BEQ never get executed! So we can repurpose the space for code to
; make the table entries only one byte apiece instead of two.

!ReadCtrlCodeArgs = $00a970

; Copy in code starting at $00abaf
org $00ab9e
    bcc NewLinebreakingCtrlFlow
    lda $57     ; get ctrl code ID value
    and #$0fff
    jsr CheckIfCtrlCodeShouldDisableKerning ; ADDED
    ldx FlagPrintingChoice
    beq CaseNotPrintingChoice

CasePrintingChoice:
    sta $10
    ldx #$0000

; now do the 1 byte table entry hack
-   lda CtrlCodeIdsToRunForChoiceText,x
    and #$00ff
    cmp #$00ff
    beq ReadArgsForCtrlCodeInChoice
    cmp $10
    beq GotCtrlCodeToRunForChoice
    inx #1
    bra -

GotCtrlCodeToRunForChoice:
    lda $10
CaseNotPrintingChoice:
    cmp #$0028
    bcs ReadArgsForCtrlCodeInChoice
    asl : tax
    php
    jsr.w (!CtrlCodeASMPointers,x)
    plp
    rts

ReadArgsForCtrlCodeInChoice:
    lda $57
    jmp.w !ReadCtrlCodeArgs

CtrlCodeIdsToRunForChoiceText:
; include JP game's list, then codes you want to include
; this new list just happens to fit in space before the "print character" ASM
    db $1c,$1b,$20,$21,$00,$1e,$1f,$17,$16
    db !KernLeftNum, !KernRightNum, !KernUpNum, !KernDownNum
    db !HonorificCtrlCodeNum
    db $ff

NewLinebreakingCtrlFlow = pc()

; assert pc() <= $00abe4
    ; fillbyte $ff
    ; fill $00abe4-pc()

; clear out the original table's data
assert pc() <= $00ac01
org $00ac01
    fillbyte $ff
    fill $00ac15-pc()

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
; character by character or one screen at a time. So we need to make a larger
; list at some other location in the ROM, and update the pointer accordingly.

; Before I was comfortable with making ASM hacks, I used to insert the new list
; after the kerning code because we can't do an in-place replacement of the list
; entries here. But it turns out the original code also uses the same useless
; logic with $00AC8F, and we can reuse the basic structure for the above hack.

!CtrlCodeIdsToRunForPgScroll = $00e095
CaseGotCharInPgScroll = $00e077

org $00e033
    ; jsr $00ac8f
    ; beq $0c
    ; lda $57
    ; cmp #$101c
    ; beq $05
    ; cmp #$101b
    ; bne ReadArgsForCtrlCodeForPgScroll+1

; check if character or control code
    lda $57
    cmp #$1000
    bcc CaseGotCharInPgScroll

; if got ctrl code, keep code ID = low byte of encoding
  ; sbc #$1000  ; original code, takes advantage of BCC check failing -> C set
    and #$0fff
    pha

; if printing a choice, you must handle any jump codes and do all line breaks
    ldx FlagPrintingChoice
    bne +
    cmp #$0006
    bcc RunCtrlCodeWhenScrollingPages

; if not printing a choice, only execute ctrl code if in the list
+   ldx #$0000

; now do the 1 byte table entry hack
-   lda !CtrlCodeIdsToRunForPgScroll,x
    inx #1
    and #$00ff
    cmp #$00ff
    beq ReadArgsForCtrlCodeForPgScroll
    cmp $01,s
    bne -

RunCtrlCodeWhenScrollingPages:
    pla
    asl : tax
    php
    jsr.w (!CtrlCodeASMPointers,x)
    plp
    rts

; case, do not execute control code, just read its arguments
ReadArgsForCtrlCodeForPgScroll:
    pla
    jmp.w !ReadCtrlCodeArgs

assert pc() <= CaseGotCharInPgScroll
    fillbyte $ff
    fill CaseGotCharInPgScroll-pc()

org !CtrlCodeIdsToRunForPgScroll
    db $19,$1a,$1c,$00,$1b,$1e,$1f,$20,$21
    db !KernLeftNum, !KernRightNum, !KernUpNum, !KernDownNum
    db !HonorificCtrlCodeNum
    db $ff

assert pc() <= $00e0a9
    fillbyte $ff
    fill $00e0a9-pc()

; --------------------

; OLD: Repoint expanded list (old list, 4 kerning codes, FFFF) to after the
; kerning ASM code.

; update pointer to new list of control code IDs
; org $00E05D
    ; dw NewScrollingCodeList

; !ScrollingCodeListStart = $06095
; !ScrollingCodeListEnd = $060A7

pullpc
; NewScrollingCodeList:
    ; incbin "!JProm":!ScrollingCodeListStart..!ScrollingCodeListEnd
    ; dw !KernLeftNum, !KernRightNum, !KernUpNum, !KernDownNum
    ; dw !HonorificCtrlCodeNum
    ; dw $FFFF
