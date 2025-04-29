asar 1.90
check title "OTOGIRISOU           "

lorom

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; decompresses Huffman code from script, puts it into A
GetScriptValue = $00ADE1

; relevant memory addresses
TextXPos = $1A7F
TextYPos = $1A81

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

org $00FAA5
; reasoning for this position: it is the start of a big block of FF bytes after
; a block of palette values from $00FA77 to $00FAA4

; add to text X position (kern RIGHT) 
KernRight:
    rep #$30
    jsr GetScriptValue
    clc
AddXOffset:
    adc TextXPos
    sta TextXPos
    rts

; subtract from text X position (kern LEFT)
KernLeft:
    rep #$30
    jsr GetScriptValue
    ; reverse subtraction because # pixels we want to shift left by is in A
    ; which is to say ['~' is binary NOT]: addr - A = addr + ~A + 1
    eor #$ffff
    sec
    bra AddXOffset

; add to text Y position (kern DOWN)
KernDown:
    rep #$30
    jsr GetScriptValue
    clc
AddYOffset:
    adc TextYPos
    sta TextYPos
    rts

; subtract from text Y position (kern UP)
KernUp:
    rep #$30
    jsr GetScriptValue
    ; same logic as kern left; do reverse subtraction on # pixels to shift up by
    eor #$ffff
    sec
    bra AddYOffset

; insert code for new linebreaking after the kerning code
; I want to next incorporate the kerning code's pointers into the game code
pushpc

;;;;;;;;;;;;;;;;;;;;

; replace ptrs for ctrl codes 100A, 100B, 100D, 100E with the four kerning codes

; Reasoning for why I chose these particular slots: they are unused codes that
; normally would change the speed of the text. I felt they were safe to replace.

; Important: I initially tried using the slots for 1001, 1002, and 1006. When I
;   tested these in game, they adversely affected the game's control flow:
;   - Setting up/down to 1001/1002 made printed text randomly kern up or down.
;   - Setting 1006 to left made choice codes randomly print whatever character
;     is assigned to 0012, before the 'A' in the first choice option.
;     (Why 0012? $009F99 has uncomp. text [1E 10 12 00] = <SET X POS 1E><$0012>)

!CtrlCodeASMPointers = $00F390
!KernLeftNum = $0A
!KernRightNum = $0B
!KernUpNum = $0D
!KernDownNum = $0E

org (!CtrlCodeASMPointers+2*!KernUpNum)
    dw KernUp

org (!CtrlCodeASMPointers+2*!KernDownNum)
    dw KernDown

org (!CtrlCodeASMPointers+2*!KernLeftNum)
    dw KernLeft

org (!CtrlCodeASMPointers+2*!KernRightNum)
    dw KernRight

;;;;;;;;;;;;;;;;;;;;

; set data about arguments for each control code (what kind, how many)
; all 4 codes take one arg that is a regular script value; data should be 01 00

!CtrlCodeArgTable = $00F41E

org (!CtrlCodeArgTable+2*!KernUpNum)
    db $01,$00

org (!CtrlCodeArgTable+2*!KernDownNum)
    db $01,$00

org (!CtrlCodeArgTable+2*!KernLeftNum)
    db $01,$00

org (!CtrlCodeArgTable+2*!KernRightNum)
    db $01,$00

;;;;;;;;;;;;;;;;;;;;

; If the game gets a control code in text when printing a choice option, it will
; only execute the code if it is in a specific list at $00AC01. By default:
; - END CHOICE 1C, EC HELPER 1B, NAME-SAN 20, NAME 21, LINE 00, SET X POS 1E,
;   SET Y POS 1F, DELAY 17, WAIT 16
; Format: The code's value, with top 1 cleared out, e.g. 1019 -> 0019 -> [19 00]
; Purpose: Do not change graphics, start/stop sounds, do not take JMP codes

; Why I bring this up: I noticed in playtesting that text would not kern when
; printing a choice but would kern once I picked it and had it print "normally."

; Of the codes listed, I felt that WAIT 16 was the best option to replace (this
; means the option can have WAITs but only let it trigger when the player picks
; the appropriate option). However, the SET X/Y POS codes are just as good.

; ASSUMPTION: no choice options in the script will contain kern up/down/right.
org $00AC11
    db !KernLeftNum

;;;;;;;;;;;;;;;;;;;;

; Similar issue for when the player presses X or Y to view previous pages.
; Game will check another list at $00E095. By default:
; - CHOICE 19, CHOICE 1A, END CHOICE 1C, LINE 00, EC HELPER 1B, SET X POS 1E,
;   SET Y POS 1F, NAME-SAN 20, NAME 21
; Same format: code's value, with top 1 cleared, e.g. 1019 -> 0019 -> [19 00]

; I had initially thought LINE 00 would be covered under some ASM near here
; ($00E054; directly run code if ID <= 1005, so LINE 00, JMP 03, JMP.cc 04).
; This is true if you scroll back/forth onto normal text, but not if you scroll
; up when the game gives you a choice. Specifically, replacing LINE 00 with
; KERN LEFT messes up the linebreaking for the options when you eventually
; scroll back down to the choice.

; Preferably, we want to keep the kerning consistent no matter if text appears
; character by character or one screen at a time. So this necessitates making
; a larger list at some other location in the ROM, and updating the pointer in
; the instruction at $00E05C appropriately.

; May as well insert the new list after the kerning code.
; Copy the old list, add the four kerning codes, and add the FFFF list ender.

!ScrollingCodeListStart = $06095
!ScrollingCodeListEnd = $060A7
pullpc
NewScrollingCodeList:
    incbin "rom/Otogirisou (Japan).sfc":!ScrollingCodeListStart..!ScrollingCodeListEnd
    dw !KernLeftNum, !KernRightNum, !KernUpNum, !KernDownNum
    dw $FFFF
