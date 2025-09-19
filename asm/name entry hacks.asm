includefrom "main.asm"

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

; Modify where (X position) to draw the characters for the name in the top left
; of the name entry screen. Let L be the length of the name in characters.

; Change formula (X = L*0xD + 0x1F) to (X = L*0xE + 0x18)
; L*0xE = L<<3 + L<<2 + L<<1 = L*(8+4+2); had to brainstorm a solution that fits
; given that code after this only has one stack pull instruction instead of two
org $02B35E
    asl a : pha   ; given that L is in A, push L*2

    asl a         ; get (L*4 + L*2)*2
    adc.b $01,s   ; 
    asl a         ; 

    adc.b $01,s   ; get L*(8+4+2)
    nop           ; fill extra byte in code with NOP
    adc.w #$0018  ; 

; change the Y position for where to draw the character
org $02B36F
    db $2A

; ;;;;;;;;;;;;;;;;;;;;

; update logic for where/how to draw highlight box for characters in the name
org $02B4BB
    asl a : pha   ; assume that A has min(L, 5); push L*2 and L*4
    asl a : pha

    asl a         ; get L*8, and add L*4 and L*2
    adc.b $01,s
    adc.b $03,s

    plx : plx     ; discard temp calculation values
    adc.w #$2818  ; highlight box for first character is at [X,Y] = [0x18,0x28]

; change the box from being 13 pixels wide to being 15 pixels wide
; the game accomplishes this as "top right 8x8 sprite is 5 pixels to right"
; and "bottom right 8x8 sprite is 5 pixels to right and 6 pixels down"
; just change the values of "5 right" to be "7 right"
org $02B4CE
    db $07
org $02B4DB
    db $07

; if you also want to change the height of the box, the bottom two sprites are
; 6 pixels down by default, which totals to a box that is 0xE pixels tall
; org $02b4d6
;     db $06

; ;;;;;;;;;;;;;;;;;;;;

; I wanted to use the full range of the 15 pixel limit for font heights, but the
; "erase font at X/Y on screen" subroutine won't erase the top few pixel rows
; for characters that are 14 pixels (top row) or 15 pixels (top 2 rows) tall.
; Fix by changing this byte from 0E to 10.
org $01f682
  ; db $0e
    db $10

; Also extend the width of how many pixel columns get erased. Game achieves this
; with a 32-bit bitmask of 0007FFFF, which erases the leftmost 13 pixels. Change
; the 0007 to 0000 to let this work for up to 15 pixels.
org $01f6ff
  ; dw $0007
  ; dw $0001 ; try this one, too?
    dw $0000

; ;;;;;;;;;;;;;;;;;;;;

; The JP game has some code for the name entry screen that checks if the player
; selected either the dakuten or handakuten character, and applies it to the
; most recently entered character if compatible. I don't need this, and you can
; free up quite a bit of space (both code and text) from dummying out the code.

AddCharsToName = $02B334

org $02b2a0
    bra CodeAddCharValInA_ToName

org $02b2c8
    lda [$10],y     ; read character value from name entry data
  ; cmp.l $02afe4   ; check if dakuten, run code if yes
  ; beq $02b2da
  ; cmp.l $02afe6   ; check if handakuten, run code if yes
  ; beq $02b300
CodeAddCharValInA_ToName:
    jsr AddCharsToName
    rts

; free up space from $02B2CE-$02B333 (code), 0x66 bytes
fillbyte $ff
fill AddCharsToName-pc()

; free up space from $02AEC4-$02AFE9 (text), another 0x126 bytes
org $02aec4
    fill $02afea-pc()
