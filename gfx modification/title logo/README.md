# Title logo
[insert Tilemap Studio screenshot]

| GFX ID(s) | Tileset(s) | Tilemap(s) | Description |
| --------- | ---------- | ---------- | ----------- |
| 0x5A | $07CC6B | $07D4B5 | title logo |

Again, you can translate the title logo however you like. I personally decided
to keep the three kanji 弟切草 and replace the furigana おとぎりそう on the
right with "Otogirisou".

Lazermutt4 had provided a mockup of the English text (the font is "Cienfuegos"),
with two options for spacing. One spaced the letters uniformly across the whole
height, and the other crunched their spacing to each kanji.

For some context, the original JP graphic uses 0x8B tiles, of which 0x1F are
dedicated to the furigana (31 of 139). I found that by crunching the spacing
slightly more than what Lazermutt4 did, I could reuse two tiles and fit the new
text into 31 tiles.
