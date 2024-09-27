check title "OTOGIRISOU           "

lorom
math pri on

UnusedKnifeTileset = $079653
UnusedKnifeTilemap = $07997E
StartPoint = $E649ED

org $00BFBA
    dl UnusedKnifeTileset

org $00BFC0
    dl UnusedKnifeTilemap
    skip $5
    ; set top left tile of graphic to appear at tile position [X,Y] = [0xB,0x6]
    ; to have it more or less centered on screen
    db $00,$6B

; change the start point for the story to $1CC93D-5
org $02A627
    dl StartPoint
    dl StartPoint
    dl StartPoint
