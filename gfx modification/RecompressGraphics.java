import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class RecompressGraphics {

    private static class DataGroup {
        private boolean isRun;
        private int length;
        private int position;

        public DataGroup(boolean isRun, int length, int position) {
            this.isRun = isRun;
            this.length = length;
            this.position = position;
        }

        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (!(obj instanceof DataGroup)) return false;
            DataGroup other = (DataGroup) obj;
            return (isRun == other.isRun) &&
                   (length == other.length) &&
                   (position == other.position);
        }

        public String toString() {
            String str = "0x%5X: 0x%5X byte %s\n";
            String type = isRun ? "run" : "sequence";
            return String.format(str, position, length, type);
        }
    }

    private static FileInputStream graphics;
    private static FileOutputStream outputFile;
    private static BufferedWriter logFile;
    private static byte[] gfxData;
    private static ArrayList<DataGroup> dataGroups;

    private static final int MAX_RUN_LENGTH = 0x80;
    private static final int MAX_CONSEC_LITERALS = 0x7F;
    private static final int BIT_FLAG_LITERAL = 0x80;
    private static final int DATA_TERM_BYTE = 0x80;

    private static String removeFileExtension(String filename) {
        int periodIndex = filename.lastIndexOf('.');
        return periodIndex == -1 ? filename : filename.substring(0, periodIndex);
    }

    private static void getGfxDataAsArray(String inputFile) throws IOException {
        // get file's size to know the exact size for the array
        File gfxFile = new File(inputFile);
        long fileLength = gfxFile.length();
        gfxData = new byte[(int) fileLength];

        graphics = new FileInputStream(inputFile);
        graphics.read(gfxData);
        graphics.close();
    }

    private static int getRunLengthAtPosition(int startPos) {
        if (startPos < 0 || startPos >= gfxData.length) return 0;

        int size = 1;
        // limit size of run based on the RLE format
        while (startPos + size < gfxData.length && size < MAX_RUN_LENGTH) {
            if (gfxData[startPos] != gfxData[startPos + size]) {
                break;
            }
            size++;
        }
        return size;
    }

    private static void getDataGroups() {
        dataGroups = new ArrayList<>();

        int pos = 0;
        int numLits = 0;
        while (pos < gfxData.length) {
            int runLength = getRunLengthAtPosition(pos);
            // run length of 1 means the value is a literal
            if (runLength == 1) {
                // combine into group of literals, if any
                // if size maxed out, this is the end of the group; output it
                numLits++;
                if (numLits == MAX_CONSEC_LITERALS) {
                    DataGroup lits = new DataGroup(false, numLits, pos - numLits);
                    dataGroups.add(lits);
                    numLits = 0;
                }
            }
            // run length of 2+ means, well, we have a run
            else {
                // output any literals we have accumulated so far
                if (numLits != 0) {
                    DataGroup lits = new DataGroup(false, numLits, pos - numLits);
                    dataGroups.add(lits);
                    numLits = 0;
                }

                // we already know how long the run is, so output the group
                DataGroup run = new DataGroup(true, runLength, pos);
                dataGroups.add(run);
            }
            // advance past run or literal
            pos += runLength;
        }

        // output any straggling literals we have at the end of the file 
        if (numLits > 0) {
            DataGroup lits = new DataGroup(false, numLits, pos - numLits);
            dataGroups.add(lits);
        }
    }

    private static void combineGroups() {
        // there is one optimization we can do based on runs of length 2
        // if a run of 2 is surrounded by sequences, you can save a byte by
        // combining the run of 2 with the two sequences into one big sequence
        // e.g. [03 04 05 05 06 07] <- [82 03 04 01 05 82 06 07]
        // but: [03 04 05 05 06 07] <- [86 03 04 05 05    06 07]

        // however, this only works if the combined sequence does not exceed the
        // size limit of 0x7F bytes -- need to account for this

        ArrayList<DataGroup> combinedDataGroups = new ArrayList<>();
        combinedDataGroups.add(dataGroups.get(0));
        for (int i = 1; i < dataGroups.size(); i++) {
            DataGroup lastCombinedDG = combinedDataGroups.get(combinedDataGroups.size() - 1);
            DataGroup currDG = dataGroups.get(i);

            // if last added a run, no advantage to combining in terms of space:
            // - if [run run]: defeats the purpose of the RLE compression
            // - if [run seq]: combining would use as much, or more, space; e.g.
            //   [08 08 09 0A 0B] -> [01 08 83 09 0A 0B]; Combining run of 2 is same
            //   [08 08 09 0A 0B] -> [85 08 08 09 0A 0B]
            //   [0C 0C 0C 0D 0E] -> [02 0C    82 0D 0E]; Combining run of 3+ is worse
            //   [0C 0C 0C 0D 0E] -> [85 0C 0C 0C 0D 0E]
            // so if last added a run, just add current group
            if (lastCombinedDG.isRun) {
                combinedDataGroups.add(currDG);
            }
            // if last combined data group is a sequence, combine any 2-runs into
            // it, as well as any sequences that come after it, as long as they fit
            else {
                boolean canCombine = !currDG.isRun || (currDG.isRun && currDG.length == 2);
                boolean canFit = lastCombinedDG.length + currDG.length <= MAX_CONSEC_LITERALS;

                if (canCombine && canFit) {
                    lastCombinedDG.length += currDG.length;
                }
                else if (!currDG.isRun && !canFit) {
                    // special case; combining groups can create a set like:
                    // [5 sequence] [7F sequence] [5 sequence]
                    // here, better to encode as [7F sequence] [A sequence]
                    // for now, group as [7F] [5] [5] -- 5s will combine later

                    // manually create new sequence with the remaining bytes
                    int newLength = (lastCombinedDG.length + currDG.length) - MAX_CONSEC_LITERALS;
                    int newPosition = lastCombinedDG.position + MAX_CONSEC_LITERALS;
                    DataGroup newGroup = new DataGroup(false, newLength, newPosition);
                    combinedDataGroups.add(newGroup);

                    // set length of previous sequence to be 0x7F
                    lastCombinedDG.length = MAX_CONSEC_LITERALS;
                }
                else {
                    combinedDataGroups.add(currDG);
                }
            }
        }

        dataGroups = combinedDataGroups;
    }

    private static void interpretGroups() throws IOException {
        // for debugging group creation process -- sizes, starts, types
        for (DataGroup dg : dataGroups) {
			logFile.write(dg.toString());
        }
    }

    private static void generateCompressedFile() throws IOException {
        for (DataGroup dg : dataGroups) {
            if (!dg.isRun) {
                // control byte for sequence: (group_length | 0x80)
                int ctrlByte = BIT_FLAG_LITERAL | (dg.length);
                outputFile.write(ctrlByte);

                // write the sequence's bytes from file into output
                for (int i = 0; i < dg.length; i++) {
                    outputFile.write(gfxData[dg.position + i]);
                }
            }
            else {
                // control byte for run: (run_length - 1)
                int ctrlByte = dg.length - 1;
                outputFile.write(ctrlByte);

                // write the data byte for the run
                outputFile.write(gfxData[dg.position]);
            }
        }
        // write the data format's 0x80 terminator byte
        outputFile.write(DATA_TERM_BYTE);
    }

    public static void main(String args[]) throws IOException {
        if (args.length == 0) {
            System.out.println("Sample usage: java RecompressGraphics data1.bin [data2.bin data3.bin ...]");
            return;
        }

        final String FILE_PREFIX = "recompressed ";
        for (String inputFile : args) {
            // allow using a wildcard like: java RecompressGraphics *.bin
            // however, do not compress any binary files that themselves are
            // the result of compressing a binary file with this program
            if (inputFile.startsWith(FILE_PREFIX)) {
                continue;
            }

            String outputFilename = FILE_PREFIX + inputFile;
            String logFilename = "LOG " + removeFileExtension(inputFile) + ".txt";

            getGfxDataAsArray(inputFile);
            if (gfxData.length == 0) {
                // skip any empty files
                continue;
            }
            getDataGroups();
            combineGroups();

            logFile = new BufferedWriter(new FileWriter(logFilename));
            interpretGroups();
            logFile.flush();
            logFile.close();

            outputFile = new FileOutputStream(outputFilename);
            generateCompressedFile();
            outputFile.close();
        }
    }
}
