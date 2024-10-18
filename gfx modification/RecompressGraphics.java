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

    private static void getDataGroups() {
        dataGroups = new ArrayList<>();

        // set up initial state for keeping track of groups
        // ASSUMPTION: an input file will contain at least one byte
        int prevByte = ((int) gfxData[0]) & 0xFF;
        int groupStart = 0;
        int groupSize = 1;
        boolean inRun = false;

        for (int pos = 1; pos < gfxData.length; pos++) {
            int currByte = ((int) gfxData[pos]) & 0xFF;
            if (inRun) {
                // run continues if data value matches and enough space exists
                if (currByte == prevByte && groupSize < MAX_RUN_LENGTH) {
                    groupSize++;
                }
                // otherwise, group is done, and set up for next one
                else {
                    DataGroup dataGroup = new DataGroup(inRun, groupSize, groupStart);
                    dataGroups.add(dataGroup);

                    // the next group is a sequence of literals that starts after
                    // the last byte of the current group
                    groupStart = pos;
                    groupSize = 1;
                    inRun = false;
                }
            }
            else {
                // list of literals stops if two matching bytes in a row
                if (currByte == prevByte) {
                    if (groupSize - 1 != 0) {
                        DataGroup dataGroup = new DataGroup(inRun, groupSize - 1, groupStart);
                        dataGroups.add(dataGroup);
                    }

                    // the next group is a run that starts on the first of the
                    // two matching bytes
                    groupStart = pos - 1;
                    groupSize = 2;
                    inRun = true;
                }
                // list of literals continues if two bytes do not match and if
                // there is enough space
                else {
                    if (groupSize == MAX_CONSEC_LITERALS) {
                        DataGroup dataGroup = new DataGroup(inRun, groupSize, groupStart);
                        dataGroups.add(dataGroup);

                        // the next group is a sequence of literals that starts
                        // on the current byte
                        groupStart = pos;
                        groupSize = 1;
                        inRun = false;
                    }
                    else {
                        groupSize++;
                    }
                }
            }
            prevByte = currByte;
        }

        // create object for whatever data is left over in the last group 
        DataGroup dataGroup = new DataGroup(inRun, groupSize, groupStart);
        dataGroups.add(dataGroup);
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
                    int combinedLength = lastCombinedDG.length + currDG.length;
                    int newLength = combinedLength - MAX_CONSEC_LITERALS;
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
        String format = "0x%5X: 0x%5X byte %s\n";
        for (DataGroup dg : dataGroups) {
            String type = dg.isRun ? "run" : "sequence";
            logFile.write(String.format(format, dg.position, dg.length, type));
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
