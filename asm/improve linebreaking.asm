includefrom "asm/main.asm"

; After integrating the kerning code, put in the new linebreaking logic.
; I will definitely admit it is not perfect, but it mostly accomplishes its goal
; of not splitting up words in the middle when doing automatic linebreaks.

; Ideal solution: have a way to calculate the pixel width of the next word.
; However, I don't know a way in Otogirisou to cleanly skip characters forwards
; or backwards like what Kamaitachi no Yoru gives you ($400 byte char buffer).

; Fix edge cases by putting non-breaking spaces "\ " (break too early, when word
; could fit beyond margin) and LINEs (break too late, when word doesn't fit in
; margin and overflows right edge of screen) in the script. My hope is that this
; code cuts down on a lot of the manual formatting.

; MostRecentChar = $1A7D
; CharWidth = $1A83
Spacing = $18DB
LINE_00 = $00A7A2

RightTextBoundVar = $18CF
!RightScreenEdge = #$0100

; change this value as you see fit to have text look decent enough without
; manually putting LINE codes everywhere in the script; I think 0xD0 pixels
; (24 pixel margin on right of screen) is an adequate choice for English
; search for [C9 D0 00] to change without having to reassemble all over again
!RightTextBound = #$00D0

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
    lda.w TextXPos                ; assume new X pos already pre-calculated

; force line break if char would go past right edge of screen; keep from JP game
    cmp.w !RightScreenEdge
    bcs PerformAutoLineBreak

; if not past horizontal limit for text, don't have to do anything
    cmp.w !RightTextBound
    bcc DoNotLineBreak

; otherwise, do automatic line break at first opportunity (space, punctuation)
    ldx.w #$0000
-   lda.w CharsCanAutoLineBreak,X
    inx #2
    ; check list terminator, and current value in list
    cmp.w #$FFFF
    beq DoNotLineBreak
    cmp.w MostRecentChar
    bne -

; if match (got punctuation or whitespace), print on current line
; NEW: also disable kerning at the start of a line
PerformAutoLineBreak:
    jsr.w LINE_00
    jsr.w DisableKerning
DoNotLineBreak:
    rts

CharsCanAutoLineBreak:
  ; when generating the font for the game, I used the table file to generate a
  ; list of these characters' hex values as a binary file; insert contents here
  ; because Asar doesn't support 16-bit table encodings, oh well
  incbin "font/auto linebreak chars.bin"

; leave this here for where to put ASM hacks for honorifics
pushpc

;;;;;;;;;;;;;;;;;;;;

; Aside: the file select and name entry screens work fine with the stock logic,
; so their control flow is unmodified. However, documenting location and logic:
; org $02b6af
;     sta $1a7d       ; set most recent character
;     jsl $009595     ; JSL to load font data
;     jsl $00958d     ; JSL to check horizontal overflow
;     jsl $00959d     ; JSL to write font data to bank 7F
;     jsl $00957d     ; JSL to calculate text X position
;     rts

;;;;;;;;;;;;;;;;;;;;

; control flow for linebreaking in main gameplay mode
; get rid of the useless JSR/BNE with $00AC8F
; org $00abe4
org NewLinebreakingCtrlFlow
    sta.w MostRecentChar
  ; jsr $00ac8f
  ; bne $00abde
  ; lda.w MostRecentChar
    jsr.w $ac15     ; check for character with auto WAIT or auto DELAY
    jsr.w $ac79     ; write Y pos. for choice arrows to table if needed
    jsr.w $a4db     ; read font data
LineBreakControlFlow:
    ; jsr.w $a777   ; original Japanese-style linebreaking routine
    ; jsr.w $a611   ; write font data to buffer in bank 7F, to be DMA'd to VRAM
    ; jmp.w $ac96   ; calculate X coordinate for next character; only 8-bit A!

    jsr.w DoAutoKernLeft
    jsr.w $a611             ; first, draw the character, assuming no overflow
    jsr.w CalcNewXPos16Bit
    jmp.w LineBreakNewLogic ; now check if need to linebreak for the next char

assert pc() <= $00ac01
    fillbyte $ff
    fill $00ac01-pc()

;;;;;;;;;;;;;;;;;;;;

; control flow for linebreaking when reading previous pages by pressing X/Y
; just copy in the new control flow used for the main gameplay

org $00e080
  ; jsr.w $ac8f     ; again, get rid of this useless control flow here
  ; bne ToTheRtsBelow
; important: there is no check for auto waits/delays here
    jsr.w $ac79     ; write Y pos. for choice arrows to table if needed
    jsr.w $a4db     ; read font data
    ; jsr.w $a777   ; original Japanese-style linebreaking routine
    ; jsr.w $a611   ; write font data to buffer in bank 7F, to be DMA'd to VRAM
    ; jsr.w $ac96   ; calculate X coordinate for next character; only 8-bit A!

; important that this location uses $00A61C, not $00A611
    jsr.w DoAutoKernLeft
    jsr.w $a61c             ; first, draw the character, assuming no overflow
    jsr.w CalcNewXPos16Bit
    jsr.w LineBreakNewLogic ; now check if need to linebreak for the next char
    rts

assert pc() <= $00e095

;;;;;;;;;;;;;;;;;;;;

; Since we're on the subject of improving how the text prints, change some
; constants related to text printing for the novel text.

; There's another set of these for the name entry and file select screens, which
; are taken care of in an Atlas file along with those screens' set text.

; set X coordinate for a new line of text (not in choice), default 0xC
org $0098ee
    dw $000c

; set Y coordinate for new screen of text; default is 0x10
org $0098fb
    dw $0011

; set the height of the first row of characters (?); default is 0xE
; set this to the height of the tallest character in your font
; I found that having, for example, characters being 0xF pixels tall with this
; value being 0xE would cut off the top row of pixels in them
org $0098af
    dw $000f

; in JP game, the right margin for novel text gets set to 0xF2
; I don't use this in the patch, but documenting here anyway
; org $009908
    ; dw $00f2

; set pixel spacing between characters; default is 0x2
org $00992f
    dw $0001

; when messing around with these values, I found that you can make the text
; print right-to-left if you make the spacing value negative (e.g. 0xFFF4 "-12")
; and the start of text on a line to the right edge of the screen (e.g. 0xE8)
; note that you would also have to check X position against a left margin

; set height of the line break, measured from the bottom of each character row
; default is 0x17 = 0xF + 0x8? max char height + 8 pixels leading, up to 9 lines
; you can fit more lines on screen: 11 if you pick 0x13 or 10 if you pick 0x14
; however, I found that using 0x13 messed up the game's system for coloring in
; choices' text depending on whether they're selected or not, due to tile bounds
; you can get 11 lines with 0x14 if you kern up by a tile
org $009922
    dw $0014

; set X coordinate for printing a choice letter
org $009fa1
    dw $0012

; set X coordinate for printing the text in a choice option
org $009fa7
    dw $0020
org $009ecb
    dw $0020

; offset the "advance text" icon that shows up during WAITs and CHOICEs
; may have to space up to match with baselines of English letters instead of
; the descenders (tails for letters like 'y' or 'g'); default [00 02] = [XX YY]
org $009c8b
    db $00,$00
