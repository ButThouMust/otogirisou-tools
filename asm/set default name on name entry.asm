includefrom "main.asm"

; Otogirisou SFC has no default name for the protagonist, but he is named Kouhei
; (公平) on a new file in the PS1 remake; if you want to, you can modify the
; subroutine at $02B6FB to set that as his default name

; the existing code for updating the entered name uses its own DMA structure
; that only DMAs as much data to VRAM as necessary for it; change it to DMA the
; name with the top row of the character grid to VRAM
org $00dffa
  ; dl $7f0a10
  ; dw $6500,$0600
    dl $7f0010
    dw $6000,$1000

; need to make a JSR to some code that does [jsr $02b54b ; jsr InitName]
; and take current JSR to InitName out of its control flow
org $2b5c8
    jsr PrintDefaultNameBeforePrintingFirstGrid

org $02B6F7
  ; jsr.w InitializeNameForNameEntry ; cut this out from existing ctrl flow
    rts

PrintDefaultNameBeforePrintingFirstGrid:
    jsr.w InitializeNameForNameEntry
    jmp.w $02b5df

InitializeNameForNameEntry:
    php
    rep #$30

    stz $1aa3
    ldx #$0000
-   lda.w DefaultName,x
    sta.w $1bc9,x
    phx
    jsr.w $02b334
    plx
    inx #2
    cpx #$000C
    bne -

    plp : rts

DefaultName:
    ; you can generate this file with a really basic Atlas script
    assert filesize("script/default name.bin") <= 6*2
    incbin "script/default name.bin"

assert pc() <= $02b730
fillbyte $ff
fill $02b730-pc()

; for some reason, Chunsoft writes the text color to indices 4k+1 and the shadow
; color to indices 4k+2 for 0 <= k <= 5; but I NOPed out the writes to all the
; indices besides 01 and 02 without any negative effects on how text looks
; so we can just cut that all out to make room for the new default name code
org $02b730
SetNameEntryTextColors:
    rep #$30
    lda.w #$0c63    ; set text color
    sta.w $0e9b
    lda.w #$5294    ; set shadowing color
    sta.w $0e9d

    lda.w $02b839   ; set initial color for highlight boxes
    sta $0d9b
    jsl $009383     ; enable DMA to CGRAM
    rts

assert pc() == $02b749

org $02B56B
    jsr SetNameEntryTextColors
