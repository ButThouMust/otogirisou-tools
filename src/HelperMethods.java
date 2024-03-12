public class HelperMethods {
    private static final int MAX_ROM_BANK = 0x1F;
    public static final int PTR_SIZE = 3;

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
        // bank number must be in range 0x00 - 0x1F
        int bankOffset = ramOffset & 0xFFFF;
        int bankNum = (ramOffset >> 16) & 0xFF;

        return bankOffset >= 0x8000 && bankNum <= MAX_ROM_BANK;
    }

    public static String removeFileExtension(String filename) {
        int periodIndex = filename.lastIndexOf('.');
        return periodIndex == -1 ? filename : filename.substring(0, periodIndex);
    }
}
