import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

public class FontInserter {

    private static boolean DEBUG = true;

    // private static final int LOOKUP_TABLE_START = 0x074AC;
    // original value in JP game is: 0xF95A8
    // // private static final int FONT_DATA_START =    0xAE401;
    // private static final int PTR_TO_FONT_BANK_NUM = 0x0256D;
    // private static final int LOOKUP_TABLE_STRUCT_SIZE = 5;
    // private static final int MAX_CHAR_GROUPS = 58;

    private String tableFileName;
    // private String romFileName;
    private FontImage fontImage;
    private ArrayList<FontInfo> fontInfoArray;

    // private RandomAccessFile romStream;
    // using RandomAccessFile instead of something like FileOutputStream
    // because RAF gives you a "get current file position" method
    // if you don't seek() anywhere, it functions as a "get file size"
    private RandomAccessFile fontDataOutput;
    // private RandomAccessFile fontLookupTable;

    private BufferedWriter newTableFile;
    private BufferedWriter newLETableFile;
    private BufferedWriter logFile;

    // *************************************************************************
    // Helper functions
    // *************************************************************************

    /*private void printFontInfoArray() {
        for (int i = 0; i < fontInfoArray.size(); i++) {
            FontInfo fontInfo = fontInfoArray.get(i);
            String format = "i = %04X ; %04X = '%s'\t; %2dx%2d";
            System.out.println(String.format(format, i, fontInfo.getHexValue(),
                               fontInfo.getEncoding(), fontInfo.getWidth(),
                               fontInfo.getHeight()));
        }
        System.out.println();
    }
    */

