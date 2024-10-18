
package dump_graphics;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

import static header_files.HelperMethods.*;

public class GraphicsStructure implements Comparable<GraphicsStructure> {

    private static final int INDEP_STRUCT = -1;

    private RandomAccessFile romStream;

    private StructureType type;
    private int structureLocation;
    private int ptrToData;
    private int endOfData;
    private int uncompDataSize;
    private int[] structureMetadata;
    private int[] pointerMetadata;

    private String filenameFormat;
    private String dataOutputFile;
    private String logFilename;

    private BufferedWriter logFile;

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public enum StructureType {
        TILE    (4, 3),
        TILEMAP (6, 7),
        PALETTE (4, 4);
        private final int metadataSize;
        private final int bytesToSkip;

        StructureType(int metadataSize, int bytesToSkip) {
            this.metadataSize = metadataSize;
            this.bytesToSkip = bytesToSkip;
        }

        public int getMetadataSize() {
            return metadataSize;
        }

        public int getBytesToSkip() {
            return bytesToSkip;
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public StructureType getType() {
        return type;
    }

    public int getStructureLocation() {
        return structureLocation;
    }

    public int getDataPointer() {
        return ptrToData;
    }

    public int getDataEndPointer() {
        return endOfData;
    }

    public int getNumBytesToSkip() {
        return type.getBytesToSkip();
    }

    public boolean isIndepStruct() {
        return structureLocation == INDEP_STRUCT;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public GraphicsStructure(int structureLocation) throws IOException {
        this.structureLocation = structureLocation;
        ptrToData = determinePointerToData();
        type = determineType();
        readStructureMetadata();
        readPointerMetadata();
    }

    // handle gfx data that no structure explicitly points to
    // note: shoehorning in the StructureType parameter was what I came up with
    //       to have a different signature for this constructor
    // consider coming up with a better way to handle this
    public GraphicsStructure(int ptrToData, StructureType type) throws IOException {
        if (!isValidRomOffset(ptrToData)) {
            String format = "Invalid data location for independent structure - $%06X does not map to ROM";
            throw new IOException(String.format(format, ptrToData));
        }

        romStream = new RandomAccessFile("rom/Otogirisou (Japan).sfc", "r");
        structureLocation = INDEP_STRUCT;
        this.ptrToData = ptrToData;
        this.type = type;
        structureMetadata = new int[0];
        readPointerMetadata();
    }

    private int determinePointerToData() throws IOException {
        if (!isValidRomOffset(structureLocation)) {
            String format = "Invalid RAM offset for structure - $%06X does not map to ROM";
            throw new IOException(String.format(format, structureLocation));
        }

        romStream = new RandomAccessFile("rom/Otogirisou (Japan).sfc", "r");
        // the first three bytes at the location of the structure are a 24-bit
        // pointer to some necessary metadata, and then the data itself
        int romOffset = getFileOffset(structureLocation);
        romStream.seek(romOffset);
        int dataPtr = (romStream.readUnsignedByte()) |
                      (romStream.readUnsignedByte() << 8) |
                      (romStream.readUnsignedByte() << 16);
        if (!isValidRomOffset(dataPtr)) {
            String format = "Structure has invalid RAM offset to data - $%06X @ 0x%05X does not map to ROM";
            throw new IOException(String.format(format, dataPtr, romOffset));
        }

        return dataPtr;
    }

    private StructureType determineType() throws IOException {
        romStream.seek(getFileOffset(ptrToData));

        romStream.readUnsignedByte();
        romStream.readUnsignedByte();
        int byte2 = romStream.readUnsignedByte();

        StructureType type = null;
        switch (byte2 & 0x7) {
            case 0x0:
            case 0x1:
                // tile data, RLE compressed
                type = StructureType.TILE;
                break;

            case 0x2:
            case 0x3:
                // tilemap data, RLE compressed
                type = StructureType.TILEMAP;
                break;

            case 0x4:
                // palette data
                type = StructureType.PALETTE;
                break;

            default:
                // undefined, throw some kind of error here
                String format = "Malformed structure - no valid type @ pointer $%06X";
                throw new IOException(String.format(format, ptrToData + (NUM_BYTES_IN_PTR - 1)));
        }
        return type;
    }

    private void readStructureMetadata() throws IOException {
        romStream.seek(getFileOffset(structureLocation) + NUM_BYTES_IN_PTR);

        structureMetadata = new int[type.bytesToSkip];
        for (int i = 0; i < structureMetadata.length; i++) {
            structureMetadata[i] = romStream.readUnsignedByte();
        }
    }

    private void readPointerMetadata() throws IOException {
        romStream.seek(getFileOffset(ptrToData));

        pointerMetadata = new int[type.metadataSize];
        for (int i = 0; i < pointerMetadata.length; i++) {
            pointerMetadata[i] = romStream.readUnsignedByte();
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public void setOutputFolder(String outputFolder) {
        filenameFormat = outputFolder + "/$%06X %s%s";
    }

    public void setOutputFolderIndepStruct(String outputFolder) throws IOException {
        String structFolderFormat = "%s/$%06X -- independent %s";
        String structFolder = String.format(structFolderFormat, outputFolder, ptrToData, type.toString());
        Files.createDirectories(Paths.get(structFolder));
        setOutputFolder(structFolder);
    }

    public void dumpData() throws IOException {
        romStream.seek(getFileOffset(ptrToData) + type.metadataSize);
        int dataStart = (int) romStream.getFilePointer();
        int dataStartRAM = getRAMOffset(dataStart);

        dataOutputFile = String.format(filenameFormat, dataStartRAM, type.toString(), ".bin");
        logFilename = String.format(filenameFormat, dataStartRAM, type.toString(), " log.txt");

        switch (type) {
            case TILE:
                dumpRLEData();
                if (!isIndepStruct()) {
                    // doing this interleave & pad operation requires info from
                    // structure's metadata; not possible for independent gfx data
                    generateSNESTileData(dataStartRAM);
                }
                break;
            case TILEMAP:
                dumpRLEData();
                generateSNESFormatTilemap(dataStartRAM);
                break;
            case PALETTE:
                logFile = new BufferedWriter(new FileWriter(logFilename));
                dumpPaletteData();
                break;
        }

        if (isIndepStruct()) {
            // for independent tile and tilemap data, just need to log metadata
            if (type != StructureType.PALETTE) {
                logFile = new BufferedWriter(new FileWriter(logFilename));
            }
            // for independent palette data, first log the table of color values
            // and then log the metadata for it
            else {
                logFile.write("\n\n");
            }
            logFile.write(this.toString());
            logFile.flush();
            logFile.close();
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private void dumpRLEData() throws IOException {
        // read a byte; if it is 0x80, we are done
        int ctrlByte = romStream.readUnsignedByte();
        // keep track of size of decompressed data
        int decompressedSize = 0;

        FileOutputStream outputFile = new FileOutputStream(dataOutputFile);

        while (ctrlByte != 0x80) {
            // "run length encoding" case: 0 <= ctrlByte < 0x80
            if (ctrlByte < 0x80) {
                int dataByte = romStream.readUnsignedByte();
                int repeatCount = ctrlByte + 1;
                // write dataByte a total of (ctrlByte + 1) times
                for (int i = 0; i < repeatCount; i++) {
                    outputFile.write(dataByte);
                }

                decompressedSize += repeatCount;
            }

            // "uncompressed data" case: 0x80 < ctrlByte <= 0xFF
            else {
                int repeatCount = ctrlByte & 0x7F;
                for (int i = 0; i < repeatCount; i++) {
                    // copy a total of (ctrlByte & 0x7F) bytes to file
                    int data = romStream.readUnsignedByte();
                    outputFile.write(data);
                }

                decompressedSize += repeatCount;
            }

            // get next control byte
            ctrlByte = romStream.readUnsignedByte();
        }

        // got 0x80, so done decompressing
        endOfData = getRAMOffset((int) romStream.getFilePointer() - 1);
        uncompDataSize = decompressedSize;
        outputFile.close();
    }

    private int getTileBitDepthMinus1() {
        int structByte5 = structureMetadata[2];
        int n = (structByte5 & 0x30) >> 4;
        int returnValue = 0;
        switch (n) {
            case 0x0:
                returnValue = 0x10;
                break;
            case 0x1:
                returnValue = 0x30;
                break;
            case 0x2:
                returnValue = 0x70;
                break;
            // default:
                // String error = "Error: Bits 5 and 4 of $%06X are 0b11 -- invalid value for tile bit depth";
                // throw new IOException(String.format(error, structureLocation + 5));
        }
        return returnValue;
    }

    private void generateSNESTileData(int dataStartRAM) throws IOException {
        // implement the subroutine at $00B215 -- interleave bytes together and
        // (depending on the raw decompressed data) pad it to a certain
        // graphics bit depth that is pre-defined in the structure data

        FileInputStream decompFile = new FileInputStream(dataOutputFile);

        final int GROUP_SIZE = 8;
        final int BUFFER_SIZE = GROUP_SIZE * 2;

        int numNonzeroGroups = ((pointerMetadata[3] >> 4) & 0x7) + 1;
        int structByte5 = structureMetadata[2];
        int addr26 = (structByte5 & 0x8F) | (getTileBitDepthMinus1());
        int tileBitDepth = ((addr26 >> 4) & 0x7) + 1;
        boolean createEmptyTile = (addr26 & 0x1) == 0;

        String extension = String.format(".%dbpp", tileBitDepth);
        String snesTileFilename = String.format(filenameFormat, dataStartRAM, "SNES format tiles", extension);
        FileOutputStream snesTiles = new FileOutputStream(snesTileFilename);

        if (createEmptyTile) {
            int numZeroDoubleBytes = tileBitDepth << 2;
            for (int i = 0; i < numZeroDoubleBytes; i++) {
                snesTiles.write(0);
                snesTiles.write(0);
            }
        }

        int numTiles = pointerMetadata[0] | (pointerMetadata[1] << 8);
        for (int tile = 0; tile < numTiles; tile++) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int byteNumber = 0;
            for (int group = 0; group < tileBitDepth; group++) {
                for (int i = 0; i < GROUP_SIZE; i++) {
                    int dataByte = 0x00;
                    if (group < numNonzeroGroups) {
                        dataByte = decompFile.read();
                    }

                    // this accomplishes interleaving bytes from the RLE output:
                    // RLE:   00 01 02 03 04 05 06 07   08 09 0A 0B 0C 0D 0E 0F
                    // tiles: 00 02 04 06 08 0A 0C 0E   01 03 05 07 09 0B 0D 0F
                    // byteNumber corresponds to RLE, bufferIndex to tiles
                    int bufferIndex = (byteNumber & 0x7) * 0x2 + (byteNumber >> 3);
                    buffer[bufferIndex] = (byte) dataByte;

                    byteNumber = (byteNumber + 1) & 0xF;
                    if (byteNumber == 0) {
                        snesTiles.write(buffer);
                        buffer = new byte[BUFFER_SIZE];
                    }
                }
            }
        }

        decompFile.close();
        snesTiles.close();
    }

    private void generateSNESFormatTilemap(int dataStartRAM) throws IOException {
        // one way Otogirisou takes advantage of the RLE compression is that its
        // tilemaps either don't store the high bytes at all (filled with [00])
        // or store them separately from the low bytes
        // GOAL: convert raw decompressed tilemap to standard SNES format

        // get an array of all the bytes from the raw decompressed tilemap
        FileInputStream decompFile = new FileInputStream(dataOutputFile);
        byte rawDecompTilemap[] = new byte[uncompDataSize];
        decompFile.read(rawDecompTilemap);
        decompFile.close();

        // create a file for the SNES format tilemap
        String snesTilemapFilename = String.format(filenameFormat, dataStartRAM, "SNES format tilemap", ".bin");
        FileOutputStream snesTilemap = new FileOutputStream(snesTilemapFilename);

        // determine if the high bytes are all 00 or are included
        // the check for this is whether metadata byte 3 is [01] or not (can be 00, 01, 80)
        int tilemapWidth = pointerMetadata[4];
        int tilemapHeight = pointerMetadata[5];
        int numTilemapEntries = tilemapWidth * tilemapHeight;
        // boolean highBytesIncluded = numTilemapEntries != uncompDataSize;
        boolean highBytesIncluded = pointerMetadata[3] == 0x01;

        for (int i = 0; i < numTilemapEntries; i++) {
            byte entryLowByte = rawDecompTilemap[i];
            byte entryHighByte = 0x00;
            if (highBytesIncluded) {
                entryHighByte = rawDecompTilemap[i + numTilemapEntries];
            }

            snesTilemap.write(entryLowByte);
            snesTilemap.write(entryHighByte);
        }

        snesTilemap.close();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private void dumpPaletteData() throws IOException {
        // String outputFilename = String.format(outputFolder + "/$%06X palette.bin", getRAMOffset(romOffset));
        // FileOutputStream outputFile = new FileOutputStream(outputFilename);
        FileOutputStream outputFile = new FileOutputStream(dataOutputFile);
        // BufferedWriter logFile = new BufferedWriter(new FileWriter(logFilename));

        // int size = pointerMetadata[0] - 3;
        int position = getRAMOffset((int) romStream.getFilePointer());
        int totalColors = romStream.readUnsignedByte();
        int colorsUsed = romStream.readUnsignedByte();

        // Note: You should determine color depth from the tile data instead.
        // int colorDepth = (int) (Math.log(totalColors) / Math.log(2));
        // String firstLineFormat = "$%06X has data for %d colors of a possible %d (%d bpp)";
        // String firstLine = String.format(firstLineFormat, position, colorsUsed, totalColors, colorDepth);

        String firstLineFormat = "$%06X has data for %d colors of a possible %d\n\n";
        String firstLine = String.format(firstLineFormat, position, colorsUsed, totalColors);
        logFile.write(firstLine);
        logFile.write("15-bit | 24-bit\n");
        logFile.write("-------+-------");

        String loopLineFormat = " %04X  | %06X";
        for (int i = 0; i < colorsUsed; i++) {
            int byte0 = romStream.readUnsignedByte();
            int byte1 = romStream.readUnsignedByte() & 0x7F;

            int colorValue15 = (byte1 << 8) | byte0;
            int colorValue24 = convertBGR15ToRGB24(colorValue15);

            outputFile.write(byte0);
            outputFile.write(byte1);

            logFile.write(String.format("\n" + loopLineFormat, colorValue15, colorValue24));
        }
        endOfData = getRAMOffset((int) (romStream.getFilePointer() - 1));
        outputFile.close();

        // note: need to also output metadata if independent structure, so do
        // not close log file here
        if (!isIndepStruct()) {
            logFile.flush();
            logFile.close();
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (!(other instanceof GraphicsStructure))
            return false;

        GraphicsStructure gfxStruct = (GraphicsStructure) other;
        // two structures are the same if they point to the same data in the ROM
        // regardless of the location of the pointer value itself
        return ptrToData == gfxStruct.ptrToData;
    }

    public int compareTo(GraphicsStructure other) { 
        // first compare by data pointer, then by structure locations
        int comp = ptrToData - other.ptrToData;
        if (comp != 0) {
            return comp;
        }
        return structureLocation - other.structureLocation;
    }

    // this used to print to a BufferedWriter, but I realized later that this is
    // perfect for a toString() implementation
    public String toString() {
        String output = "";
        // print the type of structure, the pointer value, and where the data actually starts
        // addition to a RAM offset may overflow the bank like 00FFFF -> 010000
        // so have to first convert to ROM offset before addition, then back to RAM offset
        int dataPtrROM = getFileOffset(ptrToData);
        int dataStartROM = dataPtrROM + type.metadataSize;
        int dataStartCPUAddr = getRAMOffset(dataStartROM);

        if (!isIndepStruct()) {
            String format1 = "$%06X: points to %-7s data at $%06X ($%06X)\n";
            output += String.format(format1, structureLocation, type.toString(), ptrToData, dataStartCPUAddr);
        }
        else {
            String format1 = "%-7s data at $%06X ($%06X)\n";
            output += String.format(format1, type.toString(), ptrToData, dataStartCPUAddr);
        }

        int dataEndROM = getFileOffset(endOfData);
        int dataSize = dataEndROM - dataStartROM + 1;
        String format2 = "Data range in ROM:   0x%05X to 0x%05X (size 0x%X)\n";
        output += String.format(format2, dataStartROM, dataEndROM, dataSize);

        if (!isIndepStruct()) {
            String format3 = String.format("Structure @ $%06X: %06X", structureLocation, ptrToData);
            for (int i = 0; i < type.bytesToSkip; i++) {
                format3 += String.format(" %02X", structureMetadata[i]);
            }
            output += format3 + "\n";
        }

        String format4 = String.format("Metadata  @ $%06X:", ptrToData);
        for (int i = 0; i < type.metadataSize; i++) {
            format4 += String.format(" %02X", pointerMetadata[i]);
        }
        output += format4;

        // add special text that better describes data for each structure type
        // - add more to this as you find out more stuff about data format
        switch (type) {
            case TILE: {
                // metadata bytes 00 and 01 contain # of tiles
                int numTiles = pointerMetadata[0] | (pointerMetadata[1] << 8);
                // to convert bytes/tile to bits/pixel, divide by 8 because
                // 8 bits/byte and 64 pixels/tile
                int bitDepthEstimate = (uncompDataSize / numTiles) / 8;

                output += "\n";
                String format5 = "# tiles = 0x%X; uncomp data is 0x%X bytes (%dbpp";
                output += (String.format(format5, numTiles, uncompDataSize, bitDepthEstimate));

                if (!isIndepStruct()) {
                    // note: this calculation requires structure metadata
                    int paddedBitDepth = ((getTileBitDepthMinus1() >> 4) & 0x7) + 1;
                    output += String.format(" -> %dbpp", paddedBitDepth);
                }
                output += ")";

                // check if tile data is for "type 01" instead of "type 00"
                if ((pointerMetadata[2] & 0x7) == 0x1) {
                    output += ("\nNOTE: This is for \"type 01\" of tile data.");
                }

                if (!isIndepStruct()) {
                    boolean createEmptyTile = (structureMetadata[2] & 0x1) == 0;
                    if (!createEmptyTile) {
                        // this case applies to tile data for IDs: 5B 5C 81 82 91 92 93 96 98 99 9A 9B 9C
                        output += ("\nNOTE: Game does not generate an empty tile for this.");
                    }
                }

                break;
            }
            case TILEMAP: {
                int tilemapWidth = pointerMetadata[4];
                int tilemapHeight = pointerMetadata[5];
                // boolean highBytesIncluded = tilemapWidth * tilemapHeight != uncompDataSize;
                int tilemapSizeFlag = pointerMetadata[3];
                boolean highBytesIncluded = tilemapSizeFlag == 0x01;

                output += "\n";
                String format5 = "High bytes of entries are %s\n";
                output += (String.format(format5, highBytesIncluded ? "included" : "ALL [00]"));

                String format6 = "Tilemap W*H is 0x%02X*0x%02X%s -> 0x%X bytes\n";
                output += (String.format(format6, tilemapWidth, tilemapHeight, (highBytesIncluded ? "*2" : ""), uncompDataSize));

                if (!isIndepStruct()) {
                    String format7 = "Starts on screen @ (X, Y) = (0x%02X, 0x%02X)";
                    int tilemapX = (structureMetadata[6] & 0xF) | ((structureMetadata[5] << 2) & 0x30);
                    int tilemapY = ((structureMetadata[6] >> 4) & 0xF) | (structureMetadata[5] & 0x30);
                    output += (String.format(format7, tilemapX, tilemapY));
                }

                // check a value to see if for Mode 7 graphics
                if (tilemapSizeFlag == 0x80) {
                    output += ("\nNOTE: This tilemap is for Mode 7 graphics!");
                }

                // check if tilemap data is for "type 03" instead of "type 02"
                if ((pointerMetadata[2] & 0x7) == 0x3) {
                    output += ("\nNOTE: This is for \"type 03\" of tilemap data.");
                }

                break;
            }
            case PALETTE: {
                break;
            }
        }
        return output;
    }
}
