check title "OTOGIRISOU           "

lorom
math pri on

; This will contain any miscellaneous assembly hacks that I find that are at
; least somewhat interesting but do not need to be included in the patch.

; If you change this instruction from CLC to SEC, you can skip the opening
; "Chunsoft Presents" animation by pressing any button.
; However, it doesn't seem to speed up getting to the title screen sequence.
org $02806F
  sec

; There is an unused, um, "debugging" (?) function in the game that checks if
; the byte at $008000 has its MSB set. In the final game, this was set to [FF],
; so it is normally dummied out. However, if you set it to anything from 00 to
; 7F, you can re-enable it.
;
; See $009977. If player 1 presses Start in the main gameplay mode, it will
; "pause" the script engine from advancing at all until Start is pressed again.
; Animations such as the scrolling forest with the car will still play.
; I don't really know the purpose behind this, but feel free to mess around with
; it if you like.
org $008000
  db $00
