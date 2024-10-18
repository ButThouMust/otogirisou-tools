import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ConvertSNESTilemapToInputForCompressor {

    private static final int TILEMAP_ENTRY_SIZE = 2;

    private static String getOutputFilename(String inputFilename) throws IOException {
        int periodIndex = inputFilename.lastIndexOf('.');
        if (periodIndex == -1) {
            String errorMessage = "Tilemap file should have a proper file extension.";
            throw new IOException(errorMessage);
        }
        return inputFilename.substring(0, periodIndex) + " - for RLE compressor.bin";
    }

    private static void convertToDumpTilemap(String filename, int width, int height) throws IOException {
        RandomAccessFile inputFile = new RandomAccessFile(filename, "r");
        int tilemapSize = width * height;
        
        if (tilemapSize * TILEMAP_ENTRY_SIZE != (int) inputFile.length()) {
            inputFile.close();
            throw new IOException("Warning: inputted dimensions do not match with filesize.");
        }

        int tilemapLow[] = new int[tilemapSize];
        int tilemapHigh[] = new int[tilemapSize];

        String outputFilename = getOutputFilename(filename);
        FileOutputStream outputFile = new FileOutputStream(outputFilename);

        // collect the low and high bytes for tilemap identifiers into separate arrays
        for (int i = 0; i < tilemapSize; i++) {
            tilemapLow[i]  = inputFile.readUnsignedByte();
            tilemapHigh[i] = inputFile.readUnsignedByte();
        }

        // first output all the low bytes of the tilemaps as one big block
        for (int i = 0; i < tilemapSize; i++) {
            outputFile.write(tilemapLow[i]);
        }

        // then output all the high bytes of the tilemaps as one big block
        for (int i = 0; i < tilemapSize; i++) {
            outputFile.write(tilemapHigh[i]);
        }

        inputFile.close();
        outputFile.flush();
        outputFile.close();
    }

    public static void main(String args[]) throws IOException {
        if (args.length != 3) {
            System.out.println("Sample usage: java ConvertSNESTilemapToInputForCompressor dumped_tilemap.bin tilemap_width tilemap_height");
            return;
        }

        String regularTilemapFile = args[0];
        int tilemapWidth = Integer.parseInt(args[1], 16);
        int tilemapHeight = Integer.parseInt(args[2], 16);

        convertToDumpTilemap(regularTilemapFile, tilemapWidth, tilemapHeight);
    }
    
}
