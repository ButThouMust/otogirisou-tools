includefrom "main.asm"

!CtrlCodeASMPointers = $00F390
!CtrlCodeArgTable = $00F41E

; ------------------------------------------------------------------------------

; select control code IDs for the new control codes; originally, these were all
; unused codes that change the text speed in the JP game
!KernLeftNum = $0A
!KernRightNum = $0B
!KernUpNum = $0D
!KernDownNum = $0E
!HonorificCtrlCodeNum = $0F

; Notes: I initially tried the unused slots for 1001, 1002, and 1006. When
; testing them in game, they adversely affected the game's control flow:
; - Setting up/down to 1001/1002 made printed text randomly kern up or down.
; - Setting 1006 to left made choice codes randomly print whatever character
;   is assigned to 0012, before the 'A' in the first choice option.
;   (Why 0012? $009F99 has uncomp. text [1E 10 12 00] = <SET X POS 1E><$0012>) 
; I don't know exactly why overwriting them doesn't work.

; ------------------------------------------------------------------------------

; set data about input arguments (what type, how many) for the new control codes

; set the kerning codes as using one Huffman script value as input
org !CtrlCodeArgTable+2*!KernUpNum
    db $01,$00
org !CtrlCodeArgTable+2*!KernDownNum
    db $01,$00
org !CtrlCodeArgTable+2*!KernLeftNum
    db $01,$00
org !CtrlCodeArgTable+2*!KernRightNum
    db $01,$00

; set "print arbitrary honorific" as the same
org !CtrlCodeArgTable+2*!HonorificCtrlCodeNum
    db $01,$00

; ------------------------------------------------------------------------------

; set pointers to the new control codes in the jump table

; see the kerning ASM file
org !CtrlCodeASMPointers+2*!KernUpNum
    dw KernUp
org !CtrlCodeASMPointers+2*!KernDownNum
    dw KernDown
org !CtrlCodeASMPointers+2*!KernLeftNum
    dw KernLeft
org !CtrlCodeASMPointers+2*!KernRightNum
    dw KernRight

; see the honorifics ASM file
org !CtrlCodeASMPointers+2*!HonorificCtrlCodeNum
    dw PrintHonorific
