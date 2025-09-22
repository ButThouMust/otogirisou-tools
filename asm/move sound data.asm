includefrom "main.asm"

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
    incbin "!JProm":!OldMusicBlockStartROM..!OldMusicBlockEndROM

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
