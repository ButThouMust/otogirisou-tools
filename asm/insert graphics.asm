includefrom "main.asm"

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Simple graphics changes; these assume that the compressed data to insert will
; fit into the space for the original data, so do not need to do any repointing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

!TitleLogoTilesetPtr = $07CC6B
!TitleLogoTilemapPtr = $07D4B5
!TitleLogoNumTilesPtr = $07CC67

; replace title logo's graphics and tilemap
; tileset must be <= 2116 (0x844) bytes; tilemap must be <= 154 (0x9A) bytes
org !TitleLogoTilesetPtr
    incbin "graphics/recompressed TL'd title logo TILES.bin"
    assert pc() <= !TitleLogoTilesetPtr+$844
org !TitleLogoTilemapPtr
    incbin "graphics/recompressed TL'd title logo TILEMAP.bin"
    assert pc() <= !TitleLogoTilemapPtr+$9A

; update # tiles to copy in, if needed
; org $07CC67
;     db $8b

; --------------------

!FileSelectTilesetPtr = $07E637
!FileSelectTilemapPtr = $07E855
!FileSelectNumTilesPtr = $07E633

; replace file select prompts like "begin" or "delete"
; tileset must be <= 536 (0x218) bytes; tilemap must be <= 389 (0x185) bytes
org !FileSelectTilesetPtr
    ; incbin "graphics/recompressed TL'd file select TILES.bin"
    incbin "graphics/recompressed file select prompts - new EN tileset squish 'D' no file.bin"
    assert pc() <= !FileSelectTilesetPtr+$218
org !FileSelectTilemapPtr
    ; incbin "graphics/recompressed TL'd file select TILEMAP.bin"
    incbin "graphics/recompressed file select prompts - new EN tilemap squish 'D' no file.bin"
    assert pc() <= !FileSelectTilemapPtr+$185

; update # tiles to copy in
org !FileSelectNumTilesPtr
    db $2d

; circularly shift the palette values back one position
;   purpose: versus the stock palette, the palette that superfamiconv generated
;   for the tileset ended up saving a lot of space when compressing the tileset
; specifically, 557 (not fit) vs 415 (fits)
org $07E9E0
    dw $0842, $3610, $D2F7

; --------------------

!NameEntryTilesetPtr = $07DD87
!NameEntryTilemapPtr = $07E2F3
!NameEntryNumTilesPtr = $07DD83

; replace the 4 buttons in the bottom right of the name entry screen
; tileset must be <= 1346 (0x542) bytes; tilemap must be <= 832 (0x340) bytes
org !NameEntryTilesetPtr
    incbin "graphics/recompressed TL'd name entry TILES.bin"
    assert pc() <= !NameEntryTilesetPtr+$542
; I didn't need to edit the tilemap, but including comments if you do
; org !NameEntryTilemapPtr
;     incbin "graphics/PLACEHOLDER name entry tilemap.bin"
;     assert pc() <= !NameEntryTilemapPtr+$340

; update # tiles to copy in, if needed
; org !NameEntryNumTilesPtr
;     db $42

; --------------------

!NamiTilesetPtr = $07A4EA
!NamiTilemapPtr = $07AA34
!NamiNumTilesPtr = $07A4E6

; replace the graphic that reads "奈美" with one that reads "Nami"
; tileset must be <= 1336 (0x538) bytes; tilemap must be <= 163 (0xA3) bytes
org !NamiTilesetPtr
    ; incbin "graphics/recompressed TL'd nami TILES 5bpp.bin"
    incbin "graphics/recompressed TL'd nami TILES 2bpp.bin"
    assert pc() <= !NamiTilesetPtr+$538
org !NamiTilemapPtr
    incbin "graphics/recompressed TL'd nami TILEMAP.bin"
    assert pc() <= !NamiTilemapPtr+$A3

; update the number of tiles that are copied in, if needed
org !NamiNumTilesPtr
    db $42

; to make reinsertion/recompression easier, make it so the "container" you get
; from decompression for the 2bpp graphics is 2bpp large instead of 5bpp large
org $07A4E9
    ; db $40
    db $10

; --------------------

; use new dimensions for the rectangles that surround file select prompts
; update which sprite IDs are used, and update a pointer to one of them
org $02B7BD
    incbin "graphics/new file select highlight rectangles.bin"
    assert pc() <= $02B839
