asar 1.90
check title "OTOGIRISOU           "
lorom

; ----------------------

; At certain points in the game, the script will make it so the background image
; is completely white. I found that this can make text somewhat difficult to
; read because it is a slightly brighter shade of white compared to the
; background shade.

; The font shadowing in Otogirisou is generated on-the-fly in the subroutine
; $00A6FF (called at $00A6E3). Qualitatively speaking, it copies each font pixel
; once directly to the right, and once down and to the right. Example with 'a':

;   0123456         0123456         0123456
;  +-------+       +-------+       +-------+
; 0| ###   |      0| ###-  |      0| ###-  |  Goal: Modify shadowing subroutine
; 1|#   #  |      1|#---#- |      1|#---#- |  to also copy a font pixel directly
; 2|  ###  | orig 2| -###- | goal 2|x-###- |  down by 1.
; 3| #  #  | ---> 3| #--#- | ---> 3| #--#- |
; 4|#   #  | add  4|#-- #- | add  4|#-- #- |
; 5|#   #  | '-'  5|#-  #- | 'x'  5|#-  #- |
; 6| ### # |      6| ###-#-|      6|x###-#-|
; 7|       |      7|  --- -|      7| x---x-|
;  +-------+       +-------+       +-------+

; Assumptions:
; - The row of pixel data that is currently being processed in the character is
;   in $10-$13 ($10 = extra bit right, $11 = right, $12 = middle, $13 = left).
; - When calling the $00A6FF shadowing routine, this 4 byte block is immediately
;   shifted right by 1 pixel.

; It is sufficient to take the current state of $10-$13, copy any set bits left
; once, and update the buffer. For example,  4A (0100 1010) -> DE (1101 1110),
; which after being shifted right @ $00A6FF: 6F (0110 1111).
; The existing shadowing routine already takes care of "don't set as shadow if
; already for the main font" and such.

; ----------------------

MainShadowing = $00A6FF

; This implementation takes 0x16 bytes. I put it 0x20 bytes before the end of a
; block of [FF] bytes in bank 00 before the SNES header data block at $00FFC0.
org $00FFA0
AddShadowOnePixelDown:
    rep #$20
    lda $10
    asl a
    ora $10
    sta $10

    lda $12
    ; the bit that gets left shifted out here needs to be rotated back in
    rol a
    ora $12
    sta $12

    ; original shadowing subroutine assumes that M flag is set (8-bit arith)
    sep #$20
    jmp MainShadowing
    ; jsr MainShadowing
    ; rts

; ----------------------

org MainShadowing
    phx
    ror $13
    ; start of the original routine, for your convenience:
    ;   phx
    ;   lsr $13 <- change to ROR due to shifted out bit from above LDA $12 ; ROL
    ;   ror $12
    ;   ror $11
    ;   ror $10

; ----------------------

org $00A6E3
    ; jsr MainShadowing
    jsr AddShadowOnePixelDown
