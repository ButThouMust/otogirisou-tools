# Title logo
![Tilemap Studio screenshot](JP%20title%20logo%20-%20Tilemap%20Studio.png)

| GFX ID(s) | Tileset(s) | Tilemap(s) | Description |
| --------- | ---------- | ---------- | ----------- |
| 0x5A | $07CC6B | $07D4B5 | title logo |

You can translate the title logo however you like. I personally decided
to keep the three kanji 弟切草 and replace the furigana おとぎりそう on the
right with "Otogirisou".

When I first announced this project on the RHDN Discord, Lazermutt4 provided a
mockup of this (the font is "Cienfuegos"), with two options for spacing.
One spaced the letters uniformly across the whole height, and the other
scrunched their spacing to each kanji.

At the time, I was unaware of superfamiconv or Tilemap Studio, or how to change
the number of tiles in the game's graphics system. As a result, I tried to see
if I could manually edit the tileset and tilemap to fit them in. I still have
the notes for manually editing the tilemap and what kana tiles to replace with
what letters; they are here ~~for your viewing pleasure~~ so you can understand
the madness. For your own sanity, just use those two programs instead, please!

For some context, the original JP graphic uses 0x8B tiles, of which 0x1F are
dedicated to the furigana (31 of 139). I found that by crunching the spacing
slightly more than what Lazermutt4 did, I could reuse two tiles and fit the new
text into 31 tiles.

Below is the mockup that would eventually be used in the patch.

![patched title screen mockup](translated%20title%20logo%20mockup.png)