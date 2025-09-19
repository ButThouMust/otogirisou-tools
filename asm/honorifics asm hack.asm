includefrom "main.asm"

; ASM hack for implementing an option for "enable/disable honorifics"
; TL;DR is that the protagonist's girlfriend uses a different honorific for him
; on each playthrough, with a list in the ROM

; On one hand, I think this technically counts as a "game mechanic" since they
; change on each playthrough, but on the other, I recognize that not everyone
; likes leaving in honorifics like "Kouhei-san", so giving the option for it.

; for now, implemented as having two patch versions: one for "on," one for "off"

; idea I had: if game starts with the R button pressed, turn off the honorifics
; can switch this to "if R button pressed, turn ON" if desired
; R button because it is unused in terms of control flow purposes

; for some reason, despite being a single player game, Otogirisou reads input
; from control ports 1-4 in subroutine at $008570, specifically as:
; P1 -> $0A25,  P2 -> $0A27,  P3 -> $0A29,  P4 -> $0A2B
; the four bytes for P3/P4's input ($0A29 - $0A2C) should be free for the taking

Player1Input = $0A25
UseHonorificFlag = $0A29

; 008584    ldx.w $421c     read P3
;           stx.w $0a29
;           ldx.w $421e     read P4
;           stx.w $0a2b
; 008590    lda.w $4016     
;           lsr a
;           bcs $06
;           stz.w $0a25
;           stz.w $0a26
; 00859C    rts
org $008584
    ; "two patches" version
    ldx.w #$0001

;;;;;;;;;;;;;;;;;;;;

; to check for R button being pressed, take value in $0A25, bitwise AND with
; 0x0010 (bit 4), and check if result is zero or not
; GetRButton:
    ; rep #$20
    ; lda.w Player1Input
    ; and.w #$0010
    ; rts

;;;;;;;;;;;;;;;;;;;;

; note: honorifics are longer in English than Japanese (1-4 chars -> 4-7 chars)
; so if you write a long name and an honorific, possible to overflow the string
; buffer at $5F when using the NAME-SAN 20 control code as-is

; this section accomplishes two things:
; - change NAME-SAN into just "-SAN" (only print the honorific)
; - intercept NAME-SAN control code to only print honorific if the flag is on
; tradeoff: instances of <NAME-SAN 20> must now be encoded as <NAME 21><-SAN 20>
; i.e. they take more space in the script

; original ASM of NAME-SAN 20 for your convenience:
;
; NAME_SAN_20 = $A83A
; NAME_21 = $A825
;
; org $00A83A
;     jsr.w NAME_21         ; copy player's name with FFFF terminator into $5F
;     dex                   ; the DEXes set honorific to start after the name,
;     dex                   ;   specifically on the FFFF terminator
; org $00a83f
;     lda.l $7005a8         ; read ID # for which honorific to use
; org $00a843
;     cmp.w #$000c          ; process ID # and read an honorific to $5F buffer
;     bcc $03               ; if ID value not in range 0-B, default to san
;     lda #$0000
;     cmp #$0002            ; check if ID value is for "no honorific"
;     beq $1e               ; if yes, then branch to an RTS = do nothing
;     ...

!NoHonorific = #$0002

; see ASM file for improved linebreaking
pullpc

PrintCurrHonorificCtrlCode:
    rep #$30
    lda.w UseHonorificFlag  ; if should not use honorifics, do nothing
    beq HonorificRTS

    lda.l $7005a8           ; - only write honorific if not 2 ("no honorific")
    cmp.w !NoHonorific      ; not checking this here makes game print name twice
    beq   HonorificRTS

                            ; note: this 10 byte block of code gets reused below
HonorificCheckOkay:
    ldx.w #$0000            ; set to write text to start of buffer at $5F
    jsr.w $a83f             ; run just the original "print curr. honorific" code
    jsr.w $adf5             ; signal that text to be printed is in $5F
HonorificRTS:
    rts

;;;;;;;;;;;;;;;;;;;;

; implement a control code that prints an honorific from the list in the ROM,
; given an index into it; purpose is to handle any "hard-coded" honorifics in
; the script

PrintHonorific:
    rep #$30
    lda.w UseHonorificFlag
    bne HonorificFlagOn
