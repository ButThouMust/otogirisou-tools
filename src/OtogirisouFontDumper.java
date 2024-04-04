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
    private static FileOutputStream fontDump;

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

        // see my writeup on font decompression for more information about this
        int bitPos = 0;
        int bitmask = (0xFFFF << (16 - width)) & 0xFFFF;

        for (int chNum = 0; chNum < groupSize; chNum++) {
            int pixelRows[] = new int[MAX_DIMEN];
            int topRow = pixelRows.length - height;
            for (int r = 0; r < height; r++) {
                int bytePos = bitPos >> 3;
                romStream.seek(groupOffset + bytePos);
                int rawPixelData = romStream.readUnsignedByte() |
                                   (romStream.readUnsignedByte() << 8) |
                                   (romStream.readUnsignedByte() << 16);

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

            for (int r = 0; r < pixelRows.length; r++) {
                fontDump.write(pixelRows[r] >> 8);
                fontDump.write(pixelRows[r] & 0xFF);
            }
        }
    }

    public static void main(String args[]) throws IOException {
        readLookupTable();
        fontDump = new FileOutputStream("font/JP font dump.bin");
        for (int i = 0; i < NUM_CHAR_GROUPS; i++) {
            dumpFontGroup(i);
        }
        fontDump.close();
    }
}