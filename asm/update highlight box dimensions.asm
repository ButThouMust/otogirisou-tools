includefrom "main.asm"

; update OAM data for the highlight boxes; format is typical 4-byte OAM entry
; db X pos, Y pos, tile num, attributes

!yFlip = $80
!xFlip = $40
!xyFlip = $C0

!TopRow = $00
!MidRow = $08
!BotRow = $10

!Corner  = $00      ; two 7 pixel lines at corner for character grid highlight
!TopEdge = $01
!LeftEdge2 = $02    ; line is right on the edge of 8x8 block
!RightEdge = $03    ; line is offset one pixel from edge of 8x8 block
!TwoPixels = $04    ; two pixels on corner for file prompt highlights

org $02B7AB
CharGridHighlightSpriteData:
    ; use two more sprites here to make box taller and wider
    ; move the two right corners right, and fill in empty space with top edges
    db $00,$00,!Corner,$00
    db $04,$00,!TopEdge,$00
    db $0A,$00,!Corner,!xFlip

    db $00,$04,!LeftEdge2,$00
    db $0A,$04,!LeftEdge2,!xFlip

    db $00,$09,!Corner,!yFlip
    db $04,$09,!TopEdge,!yFlip
    db $0A,$09,!Corner,!xyFlip
    dw $FFFF

; org $02B7BD
BeginDeleteCancelHighlightSpriteData:
    ; width of the prompts' text lets me use two fewer sprites for the box by
    ; cutting out the top right and bottom right corners, moving right edge left
    db $00,!TopRow,!TwoPixels,$00
    db $08,!TopRow,!TopEdge,!yFlip
    db $10,!TopRow,!TopEdge,!yFlip
    db $18,!TopRow,!TopEdge,!yFlip
    db $20,!TopRow,!TopEdge,!yFlip
  ; db $28,!TopRow,!TwoPixels,!xFlip

    db $00,!MidRow,!RightEdge,!xFlip
    db $27,!MidRow,!LeftEdge2,$00

    db $00,!BotRow,!TwoPixels,!yFlip
    db $08,!BotRow,!TopEdge,$00
    db $10,!BotRow,!TopEdge,$00
    db $18,!BotRow,!TopEdge,$00
    db $20,!BotRow,!TopEdge,$00
  ; db $28,!BotRow,!TwoPixels,!xyFlip
    dw $FFFF

ResumeRestartCancelConfirmHighlightSpriteData:
    ; likewise, text lets me get rid of the top right and bottom right corners
    db $00,!TopRow,!TwoPixels,$00
    db $08,!TopRow,!TopEdge,!yFlip
    db $10,!TopRow,!TopEdge,!yFlip
    db $18,!TopRow,!TopEdge,!yFlip
    db $20,!TopRow,!TopEdge,!yFlip
    db $28,!TopRow,!TopEdge,!yFlip

    db $00,!MidRow,!RightEdge,!xFlip
    db $2F,!MidRow,!LeftEdge2,$00

    db $00,!BotRow,!TwoPixels,!yFlip
    db $08,!BotRow,!TopEdge,$00
    db $10,!BotRow,!TopEdge,$00
    db $18,!BotRow,!TopEdge,$00
    db $20,!BotRow,!TopEdge,$00
    db $28,!BotRow,!TopEdge,$00
    dw $FFFF

    assert pc() <= $02B839

org $02B502
    dw CharGridHighlightSpriteData
org $02BC83
    dw BeginDeleteCancelHighlightSpriteData
org $02BC8D
    dw ResumeRestartCancelConfirmHighlightSpriteData
