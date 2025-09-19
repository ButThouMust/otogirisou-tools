includefrom "main.asm"

; After moving the music data, most of bank 22 is free except for a small part
; at the end. We can might as well insert it there.

; insert data for the new font here
!NewFontLocation = $228000
org !NewFontLocation
    ; incbin "font/new font data.bin"
    incbin "font/new uncomp font data.bin"

; change font storage system to be uncompressed in this format (hex editor view)
;    00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 
; 00 WH -- [R1 ] [R2 ] [R3 ] [R4 ] [R5 ] [R6 ] [R7 ]
; 10 [R8 ] [R9 ] [R10] [R11] [R12] [R13] [R14] [R15]

FontDataBuffer = $1a5d
; MostRecentChar = $1a7d
; CharWidth  = $1a83
CharHeight = $1a85

; original code from JP game for your convenience:
; org $00a4db
;     rep #$30
;     pha
;     phx
;     phy
;     jsr.w $a5e0             ; clear font data buffer (fill with 00 bytes)
;     rep #$20
;     lda.w MostRecentChar    ; if "0000" space, we are done
; ; org $00a4e9
;     beq $06
;     jsr.w $a4f4             ; $14-$17 <- calculate offset for font data
;     jsr.w $a57f             ; decompress font data from pointer
;     jmp.w $86ab             ; undo the PHA/PHX/PHY at start, and RTS

org $00a4e9
    bne LoadUncompFontData
GotSpace:
    lda.w #$0002            ; in JP game, 0000 space reuses the dimensions of
    sta.w CharWidth         ; the previously printed character, notably width
    bra DoneGettingFontData ; change so that width is just a constant 2 pixels
;     jmp.w $86ab            

LoadUncompFontData:
    rep #$30
                            ; calculate bank offset for character's font data
    lda.w MostRecentChar    ; decrement to account for 0000 space character
    dec a
    asl #5                  ; font's 32 byte structures start at $xx8000
    adc.w #$8000
    sta.b $14               ; follow JP game's convention, ptr is in $14-16

    lda.w #bank(!NewFontLocation)
    sta.b $16

    ldy.w #$0000            ; read byte for width and height
    lda.b [$14],y
    pha

    lsr #4                  ; isolate width
    and.w #$000F
    sta.w CharWidth

    pla                     ; isolate height
    and.w #$000F
    sta.w CharHeight

    ldx.w #$0000            ; X <- offset in font data buffer, and loop ctr
    GetPixelRow:
        iny #2              ; advance past [WH 00] or last row of pixels
        lda.b [$14],y       ; get pixel row, switch from big to little endian
        xba
        sta.w FontDataBuffer,x

        inx #2              ; check loop counter, read more rows if needed
        txa
        lsr
        cmp.w CharHeight
        bcc GetPixelRow
DoneGettingFontData:
    jmp.w $86ab

; optional: clear out bytes of original font decompression code
; assert pc() <= $00a5e0
; fillbyte $ff
; fill $00a5e0 - pc()

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; OLD - for inserting font in original compressed format

; update lookup table for font data (58 structs: "N chars at $xx???? are WxH")
; !FontDataLookupTable = $00F4AC
; incbin "font/new font lookup table.bin" -> !FontDataLookupTable

; update the bank number for the font; this value is hard-coded into instruction
; !LocationForFontDataBank = $00A56D
; org !LocationForFontDataBank
    ; db bank(!NewFontLocation)
