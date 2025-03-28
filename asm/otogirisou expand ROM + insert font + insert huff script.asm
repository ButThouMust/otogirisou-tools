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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Take the music-related data block from 0x4539B ($08D39B) - 0xAE400 ($15AE400)
; and move it into the new 0.5 MB in order to make space for the English script.

; To take advantage of new space, have data block end on the last byte 0x17FFFF
; (ROM file hex editor offset) of the last 0.5 MB. Calculate new start offset.
; formula: new_start = old_start + (new_end - old_end)
; in this case: 0x4539B + (0x17FFFF - 0xAE400) = 0x116F9A ($22EF9A)

!OldMusicBlockStart = $08D39B
!NewMusicBlockStart = $22EF9A
!OldMusicBlockStartROM = $4539B
!OldMusicBlockEndROM = $AE400

org !NewMusicBlockStart
    check bankcross off
    incbin "rom/Otogirisou (Japan).sfc":!OldMusicBlockStartROM..!OldMusicBlockEndROM

; fill the original space with 00 bytes
org !OldMusicBlockStart
    fillbyte $00
    fill !OldMusicBlockEndROM-!OldMusicBlockStartROM+1
    check bankcross full

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; update the pointer (a single pointer!) to the block of music data
; bank offset split up across two instructions

org $009085
MusicBankOffsetLocation:
    dw !NewMusicBlockStart

org $00908A
MusicBankNumberLocation:
    db bank(!NewMusicBlockStart)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
MostRecentChar = $1a7d
CharWidth  = $1a83
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

    lda.w #$0022            ; store bank number; hard-coded to bank $22
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Doing this here because Atlas has no "fill data block with value" function.
; Go to where the game keeps the name entry screen's character data, and fill it
; with [00] bytes (space characters) to make the Atlas script simpler.
; Fill from $02A9E4 until $02AEC1.

!NameEntryDataStart = $02A9E4
!NameEntryDataEnd = $02AEC1

org !NameEntryDataStart
    fillbyte $00
    fill !NameEntryDataEnd-!NameEntryDataStart+1

; ;;;;;;;;;;;;;;;;;;;;

; Putting this here because also related to the name entry screen, particularly
; where (X position) to draw the characters for the name in the top left.

; JP game draws each character at X = name_length * 0xD + 0x1F
; change this to draw at X = L*0xE + 0x18
; L*0xE = L<<3 + L<<2 + L<<1 = L*(8+4+2); had to brainstorm a solution that fits
; given that code after this only has one stack pull instruction instead of two
org $02B35E
    asl a         ; given that L is in A, push L*2
    pha           ; 

    asl a         ; get (L*4 + L*2)*2
    adc.b $01,s   ; 
    asl a         ; 

    adc.b $01,s   ; get L*(8+4+2)
    nop           ; fill extra byte in code with NOP
    adc.w #$0018  ; 

; change the Y position for where to draw the character
org $02B36F
    db $2A

; update logic for where/how to draw highlight box for characters in the name
org $02B4BB
    asl a         ; given A has name length (or 5, whichever is smaller)
    pha           ; push L*2
    asl a         ; push L*4
    pha           ; 
    asl a         ; get L*8, and add L*4 and L*2
    adc.b $01,s   ; 
    adc.b $03,s   ; 
    plx           ; discard temp calculation values
    plx           ; 
    adc.w #$2818  ; highlight box for first character is at [X,Y] = [0x18,0x28]

; change the box from being 13 pixels wide to being 15 pixels wide
; the game accomplishes this as "top right 8x8 sprite is 5 pixels to right"
; and "bottom right 8x8 sprite is 5 pixels to right and 6 pixels down"
; just change the values of "5 right" to be "7 right"
org $02B4CE
    db $07

org $02B4DB
    db $07

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Finally, we get to insert the Huffman compressed script itself.
; Three .bin files to insert:
; - The script encoded in Huffman format.
; - The Huffman tree's data.
; - The three pointers to where the game can start.

; But first, clear out the original Huffman script and font.

!OldFontEndROM = FFFB8
!OldScriptStartROM = AE401

org $15E401
OldScriptStart:
    check bankcross off
    fillbyte $00
    fill $!OldFontEndROM-$!OldScriptStartROM+1
    check bankcross full

org !OldMusicBlockStart
    check bankcross off
    incbin "script/huffman script.bin"
    check bankcross full

org $02C1E1
    incbin "script/huffman tree - game data format.bin"

!StartPointScriptOffsetsFile = "script/start points' script offsets.bin"

org $02A627

    ; need to convert script offset values into a 21+3 SNES pointer format
    ; 21 bit LoROM offset, plus 3 bits for the "bit in a byte" position

    ; formula: add script_start + num_bytes_into_script (using *file* offsets)
    ; convert file offset to LoROM address, then left shift 3 to make room for
    ; the bit offset

    Start0 = readfile3("!StartPointScriptOffsetsFile",3*0)
    Start1 = readfile3("!StartPointScriptOffsetsFile",3*1)
    Start2 = readfile3("!StartPointScriptOffsetsFile",3*2)
    function numBytes(startVal) = startVal>>3
    function numBits(startVal)  = startVal&$7

    ; print hex((pctosnes($!OldMusicBlockStartROM+numBytes(Start0))<<3)|numBits(Start0))
    ; print hex((pctosnes($!OldMusicBlockStartROM+numBytes(Start1))<<3)|numBits(Start1))
    ; print hex((pctosnes($!OldMusicBlockStartROM+numBytes(Start2))<<3)|numBits(Start2))
    ; print ""

    dl numBits(Start0)|(pctosnes(!OldMusicBlockStartROM+numBytes(Start0))<<3)
    dl numBits(Start1)|(pctosnes(!OldMusicBlockStartROM+numBytes(Start1))<<3)
    dl numBits(Start2)|(pctosnes(!OldMusicBlockStartROM+numBytes(Start2))<<3)

; also need to update values used for the script start in other places
org $009EAA
    ; replace CMP.W #$E401 with CMP.W #$[script_start_bank_offset]
    dw !OldMusicBlockStart
org $009EB4
    ; replace CMP.W #$0015 with CMP.W #$00[script_start_bank]
    db bank(!OldMusicBlockStart)

org $00E275
    ; replace ADC.W #$E401 with ADC.W #$[script_start_bank_offset]
    dw !OldMusicBlockStart
org $00E283
    ; replace ADC.W #$0015 with ADC.W #$00[script_start_bank]
    db bank(!OldMusicBlockStart)

; false positive, this in a list of perfect squares
; org $01EB94
    ; dw !OldMusicBlockStart

; again, possibly a false positive in a partial list of perfect squares
; org $01FC0A
    ; dw !OldMusicBlockStart

org $02C0CF
    ; replace ADC.W #$E401 with ADC.W #$[script_start_bank_offset]
    dw !OldMusicBlockStart
org $02C0DD
    ; replace ADC.W #$0015 with ADC.W #$00[script_start_bank]
    db bank(!OldMusicBlockStart)
