import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChunsoftPresentsDumper {

    private static int[] vramBuffer;

    private static final int END_DATA = 0x80;
    private static final int DATA_SIZE = 0x3000;
    private static final int TILEMAP_BYTE_OFFSET = 0x028699;
    private static final int GFX_LOW_BYTE_OFFSET = 0x028794;
    private static final int GFX_HI_BYTE_OFFSET = 0x0293E1;

    private static final String outputFolder = "gfx dump - full/! chunsoft presents";

    // change this to true if you want this program to output logs about how the
    // data is being interpreted; the writer object is static so that it won't
    // create a file at all if the debug flag is false
    private static boolean DEBUG;
    private static BufferedWriter logFile;

    private enum FillType {
        ALL  (1,0),
        EVEN (2,0),
        ODD  (2,1);
        private final int skipSize;
        private final int startOffset;
        FillType(int skipSize, int startOffset) {
            this.skipSize = skipSize;
            this.startOffset = startOffset;
        }
    }

    // implementation of subroutine at $0281FB in the Otogirisou ROM
    public static int decompress(int ramOffset, FillType fillType, String description) throws IOException {
        int vramPosition = fillType.startOffset;
        RandomAccessFile romStream = new RandomAccessFile("rom/Otogirisou (Japan).sfc", "r");

        if (DEBUG) {
            String logfileNameFormat = outputFolder + "/chunsoft presents log - $%06X (%s).txt";
            String logName = String.format(logfileNameFormat, ramOffset, description);
            logFile = new BufferedWriter(new FileWriter(logName));
        }

        romStream.seek(HelperMethods.getFileOffset(ramOffset));

        int uncompressedSize = 0;
        int compressedSize = 1;

        long filePtr = romStream.getFilePointer();
        int sizeByte = romStream.readUnsignedByte();
        while (sizeByte != END_DATA) {
            int dataSize = (sizeByte & 0x7F) + 1;
            // MSB clear; copy next [sizeByte + 1] bytes to "VRAM"
            if (sizeByte < END_DATA) {
                if (DEBUG) {
                    String format = "Byte @ 0x%05X = 0x%02X\nTake next 0x%02X bytes:";
                    logFile.write(String.format(format, filePtr, sizeByte, dataSize));
                }

                String hexData = "";
                for (int i = 0; i < dataSize; i++) {
                    int dataByte = romStream.readUnsignedByte();
                    vramBuffer[vramPosition] = dataByte;
                    vramPosition += fillType.skipSize;

                    // log file: print spaces between bytes, but new line after every 16 bytes
                    if (DEBUG) {
                        String formatting = ((i & 0xF) == 0) ? "\n" : " ";
                        hexData += formatting + String.format("%02X", dataByte);
                    }
                }

                uncompressedSize += dataSize;
                compressedSize += dataSize;

                if (DEBUG) {
                    hexData += "\n\n";
                    logFile.write(hexData);
                }
            }
            // MSB set; repeat next byte ([sizeByte & 0x7F] + 1) times
            else {
                int dataByte = romStream.readUnsignedByte();
                for (int i = 0; i < dataSize; i++) {
                    vramBuffer[vramPosition] = dataByte;
                    vramPosition += fillType.skipSize;
                }
                uncompressedSize += dataSize;
                compressedSize++;

                if (DEBUG) {
                    String format1 = "Byte @ 0x%05X = 0x%02X\nRepeat next byte 0x%02X total of 0x%02X times\n\n";
                    logFile.write(String.format(format1, filePtr, sizeByte, dataByte, dataSize));
                }
            }

            // get size of next section
            filePtr = romStream.getFilePointer();
            sizeByte = romStream.readUnsignedByte();
            compressedSize++;
        }

        romStream.close();
        if (DEBUG) {
            String format = "Got 0x80 @ 0x%05X, end of data\n";
            String format1 = "Uncompressed size: 0x%X\n";
            String format2 = "Compressed size:   0x%X\n";

            logFile.write(String.format(format, filePtr));
            logFile.write(String.format(format1, uncompressedSize));
            logFile.write(String.format(format2, compressedSize));
            logFile.close();
        }

        return uncompressedSize;
    }

    private static void dumpVRAMBufferToFile(int dataSize, String description) throws IOException {
        String outputFileFormat = outputFolder + "/chunsoft presents data (%s).bin";
        FileOutputStream rawData = new FileOutputStream(String.format(outputFileFormat, description));
        for (int i = 0; i < dataSize; i++) {
            rawData.write(vramBuffer[i]);
        }
        rawData.flush();
        rawData.close();
    }

    // The graphics are 8bpp which makes it not very compatible with most SNES
    // graphics viewers. I found that binxelview can work, and as such I formatted
    // the .pal file to be compatible with it. However, consider this a nice
    // bonus to the tile and tilemap data being dumped.
    private static void dumpChunsoftPalette() throws IOException {
        final int CHUNSOFT_PALETTE_OFFSET = 0x10527;
        final int NUM_PALETTE_VALS = 20;

        String outputFileFormat = outputFolder + "/chunsoft palette $028527.pal";
        FileOutputStream paletteOutput = new FileOutputStream(outputFileFormat);

        String logFileFormat = outputFolder + "/LOG chunsoft palette.txt";
        BufferedWriter logFile = new BufferedWriter(new FileWriter(logFileFormat));

        String line = "  %04X  | %06X\n";
        logFile.write(" 15-bit | 24-bit\n");
        logFile.write("--------+--------\n");

        RandomAccessFile romStream = new RandomAccessFile("rom/Otogirisou (Japan).sfc", "r");
        romStream.seek(CHUNSOFT_PALETTE_OFFSET);
        for (int i = 0; i < NUM_PALETTE_VALS; i++) {
            // read a BGR15 color value and convert it to an RGB24 color value
            int bgr15 = romStream.readUnsignedByte() | 
                        (romStream.readUnsignedByte() << 8);
            int rgb24 = HelperMethods.convertBGR15ToRGB24(bgr15);

            // write the RGB24 value to its own file to be used with binxelview
            // byte order is [red8 green8 blue8] for each color i.e. big endian
            paletteOutput.write((rgb24 >> 16) & 0xFF);
            paletteOutput.write((rgb24 >> 8) & 0xFF);
            paletteOutput.write(rgb24 & 0xFF);

            logFile.write(String.format(line, bgr15, rgb24));
        }

        paletteOutput.flush();
        paletteOutput.close();
        logFile.flush();
        logFile.close();
        romStream.close();
    }

    public static void main(String args[]) throws IOException {
        // lazy implementation, presence of any CLI arguments -> turn on debug output
        DEBUG = args.length != 0;
        Files.createDirectories(Paths.get(outputFolder));

        String descriptions[] = {"tilemap", "low bytes only 4bpp", "high bytes only 4bpp"};
        int offsets[] = {TILEMAP_BYTE_OFFSET, GFX_LOW_BYTE_OFFSET, GFX_HI_BYTE_OFFSET};

        int actualSize = 0;

        // dump the tilemap, the gfx low bytes, and the gfx hi bytes
        for (int i = 0; i < descriptions.length; i++) {
            vramBuffer = new int[DATA_SIZE];
            String formattedDescription = String.format("$%06X - %s", offsets[i], descriptions[i]);
            actualSize = decompress(offsets[i], FillType.ALL, descriptions[i]);
            dumpVRAMBufferToFile(actualSize, formattedDescription);
        }

        // now dump the gfx low/high bytes together
        vramBuffer = new int[DATA_SIZE];
        actualSize = decompress(GFX_LOW_BYTE_OFFSET, FillType.EVEN, "combine low bytes");
        actualSize += decompress(GFX_HI_BYTE_OFFSET, FillType.ODD, "combine high bytes");
        dumpVRAMBufferToFile(actualSize, "low + high bytes 8bpp");

        dumpChunsoftPalette();
    }
    
}
