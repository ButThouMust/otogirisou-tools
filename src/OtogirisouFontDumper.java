import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class OtogirisouFontDumper {

    private static final int LOOKUP_TABLE_START = 0x074AC;
    private static final int PTR_TO_FONT_BANK_NUM = 0x0256D;
    private static final int STRUCT_SIZE = 5;
    private static final int NUM_CHAR_GROUPS = 58;
    private static final int MAX_DIMEN = 0x10;

    // -------------------------------------------------------------------------

    private static int widthTable[] = new int[NUM_CHAR_GROUPS];
    private static int heightTable[] = new int[NUM_CHAR_GROUPS];
    private static int groupSizeTable[] = new int[NUM_CHAR_GROUPS];
    private static int offsetTable[] = new int[NUM_CHAR_GROUPS];

    private static RandomAccessFile romStream;
    private static FileOutputStream rawFontDump;
    private static FileOutputStream tiledFontDump;

    // -------------------------------------------------------------------------

    private static void readLookupTable() throws IOException {
        romStream = new RandomAccessFile("rom/Otogirisou (Japan).sfc", "r");

        // get the bank number for the font (should be 0x1F)
        // turn it into a "ROM file" base offset, as in not a LoROM CPU offset
        romStream.seek(PTR_TO_FONT_BANK_NUM);
        int fontBank = romStream.readUnsignedByte();
        int fontBankOffset = 0x8000 * (fontBank - 1);

        romStream.seek(LOOKUP_TABLE_START);
        for (int i = 0; i < NUM_CHAR_GROUPS; i++) {
            // format of 5-byte lookup table entry:
            // 00000000 11111111 22222222 33333333 44444444
            // ssssssss WWWWssss [bank 1F  offset] 00HHHH00

            int data[] = new int[STRUCT_SIZE];
            for (int j = 0; j < STRUCT_SIZE; j++) {
                data[j] = romStream.readUnsignedByte();
            }

            groupSizeTable[i] = (data[0] | (data[1] << 8)) & 0xFFF;
            widthTable[i]     = data[1] >> 4;
            heightTable[i]    = data[4] >> 2;
            offsetTable[i]    = fontBankOffset + (data[2] | (data[3] << 8));
        }
    }

    private static void dumpFontGroup(int groupNum) throws IOException {
        int groupSize = groupSizeTable[groupNum];
        int height = heightTable[groupNum];
        int width = widthTable[groupNum];
        int groupOffset = offsetTable[groupNum];

        // get a 16-bit bitmask with the top (width) bits set
        // e.g. W=1 -> 8000, W=7 -> FE00, W=A -> FFC0
        int bitmask = (0xFFFF << (16 - width)) & 0xFFFF;
        int bitPos = 0;

        for (int chNum = 0; chNum < groupSize; chNum++) {
            int pixelRows[] = new int[MAX_DIMEN];
            int topRow = pixelRows.length - height;
            for (int r = 0; r < height; r++) {
                // a row of up to 15 pixels can extend across 3 bytes
                int bytePos = bitPos >> 3;
                romStream.seek(groupOffset + bytePos);
                int rawPixelData = romStream.readUnsignedByte() |
                                   (romStream.readUnsignedByte() << 8) |
                                   (romStream.readUnsignedByte() << 16);

                // determine if need to align pixels left/right, and by how much
                // align so that leftmost pixel is on bit F in a two byte value
                int alignment = (bitPos & 0x7) + width - 0x10;
                if (alignment > 0) {
                    rawPixelData >>= alignment;
                }
                else if (alignment < 0) {
                    rawPixelData <<= Math.abs(alignment);
                }

                pixelRows[topRow + r] = rawPixelData & bitmask;
                bitPos += width;
            }

            outputPixelData(pixelRows);
            outputTileData(pixelRows);
        }
    }

    private static void outputPixelData(int pixelRows[]) throws IOException {
        // the raw pixel data is viewable in YY-CHR with format "1bpp 16x16"
        for (int r = 0; r < pixelRows.length; r++) {
            rawFontDump.write(pixelRows[r] >> 8);
            rawFontDump.write(pixelRows[r] & 0xFF);
        }
    }

    private static void outputTileData(int pixelRows[]) throws IOException {
        // if you prefer splitting it up across four 8x8 tiles, this will do it
        // intent is for this to be compatible with more tile editors

        // instead of storing as 16 two-byte values, store as 32 one-byte values
        // specifically as four groups of 8 bytes: [TL ; TR ; BL ; BR]
        final int TILE_SIZE = MAX_DIMEN >> 1;
        int tileData[] = new int[MAX_DIMEN * 2];
        for (int i = 0; i < pixelRows.length; i++) {
            // first 16 bytes = top two tiles; last 16 bytes = bottom two tiles
            int topBottom = (i & TILE_SIZE) * 2;
            int tileRow = i & 0x7;

            // write a row of 8 pixels for left tile
            tileData[tileRow + topBottom] = pixelRows[i] >> TILE_SIZE;
            // same for the right tile, at the correct array position
            tileData[tileRow + topBottom + TILE_SIZE] = pixelRows[i] & 0xFF;
        }

        for (int i = 0; i < tileData.length; i++) {
            tiledFontDump.write(tileData[i]);
        }
    }

    public static void main(String args[]) throws IOException {
        readLookupTable();
        rawFontDump = new FileOutputStream("font/JP font dump 16x16 raw.bin");
        tiledFontDump = new FileOutputStream("font/JP font dump 8x8 tiles.bin");
        for (int i = 0; i < NUM_CHAR_GROUPS; i++) {
            dumpFontGroup(i);
        }
        rawFontDump.close();
    }
}