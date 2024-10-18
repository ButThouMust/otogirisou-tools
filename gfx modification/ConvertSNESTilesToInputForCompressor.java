import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConvertSNESTilesToInputForCompressor {
    public static void main(String args[]) throws IOException {
        if (args.length != 5) {
            System.out.println("Sample use: java ConvertSNESTilesToInputForCompressor tilesInput.bin rleOutput.bin numTilesHex srcBitDepth targetBitDepth");
            return;
        }

        FileInputStream snesSrcTiles = new FileInputStream(args[0]);
        FileOutputStream outputTilesForCompressor = new FileOutputStream(args[1]);

        // bit depth of a tile e.g. 8bpp or 4bpp
        int srcBitDepth = Integer.parseInt(args[3]);
        // the other (srcBitDepth - targetBitDepth) groups should be all 00
        int targetBitDepth = Integer.parseInt(args[4]);;

        // skip the tile of full transparency if there is one
        // TODO you might want to put in a CLI option for this
        boolean hasTransparentTile = true;
        if (hasTransparentTile) {
            // N bits/pixel * 64 pixels/tile / (8 bits/byte) = N*8 bytes/tile
            int numBytesToSkip = srcBitDepth * 8;
            snesSrcTiles.skip(numBytesToSkip);
        }

        // size in bytes of a group to operate on at a time
        final int GROUP_SIZE = 8;
        // combine two groups into a buffer that gets written to a file
        final int BUFFER_SIZE = 16;

        int numTiles = Integer.parseInt(args[2], 16) + (hasTransparentTile ? -1 : 0);

        for (int tile = 0; tile < numTiles; tile++) {
            int[] buffer = new int[BUFFER_SIZE];
            int byteNumber = 0;
            for (int group = 0; group < srcBitDepth; group++) {
                // for non-zero groups, copy them back in un-interleaved
                if (group < targetBitDepth) {
                    for (int i = 0; i < GROUP_SIZE; i++) {
                        int dataByte = snesSrcTiles.read();

                        // this accomplishes UNinterleaving bytes into RLE output:
                        // RLE:   00 02 04 06 08 0A 0C 0E   01 03 05 07 09 0B 0D 0F
                        // tiles: 00 01 02 03 04 05 06 07   08 09 0A 0B 0C 0D 0E 0F
                        // byteNumber corresponds to tiles, bufferIndex to RLE
                        int bufferIndex = (byteNumber / 2) + 8 * (byteNumber & 0x1);
                        buffer[bufferIndex] = dataByte;

                        byteNumber = (byteNumber + 1) & 0xF;
                        if (byteNumber == 0) {
                            for (int j = 0; j < BUFFER_SIZE; j++) {
                                outputTilesForCompressor.write(buffer[j]);
                            }
                            buffer = new int[BUFFER_SIZE];
                        }
                    }
                }
                // for groups of all zeroes, skip them
                else {
                    // if # of non-zero groups is odd (8 bytes leftover after
                    // the last group of 16), have to write those eight bytes
                    if (group == targetBitDepth && (group & 0x1) == 1) {
                        for (int i = 0; i < GROUP_SIZE; i++) {
                            outputTilesForCompressor.write(buffer[i*2]);
                        }
                    }
                    for (int i = 0; i < GROUP_SIZE; i++) {
                        snesSrcTiles.read();
                    }
                }
            }
        }
        snesSrcTiles.close();
        outputTilesForCompressor.flush();
        outputTilesForCompressor.close();
    }
}
