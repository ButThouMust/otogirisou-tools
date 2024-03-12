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

; replace file select prompts like "begin" or "delete"
; tileset must be <= 536 bytes; tilemap must be <= 389 bytes
incbin "graphics/recompressed TL'd file select TILES.bin" -> $07E637
incbin "graphics/recompressed TL'd file select TILEMAP.bin" -> $07E855

; use new dimensions for the rectangles that surround file select prompts
; update which sprite IDs are used, and update a pointer to one of them
incbin "graphics/new file select highlight rectangles.bin" -> $02B7BD
org $02BC8D
    dw $B7F7

; replace the 4 buttons in the bottom right of the name entry screen
; notice that the tilemap is unchanged
; tileset must be <= 1346 bytes
incbin "graphics/recompressed TL'd name entry TILES.bin" -> $07DD87

; replace the graphic that reads "奈美" with one that reads "Nami"
; tileset must be <= 1336 bytes; tilemap must be <= 163 bytes
incbin "graphics/recompressed TL'd nami TILES.bin" -> $07A4EA
incbin "graphics/recompressed TL'd nami TILEMAP.bin" -> $07AA34
; also update the number of tiles that are copied in
org $07A4E6
    db $42

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

; replace the tileset used for the credits, and update # tiles for it
; notice no "-> $address" on the incbin; this is to keep the PC's value from
; directly after the inserted file, so we can assign it to a label
org $08AA0A
    dw $02DC
    skip $2
    incbin "graphics/recompressed 00 english credits TILES - no empty tile.bin"
    warnpc !PaletteForEndFinPtr

; basic template for setting up new tilemaps: get pointer to start of metadata
NewCredit1Pointer:
    ; grab 4 bytes from the original pointer
    OldPtr1 = $44282
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr1)-(OldPtr1+!NumMetadataBytesToCopy)
    ; set the new width and height of the tilemap
    db $16,$07
    ; insert the new tilemap's binary data
    incbin "graphics/recompressed 01 supervisor author RLE TILEMAP 0x16 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit2Pointer:
    OldPtr2 = $44307
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr2)-(OldPtr2+!NumMetadataBytesToCopy)
    db $14,$07
    incbin "graphics/recompressed 02 planner RLE TILEMAP 0x14 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit3Pointer:
    OldPtr3 = $4436A
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr3)-(OldPtr3+!NumMetadataBytesToCopy)
    db $16,$13
    incbin "graphics/recompressed 03 writer RLE TILEMAP 0x16 x 0x13.bin"
    warnpc !PaletteForEndFinPtr

NewCredit4Pointer:
    OldPtr4 = $44491
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr4)-(OldPtr4+!NumMetadataBytesToCopy)
    db $14,$07
    incbin "graphics/recompressed 04 art director RLE TILEMAP 0x14 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit5Pointer:
    OldPtr5 = $4450A
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr5)-(OldPtr5+!NumMetadataBytesToCopy)
    db $15,$0B
    incbin "graphics/recompressed 05 graphics RLE TILEMAP 0x15 x 0x0B.bin"
    warnpc !PaletteForEndFinPtr

NewCredit6Pointer:
    OldPtr6 = $445CC
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr6)-(OldPtr6+!NumMetadataBytesToCopy)
    db $1A,$07
    incbin "graphics/recompressed 06 composer RLE TILEMAP 0x1A x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit7Pointer:
    OldPtr7 = $44675
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr7)-(OldPtr7+!NumMetadataBytesToCopy)
    db $1A,$07
    incbin "graphics/recompressed 07 programming director RLE TILEMAP 0x1A x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit8Pointer:
    OldPtr8 = $446FE
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr8)-(OldPtr8+!NumMetadataBytesToCopy)
    db $16,$0B
    incbin "graphics/recompressed 08 programmer RLE TILEMAP 0x16 x 0x0B.bin"
    warnpc !PaletteForEndFinPtr

NewCredit9Pointer:
    OldPtr9 = $447E3
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr9)-(OldPtr9+!NumMetadataBytesToCopy)
    db $17,$07
    incbin "graphics/recompressed 09 sound programmer RLE TILEMAP 0x17 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit10Pointer:
    OldPtr10 = $44880
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr10)-(OldPtr10+!NumMetadataBytesToCopy)
    db $1B,$07
    incbin "graphics/recompressed 10 original sfx RLE TILEMAP 0x1B x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit11Pointer:
    OldPtr11 = $4493F
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr11)-(OldPtr11+!NumMetadataBytesToCopy)
    db $19,$0B
    incbin "graphics/recompressed 11 support pg 1 RLE TILEMAP 0x19 x 0x0B.bin"
    warnpc !PaletteForEndFinPtr

NewCredit12Pointer:
    OldPtr12 = $44A2E
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr12)-(OldPtr12+!NumMetadataBytesToCopy)
    db $1F,$0F
    incbin "graphics/recompressed 12 support pg 2 RLE TILEMAP 0x1F x 0x0F.bin"
    warnpc !PaletteForEndFinPtr

NewCredit13Pointer:
    OldPtr13 = $44B94
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr13)-(OldPtr13+!NumMetadataBytesToCopy)
    db $16,$07
    incbin "graphics/recompressed 13 producer director RLE TILEMAP 0x16 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

NewCredit14Pointer:
    OldPtr14 = $44C3F
    incbin "rom/Otogirisou (Japan).sfc":(OldPtr14)-(OldPtr14+!NumMetadataBytesToCopy)
    db $16,$07
    incbin "graphics/recompressed 14 (c) chunsoft RLE TILEMAP 0x16 x 0x07.bin"
    warnpc !PaletteForEndFinPtr

;;;;;;;;;;;;;;;;;;;;

; now that all of the tilemaps are inserted with their pointers accounted for,
; go to each one's structure list and update their pointers
; also update the Y/X tile positions of each tilemap on screen

; I suggest keeping the X positions as consistent as possible, and centering the 
; Y positions based on a screen height of 224 pixels = 1C tiles.
; Tilemap heights I used (7, B, F, 13) -> Y positions (A, 8, 6, 4)
; However, keep the 1st credit at Y position 9 so it appears on cue with music.

; credit 1
org $00C192
    dl NewCredit1Pointer
    skip 6
    db $93

; credit 2
org $00C1A4
    dl NewCredit2Pointer
    skip 6
    db $A3

; credit 3
org $00C1AF
    dl NewCredit3Pointer
    skip 6
    db $43

; credit 4
org $00C1BA
    dl NewCredit4Pointer
    skip 6
    db $A3

; credit 5
org $00C1C5
    dl NewCredit5Pointer
    skip 6
    db $83

; credit 6
org $00C1D0
    dl NewCredit6Pointer
    skip 6
    db $A3

; credit 7
org $00C1DB
    dl NewCredit7Pointer
    skip 6
    db $A3

; credit 8
org $00C1E6
    dl NewCredit8Pointer
    skip 6
    db $83

; credit 9
org $00C1F1
    dl NewCredit9Pointer
    skip 6
    db $A3

; credit 10
org $00C1FC
    dl NewCredit10Pointer
    skip 6
    db $A3

; credit 11
org $00C207
    dl NewCredit11Pointer
    skip 6
    db $83

; credit 12
org $00C212
    dl NewCredit12Pointer
    skip 6
    db $61

; credit 13
org $00C21D
    dl NewCredit13Pointer
    skip 6
    db $A3

; credit 14
org $00C228
    dl NewCredit14Pointer
    skip 6
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