pushpc

; update the pointer to this list of control code IDs
org $00E05D
    dw NewScrollingCodeList

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Integrating the kerning code is done. Now put in the new linebreaking logic.

; I will definitely admit it is not perfect, but it mostly accomplishes its goal
; of not splitting up words in the middle when doing automatic linebreaks.

; Ideal solution: have a way to calculate the pixel width of the next word.
; However, I don't know a way in Otogirisou to cleanly skip characters forwards
; or backwards like what Kamaitachi no Yoru gives you ($400 byte char buffer).

; Fix edge cases by putting non-breaking spaces "\ " (break too early, when word
; could fit beyond margin) and LINEs (break too late, when word doesn't fit in
; margin and overflows right edge of screen) in the script. My hope is that this
; code cuts down on a lot of the manual formatting.

MostRecentChar = $1A7D
CharWidth = $1A83
Spacing = $18DB
LINE_00 = $00A7A2

RightTextBoundVar = $18CF
!RightScreenEdge = #$0100

; change this value as you see fit to have text look decent enough without
; manually putting LINE codes everywhere in the script; I think 0xD0 pixels
; (24 pixel margin on right of screen) is an adequate choice for English
; search for [C9 D0 00] to change without having to reassemble all over again
!RightTextBound = #$00D0

pullpc
CalcNewXPos16Bit:
    php
    rep #$20
    clc
    lda.w TextXPos
    adc.w CharWidth
    adc.w Spacing
    sta.w TextXPos
    plp
    rts

LineBreakNewLogic:
    rep #$30
    lda.w TextXPos               ; assume new X position already pre-calculated

    cmp.w !RightScreenEdge        ; if char would go past right edge of screen,
    bcs PerformAutoLineBreak      ;   force a line break; keep from original

    cmp.w !RightTextBound         ; if not past horizontal limit for text,
    bcc DoNotLineBreak            ;   don't have to do anything

    ldx.w #$0000                  ; otherwise, perform an automatic line break
                                  ; at first opportunity (space, punctuation)
Loop:
    lda.w CharsCanAutoLineBreak,X ; get char value from list, advance list ptr
    inx                           ; 
    inx                           ; 
    cmp.w #$FFFF                  ; if no matches in whole list,
    beq DoNotLineBreak            ;   print on current line

    cmp.w MostRecentChar          ; compare space/punct. value with script char
    bne Loop                      ; if no match, check next value in list

PerformAutoLineBreak:             ; if match (got punctuation or whitespace),
    jsr.w LINE_00                 ;   print it on current line
DoNotLineBreak:
    rts

CharsCanAutoLineBreak:
  ; when generating the font for the game, I used the table file to generate a
  ; list of these characters' hex values as a binary file; insert contents here
  ; because Asar doesn't support 16-bit table encodings, oh well
  incbin "font/auto linebreak chars.bin"

pushpc

;;;;;;;;;;;;;;;;;;;;

; Aside: the file select and name entry screens work fine with the stock logic,
; so their control flow is unmodified.

; control flow for linebreaking in main gameplay mode
org $00ABEA
    lda.w MostRecentChar
    jsr.w $ac15     ; check for character with auto WAIT or auto DELAY
    jsr.w $ac79     ; write Y pos. to some sort of table - not of concern here
    jsr.w $a4db     ; read font data
LineBreakControlFlow:
    ; jsr.w $a777   ; original Japanese-style linebreaking routine
    ; jsr.w $a611   ; write font data to buffer in bank 7F, to be DMA'd to VRAM
    ; jsr.w $ac96   ; calculate X coordinate for next character; only 8-bit A!

    jsr.w $a611             ; first, draw the character, assuming no overflow
    jsr.w CalcNewXPos16Bit  ; calculate next X position but with 16-bit A
    jsr.w LineBreakNewLogic ; now check if need to linebreak for the next char
    rts

;;;;;;;;;;;;;;;;;;;;

; control flow for linebreaking when reading previous pages by pressing X/Y
; just copy in the new control flow used for the main gameplay

org $00E085         ; important: there is no check for auto waits/delays here
    jsr.w $ac79     ; write Y pos. to some sort of table - not of concern here
    jsr.w $a4db     ; read font data
    ; jsr.w $a777   ; original Japanese-style linebreaking routine
    ; jsr.w $a611   ; write font data to buffer in bank 7F, to be DMA'd to VRAM
    ; jsr.w $ac96   ; calculate X coordinate for next character; only 8-bit A!

                            ; important that it uses $00A61C, not $00A611
    jsr.w $a61c             ; first, draw the character, assuming no overflow
    jsr.w CalcNewXPos16Bit  ; calculate next X position but with 16-bit A
    jsr.w LineBreakNewLogic ; now check if need to linebreak for the next char
    rts

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; ASM hack for implementing an option for "enable/disable honorifics"
; TL;DR is that the protagonist's girlfriend uses a different honorific for him
; on each playthrough, with a list in the ROM; I realize that not everyone likes
; leaving in honorifics like "Player-san", so giving the option for it

; for now, implemented as having two patch versions: one for "on," one for "off"

; GOAL: if game starts with the R button pressed, turn off the honorifics
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
    ; jump over the code for "read P3/P4"
    ; jmp.w $8590

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

; for any other possible additions to this file in free space
pushpc

;;;;;;;;;;;;;;;;;;;;

; set pointers to the new control codes in the control code table

; set up pointer to code that just prints current honorific, instead of Name-San
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

pullpc