    private void readTableFile() {
        fontInfoArray = new ArrayList<>();
        try {
            BufferedReader tableFileStream = new BufferedReader(new FileReader(tableFileName));

            // for this table file, format is "[hex value]\t[character]\t[width]\t[height]"
            String line;
            while ((line = tableFileStream.readLine()) != null) {
                if (line.equals(""))
                    continue;

                String split[] = line.split("\t");
                if (split.length != 4) {
                    System.out.println("Malformed table file line:\n" + line + "\n");
                    tableFileStream.close();
                    return;
                }

                short value = Short.parseShort(split[0], 16);
                String encoding = split[1];
                int width  = Integer.parseInt(split[2]);
                int height = Integer.parseInt(split[3]);

                fontInfoArray.add(new FontInfo(value, encoding, width, height));
            }
            if (DEBUG) {
                // printFontInfoArray();
            }
            tableFileStream.close();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private int getNumEntries() {
        int tableFileSize = fontInfoArray.size();
        int numChars = fontImage.getNumChars();
        return Math.min(tableFileSize, numChars);
    }

    private void combineFontDataWithTableFile() {
        // safety just in case these sizes don't match up
        int numEntries = getNumEntries();

        for (int i = 0; i < numEntries; i++) {
            fontInfoArray.get(i).setFontData(fontImage.getPixelDataForChar(i));
        }
    }

    public void initialize() {
        // get the font data from the image
        try {
            fontImage.initialize();
            // fontImage.convertBitsToBinFile();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }

        // extract the data from the table file
        readTableFile();

        // associate the font data for each character with its appropriate entry
        combineFontDataWithTableFile();

        /* if (DEBUG) {
            printFontInfoArray();
        }
        */
    }

    // *************************************************************************
    // Constuctor
    // *************************************************************************

    public FontInserter(String tableFileName, FontImage fontImage) {
        // note: leaving to driver class to instantiate FontImage object
        this.tableFileName = tableFileName;
        this.fontImage = fontImage;
    }

    // *************************************************************************
    //
    // *************************************************************************

    /**
     * Maps from single characters (from an in-game storage perspective) to
     * their hexadecimal values, such as "AUTO ADV 13" -> 0x1013.
     */
    private static HashMap<String, Integer> tableFileHashMap;

    private static final int AUTO_ADV_VAL = 0x1013;
    private static final int DELAY_VAL = 0x1017;
    private static final int KERN_LEFT_VAL = 0x100A;

    private static int swapEndianness(int hexValue) {
        return (hexValue >> 8) | ((hexValue & 0xFF) << 8);
    }

    private static String hexAsString(int hexValue) {
        return String.format("%04X", hexValue);
    }

    // add an entry to both the big endian and little endian table files
    private void addTableFileEntry(int hexValue, String encoding) throws IOException {
        // use "\r\n" (not "\n") so that table file works properly with Atlas
        String tableFormat = "%s=%s\r\n";

        String tableFileEntry = String.format(tableFormat, hexAsString(hexValue), encoding);
        newTableFile.write(tableFileEntry);

        int hexValueLE = swapEndianness(hexValue);
        String tableFileEntryLE = String.format(tableFormat, hexAsString(hexValueLE), encoding);
        newLETableFile.write(tableFileEntryLE);
    }

    // use table file to handle kerning "automatically", without replacing all
    // instances of [char1][char2] with [char1]<KERN><##>[char2] in the script
    private void addKerningPairs() throws IOException {
        final String KERN_LEFT = "<KERN LEFT>";
        String encodings[] = KerningPunctPairs.getKerningEncodings();
        String hexSequenceStrings[][] = KerningPunctPairs.getKerningHexSequences();

        // the lists are essentially hard-coded, so put in a basic sanity check
        // for bad formatting like unmatched brackets
        if (hexSequenceStrings.length != encodings.length) {
            throw new IOException("Source code formatting error: Sizes of lists for kerning combos do not match: " + (hexSequenceStrings.length) + " & " + (encodings.length) );
        }

        String tableFormat = "%s=%s\r\n";
        for (int i = 0; i < hexSequenceStrings.length; i++) {
            // put a new line after every four table entries for formatting
            if ((i & 0x3) == 0) {
                newTableFile.newLine();
            }
            String hexValue = "";
            for (int j = 0; j < hexSequenceStrings[i].length; j++) {
                String hexSequence = hexSequenceStrings[i][j];

                // if got kern left code, first add the code, then the # of pixels
                if (hexSequence.equals(KERN_LEFT)) {
                    hexValue += hexAsString(KERN_LEFT_VAL);

                    String numPixelsString = hexSequenceStrings[i][j + 1];
                    hexValue += numPixelsString;
                    // advance past the entry with the # of pixels
                    j++;
                }

                // otherwise, get the hex value for the character and add it
                else {
                    Integer value = tableFileHashMap.get(hexSequence);
                    if (value == null) {
                        String error = "No mapping for encoding \"%s\" (position %d,%d)";
                        throw new IOException(String.format(error, hexSequence, i, j));
                    }
                    hexValue += hexAsString(value);
                }
            }
            newTableFile.write(String.format(tableFormat, hexValue, encodings[i]));
        }
    }

    private void addCompoundPunctuation() throws IOException {
        String encodings[] = KerningPunctPairs.getPunctuationEncodings();
        // use these lists as keys to the HashMap tableFileHashMap
        String hexSequenceStrings[][] = KerningPunctPairs.getPunctuationHexSequences();

        tableFileHashMap.put(KerningPunctPairs.AUTO_ADV_STR, AUTO_ADV_VAL);
        tableFileHashMap.put(KerningPunctPairs.DELAY_STR, DELAY_VAL);
        // System.out.println("tableFileHashMap has " + tableFileHashMap.size() + " mappings in it");

        // the lists are essentially hard-coded, so put in a basic sanity check
        // for bad formatting like unmatched brackets
        if (hexSequenceStrings.length != encodings.length) {
            throw new IOException("Source code formatting error: Sizes of lists for punctuation combos do not match: " + (hexSequenceStrings.length) + " & " + (encodings.length) );
        }

        String tableFormat = "%s=%s\r\n";
        for (int i = 0; i < hexSequenceStrings.length; i++) {
            // put a new line after every four table entries for formatting
            if ((i & 0x3) == 0) {
                newTableFile.newLine();
            }
            String hexValue = "";
            for (int j = 0; j < hexSequenceStrings[i].length; j++) {
                String hexSequence = hexSequenceStrings[i][j];
                Integer value = tableFileHashMap.get(hexSequence);
                if (value == null) {
                    throw new IOException("No mapping for the encoding \"" + hexSequence + "\" (position " + i + "," + j + ")");
                }
                hexValue += hexAsString(value);
            }
            newTableFile.write(String.format(tableFormat, hexValue, encodings[i]));
        }
    }

    /**
     * Generate a binary file with the (little endian) hex values for characters
     * that should have the property "line break after this character if past
     * right margin"
     * @throws IOException
     */
    private void createListOfLinebreakChars() throws IOException {
        FileOutputStream lineBreakListFile = new FileOutputStream("font/auto linebreak chars.bin");

        String linebreakList[] = {" ", "-", "　"};
        for (String str : linebreakList) {
            // take the hex value for the file
            Integer hexValue = tableFileHashMap.get(str);
            if (hexValue != null && tableFileHashMap.containsKey(str)) {
                // write the hex value in little endian order to the file
                lineBreakListFile.write(hexValue & 0xFF);
                lineBreakListFile.write((hexValue >> 8) & 0xFF);
            }
        }

        // write the list terminator 0xFFFF
        lineBreakListFile.write(0xFF);
        lineBreakListFile.write(0xFF);

        lineBreakListFile.close();
    }

    private void addCtrlCodesToGeneratedTbl() throws IOException {
        // recommendation: this file should also have the five values only used
        // as inputs to the CHOICE codes (0704, 0745, 0805, 080D, 0905, 1176)
        BufferedReader ctrlCodesTbl = new BufferedReader(new FileReader("tables/control codes.tbl"));
        String entry = "";
        while ((entry = ctrlCodesTbl.readLine()) != null) {
            newTableFile.write(entry + "\r\n");
        }
        ctrlCodesTbl.close();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private int getFontDataRow(boolean pixelRow[]) {
        int dataBuffer = 0x0000;
        for (int c = 0; c < pixelRow.length; c++) {
            dataBuffer |= (pixelRow[c] ? 1 : 0) << (pixelRow.length - 1 - c);
        }
        return dataBuffer;
    }

    public void convertFontDataToUncompGameFormat() {
        // generate the font in the format that it ultimately gets decompressed
        // to, instead of the compression format used in the original game
        int numChars = getNumEntries();
        try {
            // originally, this overwrote data in the ROM file itself, but
            // this now writes data to their own individual files instead
            fontDataOutput = new RandomAccessFile("font/new uncomp font data.bin", "rw");

            // NOTE: let Asar handle insertion and updating bank # instead

            // keep two table files: one with big endian entries (actual script),
            // and another with little endian entries (name entry screen, lists
            // of characters for things like "auto WAIT" or choice option letters)
            newTableFile = new BufferedWriter(new FileWriter("tables/uncomp font.tbl"));
            newLETableFile = new BufferedWriter(new FileWriter("tables/uncomp font LE.tbl"));
            logFile = new BufferedWriter(new FileWriter("logs/uncomp font insertion log.txt"));

            tableFileHashMap = new HashMap<>();

            // always first write the Japanese space entry
            addTableFileEntry(0x0000, "　");
            tableFileHashMap.put("　", 0x0000);

            for (int ch = 0; ch < numChars; ch++) {
                FontInfo fontInfo = fontInfoArray.get(ch);
                int charHeight = fontImage.getCharHeight();

                int width = fontInfo.getWidth();
                int height = fontInfo.getHeight();

                // use (ch + 1) to account for 0000 being justified space
                if (DEBUG) {
                    String format = "Char %04X '%s' is %dx%d\r\n";
                    logFile.write(String.format(format, ch + 1, fontInfo.getEncoding(), fontInfo.getWidth(), fontInfo.getHeight()));
                }

                // write a byte containing the width and height
                int whByte = (width << 4) | height;
                fontDataOutput.write(whByte);

                // then write an empty byte for alignment purposes
                fontDataOutput.write(0x00);

                // Otogirisou calculates its heights going from the bottom up
                // need to determine top row of the character and start there in 16x16 buffer
                int startRow = charHeight - fontInfo.getHeight();
                boolean charData[][] = fontInfo.getFontData();

                for (int r = 0; r < height; r++) {
                    // for uncompressed font format, take a whole row and write
                    // it in big endian, for ease of use with a tile editor
                    boolean pixelRow[] = charData[startRow + r];
                    int rowData = getFontDataRow(pixelRow);

                    if (DEBUG) {
                        String format = "0x%05X: %04X\r\n";
                        logFile.write(String.format(format, fontDataOutput.getFilePointer(), rowData));
                    }
                    fontDataOutput.write(rowData >> 8);
                    fontDataOutput.write(rowData & 0xFF);
                }
                for (int r = height; r < charHeight - 1; r++) {
                    // for the remaining empty rows, simply write 0x00 bytes
                    fontDataOutput.write(0x00);
                    fontDataOutput.write(0x00);
                }

                // create entry in new table files
                // add mapping to the table file HashMap
                addTableFileEntry(fontInfo.getHexValue() + 1, fontInfo.getEncoding());
                tableFileHashMap.put(fontInfo.getEncoding(), fontInfo.getHexValue() + 1);
            }

            fontDataOutput.close();

            addCtrlCodesToGeneratedTbl();
            addCompoundPunctuation();
            addKerningPairs();
            createListOfLinebreakChars();

            newTableFile.flush();
            newTableFile.close();
            newLETableFile.flush();
            newLETableFile.close();

            logFile.flush();
            logFile.close();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // NOTE: Everything past this point is what I used to use for inserting the
    // font in the original compressed format. This is otherwise the end of the
    // file.
    // -------------------------------------------------------------------------

    /*
    private void printErrorForTooManyPairs(FontInfo fontInfo, int ch) throws IOException {
        String errorMessage = "ERROR - supplied font has too many distinct WxH pairs!";

        String format = "Execution halted with %dx%d char %04X = '%s'";
        String output = String.format(format, fontInfo.getWidth(),
            fontInfo.getHeight(), ch, fontInfo.getEncoding());

        logFile.write(errorMessage + "\r\n");
        logFile.write(output + "\r\n");
        logFile.flush();

        System.out.println(errorMessage);
        System.out.println("Please check \"font insertion log.txt\"");
    }

    private byte[] calculateFiveByteStructData(FontInfo fontInfo, int numCharsInGroup, int groupStartOffset) throws IOException {
        byte fiveByteStruct[] = new byte[LOOKUP_TABLE_STRUCT_SIZE];

        // byte 0 is low 8 bits of range value
        fiveByteStruct[0] = (byte) (numCharsInGroup & 0xFF);

        // byte 1 is char width, and high 4 bits of range value
        fiveByteStruct[1] = (byte) (((fontInfo.getWidth() & 0xF) << 4) |
                    ((numCharsInGroup >> 8) & 0xF));

        // bytes 2 and 3 are low and high byte of bank offset for start of char
        // group; convert file "hex editor" offset to LoROM bank offset
        int bankOffset = ((int) (groupStartOffset & 0xFFFF)) | 0x8000;
        fiveByteStruct[2] = (byte) ((bankOffset & 0xFF));
        fiveByteStruct[3] = (byte) ((bankOffset >> 8) & 0xFF);

        // byte 4 is char height, shifted left twice
        fiveByteStruct[4] = (byte) ((fontInfo.getHeight() & 0xF) << 2);

        return fiveByteStruct;
    }

    public void convertFontDataToGameFormat() {
        // for 1st character in a WxH group, write bits into ROM starting at LSB
        // fill up a byte going from LSB to MSB
        // subsequent characters in group continue at bit where last one ends
        // always start at LSB for new WxH groups
        int bitOffset = 0;
        int numChars = getNumEntries();
        int numCharsInGroup = 0;
        int dataBuffer = 0;
        int numCharGroups = 0;

        // due to nature of Otogirisou's font storage format, sort characters
        // by width, height and then by encoding
        Collections.sort(fontInfoArray, new FontInfoDimensionComparator());

        try {
            // originally, this overwrote data in the ROM file itself, but
            // this now writes data to their own individual files instead
            fontDataOutput = new RandomAccessFile("font/new font data.bin", "rw");
            fontLookupTable = new RandomAccessFile("font/new font lookup table.bin", "rw");

            // NOTE: let Asar handle insertion and updating bank # instead

            // just set it to 0 at the start
            long groupStartOffset = 0x0;

            // keep two table files: one with big endian entries (actual script),
            // and another with little endian entries (name entry screen, lists
            // of characters for things like "auto WAIT" or choice option letters)
            newTableFile = new BufferedWriter(new FileWriter("tables/inserted font.tbl"));
            newLETableFile = new BufferedWriter(new FileWriter("tables/inserted font LE.tbl"));
            logFile = new BufferedWriter(new FileWriter("logs/font insertion log.txt"));

            tableFileHashMap = new HashMap<>();

            // always first write the Japanese space entry
            addTableFileEntry(0x0000, "　");
            tableFileHashMap.put("　", 0x0000);

            for (int ch = 0; ch < numChars; ch++) {
                FontInfo fontInfo = fontInfoArray.get(ch);

                // note: original game has space for at most 58 structures
                // possibly allow specifying a different location for them
                // with more space, similar for the font data itself
                if (numCharGroups >= MAX_CHAR_GROUPS) {
                    printErrorForTooManyPairs(fontInfo, ch);
                    break;
                }

                // use (ch + 1) to account for 0000 being justified space
                if (DEBUG) {
                    String format = "Char %04X '%s' is %dx%d\r\n";
                    logFile.write(String.format(format, ch + 1, fontInfo.getEncoding(), fontInfo.getWidth(), fontInfo.getHeight()));
                    logFile.flush();
                }

                // Otogirisou calculates its heights going from the bottom up
                // need to determine top row of the character and start there in 16x16 buffer
                int startRow = fontImage.getCharHeight() - fontInfo.getHeight();
                boolean charData[][] = fontInfo.getFontData();
                numCharsInGroup++;

                for (int r = 0; r < fontInfo.getHeight(); r++) {
                    // if you write bits starting from left, chars are mirrored
                    // horizontally, so write bits starting from right instead
                    for (int c = fontInfo.getWidth() - 1; c >= 0; c--) {
                        int pixel = charData[startRow + r][c] ? 1 : 0;
                        dataBuffer |= pixel << bitOffset;

                        bitOffset = (bitOffset + 1) & 0x7;
                        if (bitOffset == 0) {
                            if (DEBUG) {
                                String format = "0x%05X: %02X\r\n";
                                logFile.write(String.format(format, fontDataOutput.getFilePointer(), dataBuffer));
                            }
                            fontDataOutput.writeByte(dataBuffer);
                            dataBuffer = 0;
                        }
                    }
                }
                logFile.flush();

                // create entry in new table files sorted by heights/widths
                // add mapping to the table file HashMap
                addTableFileEntry(ch + 1, fontInfo.getEncoding());
                tableFileHashMap.put(fontInfo.getEncoding(), ch + 1);

                // done with char; need to update font lookup table if either:
                // - no characters left to insert
                // - next char has a different height
                // - next char has a different width
                // it is assumed that in this loop, (ch != numChars -1) implies
                // that there is at least 1 more char to look at
                if (ch == numChars - 1 ||
                    fontInfo.getWidth()  != fontInfoArray.get(ch + 1).getWidth() ||
                    fontInfo.getHeight() != fontInfoArray.get(ch + 1).getHeight()) {

                    // if last character does not end on byte boundary,
                    // have to write the last N bits of the character
                    if (bitOffset != 0) {
                        if (DEBUG) {
                            String format = "0x%05X: %02X - last for group\r\n";
                            logFile.write(String.format(format, fontDataOutput.getFilePointer(), dataBuffer));
                            logFile.flush();
                        }
                        fontDataOutput.writeByte(dataBuffer);
                        dataBuffer = 0;
                        bitOffset = 0;
                    }

                    // update the font data lookup table
                    // save font space pointer for debug output
                    long lookupTableOffset = fontLookupTable.getFilePointer();
                    byte fiveByteStruct[] = calculateFiveByteStructData(fontInfo, numCharsInGroup, (int) groupStartOffset);
                    for (int i = 0; i < fiveByteStruct.length; i++) {
                        fontLookupTable.writeByte(fiveByteStruct[i]);
                    }

                    if (DEBUG) {
                        // size of the WxH group in bits
                        int groupSize = numCharsInGroup * fontInfo.getWidth() * fontInfo.getHeight();

                        // print the five bytes written to the lookup table
                        String format1 = "Lookup table @ 0x%05X: %02X %02X %02X %02X %02X\r\n";
                        logFile.write(String.format(format1, lookupTableOffset,
                            fiveByteStruct[0], fiveByteStruct[1], fiveByteStruct[2],
                            fiveByteStruct[3], fiveByteStruct[4]));

                        // print # of chars in group, plus where they are in ROM
                        String format2 = "%d chars are %dx%d from 0x%05X-0 to 0x%05X-%1X\r\n";
                        logFile.write(String.format(format2, numCharsInGroup,
                            fontInfo.getWidth(), fontInfo.getHeight(),
                            groupStartOffset, fontDataOutput.getFilePointer(), groupSize & 0x7));

                        // print # of char groups so far, and size of curr group
                        String format3 = "Group #%d is %d bits = 0x%X-%d bytes\r\n";
                        logFile.write(String.format(format3, numCharGroups + 1,
                            groupSize, groupSize >> 3, groupSize & 0x7));

                        logFile.write("\r\n");
                    }

                    // initialize variables for the next group of characters
                    numCharsInGroup = 0;
                    numCharGroups++;
                    groupStartOffset = fontDataOutput.getFilePointer();
                }
            }

            // pad lookup table space to 58 structures, to avoid possiblility of
            // reading data from the original Japanese table 
            while (numCharGroups < MAX_CHAR_GROUPS) {
                // meaning: 0xFFF chars are 1x1 and start at 0x??8000
                fontLookupTable.writeByte(0xFF);
                fontLookupTable.writeByte((0x01 << 4) | 0xF);
                fontLookupTable.writeByte(0x00);
                fontLookupTable.writeByte(0x80);
                fontLookupTable.writeByte(0x01 << 2);

                numCharGroups++;
            }

            fontDataOutput.close();
            fontLookupTable.close();

            addCtrlCodesToGeneratedTbl();
            addCompoundPunctuation();
            addKerningPairs();
            createListOfLinebreakChars();

            newTableFile.flush();
            newTableFile.close();
            newLETableFile.flush();
            newLETableFile.close();

            logFile.flush();
            logFile.close();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
        //convertToGameFormat(fontInfo, 0);
    }
    */

}
