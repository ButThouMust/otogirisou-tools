
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class GenerateKerningData {
    private static final int NUM_ENTRIES_PER_BYTE = 4;

    private static int[][] readKerningList() throws IOException {
        BufferedReader kerningListReader = new BufferedReader(new FileReader("font/sorted kerning pairs.txt"));
        Object list[] = kerningListReader.lines().toArray();
        kerningListReader.close();

        kerningListReader = new BufferedReader(new FileReader("font/sorted kerning pairs.txt"));
        // N characters in the font -> N^2 possible pairs of characters
        int numChars = (int) Math.sqrt(list.length);

        int kerningData[][] = new int[numChars][numChars];
        String line = "";
        while ((line = kerningListReader.readLine()) != null) {
            String split[] = line.split("\t");
            if (split.length < 3) continue;

            int prevChar = Integer.parseInt(split[0], 16);
            int currChar = Integer.parseInt(split[1], 16);
            int dist = Integer.parseInt(split[2]);

            kerningData[prevChar][currChar] = -dist;
        }

        kerningListReader.close();
        return kerningData;
    }

    private static int[][] convertKerningDataToBinaryFormat(int kerningData[][]) {
        // pack every group of 4 entries into a byte each
        int numChars = kerningData[0].length;
        int numBytesPerCharTable = (int) Math.ceil(((double) numChars) / 4.0);
        // System.out.println("# bytes / char = " + numBytesPerCharTable);

        int binaryData[][] = new int[numChars][numBytesPerCharTable];
        for (int prevChar = 0; prevChar < kerningData.length; prevChar++) {
            for (int currChar = 0; currChar < numBytesPerCharTable; currChar++) {
                int byteValue = 0;
                int columnPos = currChar * NUM_ENTRIES_PER_BYTE;
                for (int i = 0; i < NUM_ENTRIES_PER_BYTE; i++) {
                    byteValue <<= 2;
                    if (columnPos + i < kerningData.length) {
                        byteValue |= (kerningData[prevChar][columnPos + i] & 0x3);
                    }
                }
                binaryData[prevChar][currChar] = byteValue;
            }
        }
        return binaryData;
    }

    private static boolean kerningTablesMatch(int kerningTable1[], int kerningTable2[]) {
        if (kerningTable1 == null || kerningTable2 == null) return false;
        if (kerningTable1.length != kerningTable2.length) return false;

        for (int i = 0; i < kerningTable1.length; i++) {
            if (kerningTable1[i] != kerningTable2[i]) {
                return false;
            }
        }
        return true;
    }

    private static int[] createIndexTable(int kerningData[][]) {
        // determine how many of the kerning tables are unique, and assign a
        // number to each character value that represents what # table to use
        // in the ROM once the duplicates are weeded out
        int indexTable[] = new int[kerningData.length];
        int numUniqueTables = 1;

        // this works but sadly is O(n^3)
        for (int currChar = 1; currChar < kerningData.length; currChar++) {
            int currKerningTable[] = kerningData[currChar];
            int index = numUniqueTables;
            for (int i = 0; i < currChar; i++) {
                int kerningTableToCheck[] = kerningData[i];
                if (kerningTablesMatch(currKerningTable, kerningTableToCheck)) {
                    // System.out.printf("Same kerning tables for %02X,%02X\n", i, currChar);
                    index = indexTable[i];
                    break;
                }
            }
            indexTable[currChar] = index;
            if (index == numUniqueTables) {
                numUniqueTables++;
            }
        }
        return indexTable;
    }

    // -------------------------------------------------------------------------

    private static void createBinaryPtrTable(int indexTable[], int numBytesPerTable) throws IOException {
        FileOutputStream binaryPtrTable = new FileOutputStream("font/kerning ptr table.bin");

        // in the ROM, write the kerning tables right after the pointer table
        int baseOffset = 2 * indexTable.length;
        for (int i = 0; i < indexTable.length; i++) {
            int ptr = baseOffset + indexTable[i] * numBytesPerTable;
            binaryPtrTable.write(ptr & 0xFF);
            binaryPtrTable.write((ptr >> 8) & 0xFF);
        }
        binaryPtrTable.flush();
        binaryPtrTable.close();
    }

    private static void outputBinaryKerningDataToFile(int binaryKerningData[][], int indexTable[]) throws IOException {
        FileOutputStream binaryOutput = new FileOutputStream("font/kerning tables.bin");
        int expectedNextIndex = 0;
        for (int i = 0; i < indexTable.length; i++) {
            // if current kerning table is unique so far, it must be output here
            if (indexTable[i] == expectedNextIndex) {
                for (int j = 0; j < binaryKerningData[i].length; j++) {
                    binaryOutput.write(binaryKerningData[i][j]);
                }
                // assumption: sequence of indices is monotonically increasing
                // minus any repeats
                expectedNextIndex++;
            }
        }
        binaryOutput.flush();
        binaryOutput.close();
    }

    // -------------------------------------------------------------------------

    public static void main(String args[]) throws IOException {
        int kerningData[][] = readKerningList();
        int binaryKerningData[][] = convertKerningDataToBinaryFormat(kerningData);
        int indexTable[] = createIndexTable(kerningData);

        int numBytesPerTable = binaryKerningData[0].length;
        createBinaryPtrTable(indexTable, numBytesPerTable);
        outputBinaryKerningDataToFile(binaryKerningData, indexTable);

        /*
        System.out.print("Index table:");
        for (int i = 0; i < indexTable.length; i++) {
            System.out.printf(" %02X", indexTable[i]);
        }
        */
    }
}
