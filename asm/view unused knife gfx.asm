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
    db $00,$6B

org $02A627
    dl StartPoint
    dl StartPoint
    dl StartPoint
