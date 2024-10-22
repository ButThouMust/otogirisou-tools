check title "OTOGIRISOU           "

lorom
math pri on

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Simple graphics changes; these assume that the compressed data to insert will
; fit into the space for the original data, so do not need to do any repointing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; replace title logo's graphics and tilemap
; tileset must be <= 2116 bytes; tilemap must be <= 154 bytes
incbin "graphics/recompressed TL'd title logo TILES.bin" -> $07CC6B
incbin "graphics/recompressed TL'd title logo TILEMAP.bin" -> $07D4B5

; update the number of tiles that get copied in, if needed
; org $07CC67
;     db $8b

; --------------------

; replace file select prompts like "begin" or "delete"
; tileset must be <= 536 bytes; tilemap must be <= 389 bytes
; incbin "graphics/recompressed TL'd file select TILES.bin" -> $07E637
incbin "graphics/recompressed file select prompts - new EN tileset squish 'D' no file.bin" -> $07E637

; update the number of tiles that get copied in
org $07E633
    db $2d

; incbin "graphics/recompressed TL'd file select TILEMAP.bin" -> $07E855
incbin "graphics/recompressed file select prompts - new EN tilemap squish 'D' no file.bin" -> $07E855

; circularly shift the palette values back one position
;   purpose: versus the stock palette, the palette that superfamiconv generated
;   for the tileset ended up saving a lot of space when compressing the tileset
;   specifically, 557 (not fit) vs 415 (fits)
org $07E9E0
    dw $0842, $3610, $D2F7

; --------------------

; replace the 4 buttons in the bottom right of the name entry screen
; notice: tilemap is unchanged, but including instruction if need to change
; tileset must be <= 1346 bytes; tilemap must be <= 832 bytes
incbin "graphics/recompressed TL'd name entry TILES.bin" -> $07DD87
; incbin "graphics/PLACEHOLDER name entry tilemap.bin" -> $07E2F3

; update the number of tiles that get copied in, if needed
; org $07DD83
;     db $42

; --------------------

; replace the graphic that reads "奈美" with one that reads "Nami"
; tileset must be <= 1336 bytes; tilemap must be <= 163 bytes
; incbin "graphics/recompressed TL'd nami TILES 5bpp.bin" -> $07A4EA
incbin "graphics/recompressed TL'd nami TILES 2bpp.bin" -> $07A4EA
incbin "graphics/recompressed TL'd nami TILEMAP.bin" -> $07AA34

; update the number of tiles that are copied in, if needed
org $07A4E6
    db $42

; to make reinsertion/recompression easier, make it so the 2bpp graphics are in
; a 2bpp container instead of a 5bpp container when decompressed
org $07A4E9
    ; db $40
    db $10

; --------------------

; use new dimensions for the rectangles that surround file select prompts
; update which sprite IDs are used, and update a pointer to one of them
incbin "graphics/new file select highlight rectangles.bin" -> $02B7BD
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

!JProm = "rom/Otogirisou (Japan).sfc"

; replace the tileset used for the credits, and update # tiles for it
; notice no "-> $address" on the incbin; this is to keep the PC's value from
; directly after the inserted file, so we can assign it to a label
org $08AA0A
    dw $02D9
    skip $2
    incbin "graphics/recompressed 00 english credits TILES - no empty tile.bin"
    warnpc !PaletteForEndFinPtr

; basic template for setting up new tilemaps: get pointer to start of metadata
NewCredit1Pointer:
    ; grab 4 bytes from the original pointer
    OldPtr1 = $44282
    incbin "!JProm":(OldPtr1)-(OldPtr1+!NumMetadataBytesToCopy)
    ; set the new width and height of the tilemap
    db $17,$07
    ; insert the new tilemap's binary data
    incbin "graphics/recompressed 01 supervisor author RLE TILEMAP 0x17 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit2Pointer:
    OldPtr2 = $44307
    incbin "!JProm":(OldPtr2)-(OldPtr2+!NumMetadataBytesToCopy)
    db $14,$07
    incbin "graphics/recompressed 02 planner RLE TILEMAP 0x14 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit3Pointer:
    OldPtr3 = $4436A
    incbin "!JProm":(OldPtr3)-(OldPtr3+!NumMetadataBytesToCopy)
    db $16,$13
    incbin "graphics/recompressed 03 writer RLE TILEMAP 0x16 x 0x13.bin"
    warnpc !PaletteForEndFinPtr

NewCredit4Pointer:
    OldPtr4 = $44491
    incbin "!JProm":(OldPtr4)-(OldPtr4+!NumMetadataBytesToCopy)
    db $14,$07
    incbin "graphics/recompressed 04 art director RLE TILEMAP 0x14 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit5Pointer:
    OldPtr5 = $4450A
    incbin "!JProm":(OldPtr5)-(OldPtr5+!NumMetadataBytesToCopy)
    db $15,$0B
    incbin "graphics/recompressed 05 graphics RLE TILEMAP 0x15 x 0x0B.bin"
    warnpc !PaletteForEndFinPtr

NewCredit6Pointer:
    OldPtr6 = $445CC
    incbin "!JProm":(OldPtr6)-(OldPtr6+!NumMetadataBytesToCopy)
    db $1A,$07
    incbin "graphics/recompressed 06 composer RLE TILEMAP 0x1A x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit7Pointer:
    OldPtr7 = $44675
    incbin "!JProm":(OldPtr7)-(OldPtr7+!NumMetadataBytesToCopy)
    db $1A,$07
    incbin "graphics/recompressed 07 programming director RLE TILEMAP 0x1A x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit8Pointer:
    OldPtr8 = $446FE
    incbin "!JProm":(OldPtr8)-(OldPtr8+!NumMetadataBytesToCopy)
    db $16,$0B
    incbin "graphics/recompressed 08 programmer RLE TILEMAP 0x16 x 0x0B.bin"
    warnpc !PaletteForEndFinPtr

