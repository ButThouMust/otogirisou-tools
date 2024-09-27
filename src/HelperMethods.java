public class HelperMethods {

    public static final int MAX_ROM_BANK = 0x1F;
    public static final int BYTE = 8;
    public static final int NUM_BYTES_IN_PTR = 3;
    public static final int PTR_SIZE_BITS = BYTE * NUM_BYTES_IN_PTR;

    // constants for control code argument table
    public static final int NUM_CTRL_CODES = 0x47;
    public static final int CTRL_CODE_ARG_TBL = 0x0741E;
    public static final int CHAR_ARG = 0;
    public static final int PTR_ARG = 1;

    // control codes for special cases when interpretting or when printing scripts
    public static final int LINE_00 = 0x1000;
    public static final int MIN_CTRL_CODE = LINE_00;
    public static final int JMP_03 =  0x1003;
    public static final int JMP_CC_04 = 0x1004;
    public static final int CHOICE_19 = 0x1019;
    public static final int CHOICE_1A = 0x101A;
    public static final int END_CHOICE_1C = 0x101C;
    public static final int NOP_1D = 0x101D;
    public static final int NAME_SAN_20 = 0x1020;
    public static final int NAME_21 = 0x1021;
    public static final int SET_FLAG_22 = 0x1022;
    public static final int CLEAR_25  = 0x1025;
    public static final int CLEAR_27  = 0x1027;

    // 
    public static final int NUM_ENCODINGS = 1702;

    // constants related to Huffman encoding for script
    public static final String LEFT_BIT = "0";
    public static final String RIGHT_BIT = "1";
    public static final int LEFT_BIT_INT = 0;
    public static final int RIGHT_BIT_INT = 1;
    public static final int NO_HEX_VAL = -1;
    public static final int NUM_HUFFMAN_ENTRIES = 0x5B7;
    public static final int ROOT_ENTRY_POS = NUM_HUFFMAN_ENTRIES - 1;
    public static final int HUFF_LEFT_OFFSET = 0x14D4F;
    public static final int HUFF_RIGHT_OFFSET = 0x141E1;
    public static final int HUFF_TABLE_ENTRIES = 0x5B7;

    // constants related to script start points and script pointers
    public static final int START_POINT_LIST = 0x12627;
    public static final int NUM_START_POINTS = 3;
    public static final int NUM_POINTERS = 2500;

    // memory address for start of save file progress flags
    public static final int FLAG_BASE_ADDRESS = 0x1BEF;

    // convert a LoROM offset into a "hex editor" file offset
    public static int getFileOffset(int ramOffset) {
        int bankNum = (ramOffset >> 16) & 0xFF;
        int bankOffset = ramOffset & 0xFFFF;
        return 0x8000 * (bankNum - 1) + bankOffset;
    }

    public static int getRAMOffset(int fileOffset) {
        int bankOffset = (fileOffset & 0xFFFF) | 0x8000;
        int bankNum = 1 + (fileOffset - bankOffset) / 0x8000;
        return (bankNum << 16) | bankOffset;
    }

    public static boolean isValidRomOffset(int ramOffset) {
        // Otogirisou is a 1MB LoROM game
        // bank offset must be in range 0x8000 - 0xFFFF
        // bank number "should" be in range 0x00 - 0x1F, but won't enforce this
        // as a hard rule because I expand the ROM to 1.5MB
        int bankOffset = ramOffset & 0xFFFF;
        // int bankNum = (ramOffset >> 16) & 0xFF;

        return bankOffset >= 0x8000;
        // return bankOffset >= 0x8000; && bankNum <= MAX_ROM_BANK;
    }

    public static String removeFileExtension(String filename) {
        int periodIndex = filename.lastIndexOf('.');
        return periodIndex == -1 ? filename : filename.substring(0, periodIndex);
    }

    public static int convertBGR15ToRGB24(int colorValue15) {
        // convert 15-bit BGR to 24-bit RGB
        // 0 BBBBB GGGGG RRRRR -> RRRRRRRR GGGGGGGG BBBBBBBB
        // source: https://wiki.superfamicom.org/palettes
        int red5 = colorValue15 & 0x1F;
        int green5 = (colorValue15 >> 5) & 0x1F;
        int blue5 = (colorValue15 >> 10) & 0x1F;

        int red8 = red5 << 3;
        int green8 = green5 << 3;
        int blue8 = blue5 << 3;

        red8 += red8 >> 5;
        green8 += green8 >> 5;
        blue8 += blue8 >> 5;
        int colorValue24 = (red8 << 16) | (green8 << 8) | blue8;
        return colorValue24;
    }
}
