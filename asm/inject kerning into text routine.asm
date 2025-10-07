includefrom "main.asm"

; ------------------

; repurpose memory address for JOY4L/JOY4H (player 4's input state)
LastPrintedChar = $0a2b

org $00858a
    nop #6

; ------------------

pullpc

; use current character to get a byte offset from pointer from table
DoAutoKernLeft:
    php
    rep #$30

    ; encoding 0000 for a space is taken to mean "do not auto kern"
    ldy MostRecentChar
    beq ExitIfKerningOff
    dey
    lda LastPrintedChar
    beq ExitIfKerningOff

    ; use previously printed character to get pointer from table
    dec : asl : tax
    lda.l KerningPtrTable,x
    pha

    ; use current character as pointer offset; read byte with four 2-bit values
    tya
    lsr #2
    clc
    adc $01,s
    tax
    lda.l KerningPtrTable,x
    sta $01,s   ; this is faster than doing PLX before the TAX, and PHA here

    ; use current char to determine which of the four 2-bit values to extract
    tya
    and #$0003
    asl : tax
    pla
    jmp (JumpTableKerning,x)

JumpTableKerning:
    dw GetValue0, GetValue1, GetValue2, GetValue3

GetValue0:  ; extract out 00------
    lsr #2
GetValue1:  ; extract out --11----
    lsr #2
GetValue2:  ; extract out ----22--
    lsr #2
GetValue3:  ; extract out ------33
    and #$0003

; subtract this value from the current X position
    eor #$ffff
    sec
    adc TextXPos
    sta TextXPos

ExitIfKerningOff:
    lda MostRecentChar
    sta LastPrintedChar
    plp
    rts

; ------------------

CalcNewXPos16BitAndAutoKernLeft:
    jsr CalcNewXPos16Bit
    jsr DoAutoKernLeft
    lda MostRecentChar
    sta LastPrintedChar
    rts

; in case you need to do this outside bank 00 like for names on file select
; JslHereToCalcNewXPosAndAutoKernLeft:
    ; jsr CalcNewXPos16BitAndAutoKernLeft
    ; rtl

; ------------------

; some control codes should outright disable automatic kerning; see list below
CheckIfCtrlCodeShouldDisableKerning:
    pha : phx : php
    and #$00ff
    sep #$30
    pha
    ldx #$00

-   lda ListCodesThatDisableKerning,x
    cmp #$ff    ; check end of list
    beq +
    inx
    cmp $01,s   ; if got match in list, overwrite last char value with a space
    bne -
    jsr DisableKerning

+   pla
    plp : plx : pla
    rts

ListCodesThatDisableKerning:
    ; LINE, CHOICEs, END CHOICE, CLEARs
    db $00,$19,$1a,$1b,$1c,$25,$26
    ; SET X POS, SET Y POS
    db $1e,$1f
    ; optional? manually kerning in the script
    db !KernLeftNum, !KernRightNum, !KernUpNum, !KernDownNum
    db $ff

DisableKerning:
    php
    rep #$30
    stz LastPrintedChar
    plp
    rts

; ------------------

; insert kerning distance tables into start of extra 0.5 MB
; the data here can take up a LOT of space depending on how many characters are
; in your font and on how many characters reuse kerning pairs
org $208000
KerningPtrTable:
    incbin "font/kerning ptr table.bin"
    incbin "font/kerning tables.bin"
  ; db $00,$00
