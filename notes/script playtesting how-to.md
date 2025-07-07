# How to playtest the Huffman script
In script dumps, the top of every screen of text will contain a commented
pointer for where that text exists in the translated ROM. Importantly, it will
also contain a three-byte sequence like `[08 20 AF]`. This is data that you
should insert three times at ROM offset 0x12627 (CPU offset $02A627) if you want
to start directly at that screen of text when creating a new file or restarting
an existing file from the "beginning". You can use this to spot-check specific
screens of text without needing to plot out a list of choices for how to get
there.

![start points hex editor screenshot](/images/script%20start%20points'%20bytes.png)

If you use the Mesen emulator, its Memory Viewer allows you to temporarily edit
the game's start points, without needing to keep a backup copy of the ROM with
the proper start points.

# A useful pre-requisite
The game keeps track of what endings the player has viewed. The main purpose is
to determine how many choice options to show to the player for each branching
path. In particular, if the player views a total of 9 specific endings, the
bookmark for their file will turn pink instead of yellow, and set all the choice
options in the game as viewable.

It is possible to artificially give yourself the pink bookmark by editing the
game's RAM in a specific way. See the control codes writeup, particularly the
section about `SET FLAG 22`.

# How to spot check a screen of text
Open both your, so to speak, "original" translated script and redumped
translated script. For a particular page of text, pick a decently unique or
uncommon word in the original script, and search for it in the redumped script.
Hopefully, that should land you on the corresponding page in the redumped
script. If not, repeat the search until it does.

![find text in redumped script file](/images/finding%20page%20in%20original%20and%20dumped%20scripts.png)

In the above example, some good words to search for would be `fishing`, `river`,
`passenger's`, `beautiful`, or `wheel`. Words like `her`, `Nami`, `was`, `the`,
etc. are too common.

Take the three byte sequence for this page (in this case, `[43 E4 94]`), and put
it into the correct place to change the start points. Open the translated ROM in
an SNES emulator, and start a new game or pick "Restart" for an existing file.

I recommend creating a save state as the cursor is on the "Restart" option, so
you don't have to sit through the Chunsoft splash screen and mash buttons to get
to the file select screen.

# Keeping track of what text is good/bad
To not go totally crazy checking the script, I needed a systematic way to track
if pages were good, had rewrites/reformats that needed to be checked in-game, or
needed rewrites/retranslations. What I came up with, was to take a clean JP
script dump, and only keep the lines like `//[$15E401-0] -> [08 20 AF]`. A
[blank checklist](/script/blank%20script%20checklist.txt) (created with regex)
is included for your convenience.

The game's choices reach all around the script, so it's sadly not very feasible
to test a script translation until it's fully filled in. So make sure that the
raw dump is in a format you like before you start editing it.

# Linebreaking abnormalities
Once your script is ready for playtesting, I recommend just going through the
story one "route" at a time. See if text is formatted nicely in game, and mark
the page in your checklist as good or as needing reformatting.

Note that the modified ASM code for when to automatically linebreak is not
perfect. Common edge cases I found include:
1. Text overflows the right edge of the screen
   - Manually put in a `<LINE 00>` code before the long word.
2. Text overflows the bottom of the screen (too many lines)
   - Manually put in a `<CLEAR 25>` code at some point on the page that doesn't
     interrupt the flow of the story. Use your judgment.
3. A word can fit into the right margin, but got an auto linebreak before it
   - I made another space encoding specifically to fix these. Change the space
     (represented by an underscore here for clarity) before the short word(s)
     from `_` to `\_`.

Is this system perfect? No, but my hope was that it cuts out a lot of manual
formatting had I reused the original Japanese linebreaking logic.

Also note that for choice codes, you will need to check their linebreaking both
as options in the list, and when you actually pick them.

Below are examples of the "right edge overflow" and "word can fit" (`before I
went / on`) issues described above:

![right edge text overflow](/images/right%20screen%20overflow%20example.png)

![word could have fit](/images/unnecessary%20line%20break%20for%20short%20word.png)

# Control codes with variable text (name, honorific)
In terms of printing the player's name, I would suggest you write translations
with the `<NAME-SAN 20>` or `<NAME 21>` control code at the end of a line in the
script. Of course, this assumes that the sentence still flows correctly if you
do.

In terms of occurrences for the hard-coded "conditionally print honorific"
control codes like `<SAN>`, `<KUN>`, and `<CHAN>` that I added, you should check
the text both when honorifics are enabled or disabled. See the ASM hack for what
byte to edit to toggle them on or off. Of course, if you plan to not give the
option one way or another, you can test with just the option you do plan to use.