NewCredit9Pointer:
    OldPtr9 = $447E3
    incbin "!JProm":(OldPtr9)-(OldPtr9+!NumMetadataBytesToCopy)
    db $17,$07
    incbin "graphics/recompressed 09 sound programmer RLE TILEMAP 0x17 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit10Pointer:
    OldPtr10 = $44880
    incbin "!JProm":(OldPtr10)-(OldPtr10+!NumMetadataBytesToCopy)
    db $1B,$07
    incbin "graphics/recompressed 10 original sfx RLE TILEMAP 0x1B x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit11Pointer:
    OldPtr11 = $4493F
    incbin "!JProm":(OldPtr11)-(OldPtr11+!NumMetadataBytesToCopy)
    db $19,$0B
    incbin "graphics/recompressed 11 support pg 1 RLE TILEMAP 0x19 x 0x0B.bin"
    warnpc !PaletteForEndFinPtr

NewCredit12Pointer:
    OldPtr12 = $44A2E
    incbin "!JProm":(OldPtr12)-(OldPtr12+!NumMetadataBytesToCopy)
    db $1E,$0F
    incbin "graphics/recompressed 12 support pg 2 RLE TILEMAP 0x1E x 0x0F.bin"
    warnpc !PaletteForEndFinPtr

NewCredit13Pointer:
    OldPtr13 = $44B94
    incbin "!JProm":(OldPtr13)-(OldPtr13+!NumMetadataBytesToCopy)
    db $16,$07
    incbin "graphics/recompressed 13 producer director RLE TILEMAP 0x16 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit14Pointer:
    OldPtr14 = $44C3F
    incbin "!JProm":(OldPtr14)-(OldPtr14+!NumMetadataBytesToCopy)
    db $16,$07
    incbin "graphics/recompressed 14 (c) chunsoft RLE TILEMAP 0x16 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

; If you NEED to repoint the palette data, here is some code to do that for you:
; NewCreditsPalettePointer:
;     incbin "rom/Otogirisou (Japan).sfc":$44D48-$44D4F
; org $00C19C
;     dl NewCreditsPalettePointer

;;;;;;;;;;;;;;;;;;;;

; now that all of the tilemaps are inserted with their pointers accounted for,
; go to each one's structure list and update their pointers
; also update the Y/X tile positions of each tilemap on screen

; I suggest keeping the X positions as consistent as possible, and centering the 
; Y positions based on a screen height of 224 pixels = 1C tiles.
; Tilemap heights I used (7, B, F, 13) -> Y positions (A, 8, 6, 4)
; However, keep the 1st credit at Y position 9 so it appears on cue with music.

; A tilemap structure is 10 bytes. The first three bytes are the pointer. The
; last byte is the Y/X position on screen. So six bytes are ignored.
!SkipBytes = $A-1-3

; credit 1
org $00C192
    dl NewCredit1Pointer
    skip !SkipBytes
    db $93

; credit 2
org $00C1A4
    dl NewCredit2Pointer
    skip !SkipBytes
    db $A3

; credit 3
org $00C1AF
    dl NewCredit3Pointer
    skip !SkipBytes
    db $43

; credit 4
org $00C1BA
    dl NewCredit4Pointer
    skip !SkipBytes
    db $A3

; credit 5
org $00C1C5
    dl NewCredit5Pointer
    skip !SkipBytes
    db $83

; credit 6
org $00C1D0
    dl NewCredit6Pointer
    skip !SkipBytes
    db $A3

; credit 7
org $00C1DB
    dl NewCredit7Pointer
    skip !SkipBytes
    db $A3

; credit 8
org $00C1E6
    dl NewCredit8Pointer
    skip !SkipBytes
    db $83

; credit 9
org $00C1F1
    dl NewCredit9Pointer
    skip !SkipBytes
    db $A3

; credit 10
org $00C1FC
    dl NewCredit10Pointer
    skip !SkipBytes
    db $A3

; credit 11
org $00C207
    dl NewCredit11Pointer
    skip !SkipBytes
    db $83

; credit 12
org $00C212
    dl NewCredit12Pointer
    skip !SkipBytes
    db $61

; credit 13
org $00C21D
    dl NewCredit13Pointer
    skip !SkipBytes
    db $A3

; credit 14
org $00C228
    dl NewCredit14Pointer
    skip !SkipBytes
    db $95

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Translate 終 to "END" and 完 to "FIN"
; Both default screen positions work fine for my replacement images, but I will
; include the pointers to the graphics structures as comments anyway.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; 終 to "END"

; org $00C233
; skip $9
; db $8A

; quick reminder: change # tiles, and insert new tileset
org $08CD50
    dw $0037
    incbin "graphics/recompressed 15 END TILES - no empty tile.bin" -> $08CD54

; change width and height, and insert new tilemap
org $08CFDE
    db $0C,$05
    incbin "graphics/recompressed 15 END tilemap 0x0C x 0x05.bin" -> $08CFE0

;;;;;;;;;;;;;;;;;;;;

; 完 to "FIN"

; org $00C23E
; skip $9
; db $8A

org $08D082
    dw $002E
    incbin "graphics/recompressed 16 FIN TILES - no empty tile.bin" -> $08D086

org $08D2FC
    db $0B,$05
    incbin "graphics/recompressed 16 FIN tilemap 0x0B x 0x05.bin" -> $08D2FE