DoNotUseHonorific:
    jsr.w GetScriptValue    ; if should not use honorifics, just read the value
    rts                     ; and RTS
HonorificFlagOn:
    jsr.w GetScriptValue    ; get index for honorific list from script
    cmp.w !NoHonorific      ; - only write honorific if not 2 ("no honorific")
    beq   HonorificRTS
    cmp.w #$000C            ;   also check if index is within bounds for list
    bcs HonorificRTS
    bra HonorificCheckOkay

;     ldx.w #$0000            ; set to write text to start of buffer at $5F
;     jsr.w $a843             ; reuse <NAME-SAN 20>'s code for printing honorific
;     jsr.w $adf5             ; signal that text to be printed is in $5F
; HonorificRTS:
    ; rts

; for any other possible ASM hacks in bank 00
; pushpc

;;;;;;;;;;;;;;;;;;;;

; set pointers to the new control codes in the control code table

; set up ptr to code that just prints current honorific, instead of Kouhei-san
org (!CtrlCodeASMPointers+2*$20)
    dw PrintCurrHonorificCtrlCode

; reasoning for position: overwrite an unused control code for altering the text
; printing speed
!HonorificCtrlCodeNum = $0F

org (!CtrlCodeASMPointers+2*!HonorificCtrlCodeNum)
    dw PrintHonorific

; again, take one argument that is a regular script value
org (!CtrlCodeArgTable+2*!HonorificCtrlCodeNum)
    db $01,$00

;;;;;;;;;;;;;;;;;;;;

; on the topic of honorifics, the existing space for honorifics is too small for
; their English replacements (0x7C vs 0x46), so clear out the original space
; if you're desperate for space, you can also clear out Seiichirou (Nagahata)'s
; name, which is in the ROM from $02AFEA-$02aff7
org $02aff8
    fillbyte $ff
    fill $02b03e-pc()

;;;;;;;;;;;;;;;;;;;;

; The game's code for determining which honorific to use is at $01F854.
; Short version: randomly chosen, with the rules depending on game progress.

; There's some extra logic where if combining the honorific and name would be
; too long of a string, it will generate another random value and use it to read
; from a pre-determined list of honorific IDs. Only happens if chosen honorific
; is either ちゃん (chan, L=6), センパイ (senpai, L=5+), or チーフ (chief, L=6).

; 0 playthroughs: random between 0, 1 (san, kun hiragana)
; 1+ playthrough: random between 0, 1, 2 (san, kun hiragana, no honorific)

; If you have viewed one particular ending (flag 0x66), generate a random value
; N from 0-7. If N is 3 (chan) and if the player's name is 6 characters long,
; generate another random value from 0-7 to index the list [0 1 2 4 5 6 7 0]
; i.e. just pick another random value from 0-7 and make sure it's not 3.
; Otherwise, use the original generated value N as the honorific ID.

; If you have the pink bookmark, generate a random value from 0-B. If N is
; [3 (chan) or A (chief) with a 6 char name, or 8 (senpai) with a 5+ char name],
; generate a random value from 0-8 to index the list [0 1 2 4 5 6 7 9 B] i.e.
; just pick another random value from 0-B and make sure it's not 3, 8, or A.

; With the dedicated "print current honorific" control code making it print
; separately from the name, we don't need to restrict the randomly chosen
; honorific anymore. Saves quite a lot of space, too.

JslToGenerateRandomByte = $009357
HonorificValueSRAM = $7005a8

org $01f854
    lda $1bbd
    lsr
    bcc CheckIfViewedParticularEnding

PickHonorificForPinkBookmarkFile:
    ; calculate ((rand_byte * 0xC) / 0x100) & 0xF -> random value from 00-0B
    jsl JslToGenerateRandomByte
    asl #2      ; rand*4 + rand*8 = rand*0xC
    pha
    asl
    adc $01,s
    plx

    xba
    and #$000f
GotHonorificValue:
    sta.l HonorificValueSRAM
-   rts

CheckIfViewedParticularEnding:
    lsr
    bcc -

PickHonorificForFileWithParticularEnding:
    jsl JslToGenerateRandomByte
  ; I suppose `AND #$0007` should work fine, but I'll follow Chunsoft's example
    and #$000e
    lsr
    bra GotHonorificValue

; save 0x93 bytes
fillbyte $ff
fill $01F90E-pc()
