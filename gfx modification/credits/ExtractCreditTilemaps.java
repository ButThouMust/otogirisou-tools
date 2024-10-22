
// purpose: given a tilemap of the translated credits generated from Tilemap
// Studio, plus a text file with dimensions for each credit's tilemap, extract
// out the individual tilemaps for each credit into their own files

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ExtractCreditTilemaps {
    private static final int NUM_TILEMAPS = 14;
    private static final int TILEMAP_ID_SIZE = 2;

    private static int[] tilemapWidths;
    private static int[] tilemapHeights;
    private static String[] tilemapNames;

    private static int maxWidth = -1;

    private static String getTilemapOutputName(int tilemapNum) {
        String format = "%02d %s SNES TILEMAP 0x%02X x 0x%02X.bin";
        return String.format(format, tilemapNum + 1, tilemapNames[tilemapNum], tilemapWidths[tilemapNum], tilemapHeights[tilemapNum]);
    }

    private static void getTilemapSizes(String sizesCSV) throws IOException {
        BufferedReader sizesReader = new BufferedReader(new FileReader(sizesCSV));
        tilemapHeights = new int[NUM_TILEMAPS];
        tilemapWidths  = new int[NUM_TILEMAPS];
        tilemapNames   = new String[NUM_TILEMAPS];

        int lineNum = 0;
        String line = "";
        while ((line = sizesReader.readLine()) != null && lineNum < NUM_TILEMAPS) {
            // lines are expected to be formatted like "WW,HH,description"
            // where WW and HH are the width and height of each tilemap in tiles
            String[] split = line.split(",");
            tilemapWidths[lineNum]  = Integer.parseInt(split[0], 16);
            tilemapHeights[lineNum] = Integer.parseInt(split[1], 16);
            tilemapNames[lineNum]   = split[2];

            // determine the largest width of any one tilemap; it is assumed to
            // be the width of the source image i.e. how to know how many tile
            // identifiers are used for padding to that consistent width
            if (tilemapWidths[lineNum] > maxWidth) {
                maxWidth = tilemapWidths[lineNum];
            }
            lineNum++;
        }
        sizesReader.close();
    }

    private static void readTilemapData(String combinedTilemapFilename) throws IOException {
        RandomAccessFile combinedTilemap = new RandomAccessFile(combinedTilemapFilename, "r");
        int numBytesInRow = maxWidth * TILEMAP_ID_SIZE;

        for (int tilemapNum = 0; tilemapNum < NUM_TILEMAPS; tilemapNum++) {
            // for each tilemap, read [height] rows that are each [width] columns wide
            int height = tilemapHeights[tilemapNum];
            int width = tilemapWidths[tilemapNum];
            String tilemapOutputName = getTilemapOutputName(tilemapNum);
            FileOutputStream newTilemap = new FileOutputStream(tilemapOutputName, false);

            for (int row = 0; row < height; row++) {
                long startOfRowInFile = combinedTilemap.getFilePointer();
                for (int col = 0; col < width; col++) {
                    newTilemap.write(combinedTilemap.readUnsignedByte());
                    newTilemap.write(combinedTilemap.readUnsignedByte());
                }
                // go to next row; skip empty tiles past this tilemap's right edge
                combinedTilemap.seek(startOfRowInFile + numBytesInRow);
            }

            // in my source image, I put a row of empty tiles between each tilemap
            // this skips the appropriate number of bytes to the start of the next tilemap
            combinedTilemap.seek(combinedTilemap.getFilePointer() + numBytesInRow);
            newTilemap.close();

            ConvertSNESTilemapToRLETilemap.convertToDumpTilemap(tilemapOutputName, width, height);
        }

        combinedTilemap.close();
    }

    public static void main(String args[]) throws IOException {
        // sample usage: java ExtractCreditTilemaps combined_tilemap.bin tilemap_sizes.csv
        if (args.length != 2){ 
            System.out.println("Sample usage: java ExtractTilemaps combined_tilemap.bin tilemap_sizes.csv");
            return;
        }

        String combinedTilemapFile = args[0];
        String tilemapSizesFile = args[1];

        getTilemapSizes(tilemapSizesFile);
        readTilemapData(combinedTilemapFile);
    }
}