org $02BC8D
    dw $B7F7

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Restoring an unused graphic into the game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; alters the graphics structure list for graphics ID 0x4E, to replace one knife
; that is also used in graphics ID 0x52, with a knife that went unused

; all you have to do is change out the tileset and tilemap, and change where the
; tileset gets drawn on screen; the existing palette works fine

UnusedKnifeTileset = $079653
UnusedKnifeTilemap = $07997E

org $00BFBA
    dl UnusedKnifeTileset

org $00BFC0
    dl UnusedKnifeTilemap
    skip $5
    db $00,$6B

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Inserting the tilemaps and tileset for the English credits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; to insert the tilemaps, copy the first 4 bytes of the 6 bytes of metadata for
; each tilemap, from the original position, into the new position
; the last two bytes represent the width and height of each tilemap, in tiles
!NumMetadataBytesToCopy = 4

; along the way, throw a warning if this much data overwrites the palette used
; for the END or FIN images
!PaletteForEndFinPtr = $08CD48

; if you want, you can change the palette value for the 1bpp credits to whatever
; you want here; by default, it is $7FFF (white)
; org $01FAFA
    ; dw $7FFF

!NumCreditsTilemaps = 14

; important: these are ROM offsets, not CPU pointers
OldPtr1 = $44282
OldPtr2 = $44307
OldPtr3 = $4436A
OldPtr4 = $44491
OldPtr5 = $4450A
OldPtr6 = $445CC
OldPtr7 = $44675
OldPtr8 = $446FE
OldPtr9 = $447E3
OldPtr10 = $44880
OldPtr11 = $4493F
OldPtr12 = $44A2E
OldPtr13 = $44B94
OldPtr14 = $44C3F

!CreditsFile1 = "graphics/recompressed 01 supervisor author RLE TILEMAP 0x17 x 0x07.bin"
!CreditsFile2 = "graphics/recompressed 02 planner RLE TILEMAP 0x14 x 0x07.bin"
!CreditsFile3 = "graphics/recompressed 03 writer RLE TILEMAP 0x16 x 0x13.bin"
!CreditsFile4 = "graphics/recompressed 04 art director RLE TILEMAP 0x14 x 0x07.bin"
!CreditsFile5 = "graphics/recompressed 05 graphics RLE TILEMAP 0x15 x 0x0B.bin"
!CreditsFile6 = "graphics/recompressed 06 composer RLE TILEMAP 0x1A x 0x07.bin"
!CreditsFile7 = "graphics/recompressed 07 programming director RLE TILEMAP 0x1A x 0x07.bin"
!CreditsFile8 = "graphics/recompressed 08 programmer RLE TILEMAP 0x16 x 0x0B.bin"
!CreditsFile9 = "graphics/recompressed 09 sound programmer RLE TILEMAP 0x17 x 0x07.bin"
!CreditsFile10 = "graphics/recompressed 10 original sfx RLE TILEMAP 0x1B x 0x07.bin"
!CreditsFile11 = "graphics/recompressed 11 support pg 1 RLE TILEMAP 0x19 x 0x0B.bin"
!CreditsFile12 = "graphics/recompressed 12 support pg 2 RLE TILEMAP 0x1E x 0x0F.bin"
!CreditsFile13 = "graphics/recompressed 13 producer director RLE TILEMAP 0x16 x 0x07.bin"
!CreditsFile14 = "graphics/recompressed 14 (c) chunsoft RLE TILEMAP 0x16 x 0x07.bin"

; (ab)use of plaintext replacement for defines
!CreditsDimens1 = $17,$07
!CreditsDimens2 = $14,$07
!CreditsDimens3 = $16,$13
!CreditsDimens4 = $14,$07
!CreditsDimens5 = $15,$0B
!CreditsDimens6 = $1A,$07
!CreditsDimens7 = $1A,$07
!CreditsDimens8 = $16,$0B
!CreditsDimens9 = $17,$07
!CreditsDimens10 = $1B,$07
!CreditsDimens11 = $19,$0B
!CreditsDimens12 = $1E,$0F
!CreditsDimens13 = $16,$07
!CreditsDimens14 = $16,$07

