includefrom "main.asm"

; Three .bin files to insert:
; - The script encoded in Huffman format.
; - The Huffman tree's data.
; - The three pointers to where the game can start.

; first, clear out the original Huffman script and font.
!OldFontEndROM = FFFB8
!OldScriptStartROM = AE401
org $15E401
OldScriptStart:
    check bankcross off
    fillbyte $00
    fill $!OldFontEndROM-!OldScriptStartROM+1
    check bankcross full

; insert the new script
org !OldMusicBlockStart
    check bankcross off
    incbin "script/huffman script.bin"
    check bankcross full

; insert the new Huffman tree data
org $02C1E1
    incbin "script/huffman tree - game data format.bin"

; insert the new script start points
!StartPointScriptOffsetsFile = "script/start points' script offsets.bin"
org $02A627
    ; need to convert script offset values into a 21+3 SNES pointer format
    ; 21 bit LoROM offset, plus 3 bits for the "bit in a byte" position

    ; formula: add script_start + num_bytes_into_script (using *file* offsets)
    ; convert file offset to LoROM address, then left shift 3 to make room for
    ; the bit offset
    function numBytes(startVal) = startVal>>3
    function numBits(startVal)  = startVal&$7
    for startNum = 0..3
        !start #= readfile3("!StartPointScriptOffsetsFile",3*!startNum)
        ; print hex((pctosnes($!OldMusicBlockStartROM+numBytes(!start))<<3)|numBits(!start))
        dl numBits(!start)|(pctosnes(!OldMusicBlockStartROM+numBytes(!start))<<3)
    endfor
    ; print ""

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

; false positive, this is in a list of perfect squares
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
