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
    db $00,$19,$1a,$1b,$1c,$25,$26,$27
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

JslHereToAutoKernLeft:
    jsr DoAutoKernLeft
    rtl

JslHereToDisableKerning:
    jsr DisableKerning
    rtl

; ------------------

; insert kerning distance tables into start of extra 0.5 MB
; the data here can take up a LOT of space depending on how many characters are
; in your font and on how many characters reuse kerning pairs
org $208000
KerningPtrTable:
    incbin "font/kerning ptr table.bin"
    incbin "font/kerning tables.bin"
  ; db $00,$00

; --------------------

; You can also add kerning for the names on the file select screen. The idea
; behind this hack is similar to what is in "improve linebreaking.asm", but
; putting here to keep it with the theme of adding kerning to the text routines.

; You must make a new version of the code at $02b685 (read chars from name ptr)
; and $02b6af (print char encoding in A), and change the [jsl $02b685 ; rts] at
; $02b984 to go to this new code instead. Both the file select and name entry
; screens use the existing code for printing characters without kerning: page
; and playthrough counters, # choices picked, and text on the name entry screen
; (grid of characters, page #, page descriptions).

org $02b685
; this "JSL here" setup turns out to be unneeded; only used once, and in bank 02
;     jsr.w $02b689 
;     rtl
; org $02b689
;     php
;     rep #$30
;     sep #$10
;     ldx #$70      ; set up to read from bank $70 = SRAM for the name
;     bra +
    fillbyte $ff
    fill $02b692-pc()

; org $02b692
;     php
;     rep #$30
;     sep #$10
;     ldx #$02      ; set up to read from bank $02

; +   stx $54       ; construct pointer from bank number and bank offset in A
;     sta $52

; -   lda [$52]     ; read characters and print them; string is FFFF terminated
;     inc $52
;     inc $52
;     cmp #$ffff
;     beq +
;     jsr.w $02b6af
;     bra -

; +   plp
;     rts

; the space where the honorifics used to be happens to fit this new code
org $02aff8
PrintBookmarkNameWithKerning:
    php
    rep #$30
    sep #$10
    ldx #$70      ; set up to read from bank $70 = SRAM for the name
    stx $54       ; construct pointer from bank number and bank offset in A
    sta $52

    jsl JslHereToDisableKerning
-   lda [$52]     ; read characters and print them; string is FFFF terminated
    inc $52
    inc $52
    cmp #$ffff
    beq +

    ; assume that specifically for the name, do not need to check linebreaking
    sta.w MostRecentChar
    jsl $009595     ; JSL to load font data
    jsl JslHereToAutoKernLeft
    jsl $00959d     ; JSL to write font data to bank 7F
    jsl $00957d     ; JSL to calculate text X position
    bra -

+   plp
    rts

; taken care of in "honorifics asm hack.asm", but putting here for clarity
; assert pc() <= $02b03e
    ; fillbyte $ff
    ; fill $02b03e-pc()

; inject new code into game logic
org $02b984
  ; jsl $02b685
  ; rts
    jmp PrintBookmarkNameWithKerning
assert pc() <= $02b989
    fillbyte $ea
    fill $02b989-pc()