; replace tileset used for the credits, and update # tiles used
org $08AA0A
    dw $02D9
    skip $2
    incbin "graphics/recompressed 00 english credits TILES - no empty tile.bin"
    assert pc() <= !PaletteForEndFinPtr

; for each tilemap, grab 4 metadata bytes from original ptr, set new tilemap's
; width/height, and insert new tilemap's compressed data
for i = 1..!NumCreditsTilemaps+1
    NewCreditPointer!i:
        incbin "!JProm":(OldPtr!i)..(OldPtr!i+!NumMetadataBytesToCopy)
        db !{CreditsDimens!{i}}
        incbin "!{CreditsFile!{i}}"
        assert pc() <= !PaletteForEndFinPtr
endfor

; If you NEED to repoint the palette data, you can do so with these commands:
; NewCreditsPalettePointer:
;     incbin "!JProm":$44D48..$44D4F
; org $00C19C
;     dl NewCreditsPalettePointer

;;;;;;;;;;;;;;;;;;;;

; now that all of the tilemaps are inserted with their pointers accounted for,
; go to the structure list for each one, and update both their pointers and the
; Y/X tile positions of each tilemap on screen

; I suggest keeping the X positions as consistent as possible, and centering the 
; Y positions based on a screen height of 224 pixels = 1C tiles.
; Tilemap heights I used (7, B, F, 13) -> Y positions (A, 8, 6, 4)
; However, keep the 1st credit at Y position 9 so it appears on cue with music.

; Tilemap structures are each 10 bytes. Bytes 0-2 are the pointer.
; Byte 9 is the Y/X position on screen. So six bytes are ignored.
!SkipBytes = $A-1-3

CreditsXY1 = $93
CreditsXY2 = $A3
CreditsXY3 = $43
CreditsXY4 = $A3
CreditsXY5 = $83
CreditsXY6 = $A3
CreditsXY7 = $A3
CreditsXY8 = $83
CreditsXY9 = $A3
CreditsXY10 = $A3
CreditsXY11 = $83
CreditsXY12 = $61
CreditsXY13 = $A3
CreditsXY14 = $95

; credit 1 needs to be handled separately because it's clumped together with
; more structures in its list
org $00C192
    dl NewCreditPointer1
    skip !SkipBytes
    db CreditsXY1

; loop over the structure lists for credits 2-14 (all are same size, 0xB)
; each list starts with a byte to indicate 1 structure, which is 0xA bytes large
for i = 2..!NumCreditsTilemaps+1
    org $00C1A4+(!i-2)*$B
        dl NewCreditPointer!i
        skip !SkipBytes
        db CreditsXY!i
endfor

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Translate 終 to "END" and 完 to "FIN"
; Both default screen positions work fine for my replacement images, but I will
; include the pointers to the graphics structures as comments anyway.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; 終 to "END"

!EndTilesetPtr = $08CD54
!EndTilemapPtr = $08CFE0

; org $00C233
    ; skip $9
    ; db $8A

; change # tiles, and insert new tileset (size <= 646, 0x286)
org $08CD50
    dw $0037
org !EndTilesetPtr
    incbin "graphics/recompressed 15 END TILES - no empty tile.bin"
    assert pc() <= !EndTilesetPtr+$286

; change width and height, and insert new tilemap (size <= 162, 0xA2)
org $08CFDE
    db $0C,$05
org !EndTilemapPtr
    incbin "graphics/recompressed 15 END tilemap 0x0C x 0x05.bin"
    assert pc() <= !EndTilemapPtr+$A2

;;;;;;;;;;;;;;;;;;;;

; 完 to "FIN"

!FinTilesetPtr = $08D086
!FinTilemapPtr = $08D2FE

; org $00C23E
    ; skip $9
    ; db $8A

; insert new tileset (size <= 626, 0x272)
org $08D082
    dw $002E
org !FinTilesetPtr
    incbin "graphics/recompressed 16 FIN TILES - no empty tile.bin"
    assert pc() <= !FinTilesetPtr+$272

; change width and height, and insert new tilemap (size <= 157, 0x9D)
org $08D2FC
    db $0B,$05
org !FinTilemapPtr
    incbin "graphics/recompressed 16 FIN tilemap 0x0B x 0x05.bin"
    assert pc() <= !FinTilemapPtr+$9D
