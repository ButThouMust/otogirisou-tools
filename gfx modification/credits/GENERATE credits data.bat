prompt $g

@javac .\*.java

:: change these filenames as appropriate for your files
@set tilesetImageInput="credits tileset - reduce indent support pg 2.png"
@set tilesetBinaryOutput="credits tileset.bin"
@set tilemapFile="credits tilemap - reduce indent support pg 2.bin"
@set tilemapSizesCSV="english credits tilemap sizes.csv"

:: Generate binary format version of tileset, using tileset from Tilemap Studio
:: specify 1bpp, and that the background color is white (change as appropriate)
@set backgroundColor="ffffffff"
..\superfamiconv.exe -v -i %tilesetImageInput% -t %tilesetBinaryOutput% -B 1 --color-zero %backgroundColor%

:: Old: A program I had originally made for working on the font, also works for
:: the credits because they are 1bpp, too. However, I'd rather let superfamiconv
:: do the work for me now, instead of my own not-thoroughly-tested tool.
:: @set textColor="000000"
:: @set tileWidth=8
:: @set tileHeight=8
:: java FontImageDriver %tilesetImageInput% %textColor% %tileWidth% %tileHeight%

:: Extract tilemaps for individual credits from tilemap for the combined credits
:: New addition: add functionality to also convert to RLE format in the process
java ExtractCreditTilemaps %tilemapFile% %tilemapSizesCSV%
