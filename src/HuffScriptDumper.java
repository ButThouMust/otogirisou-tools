import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HuffScriptDumper {

    // control codes for special cases when interpretting or when printing
    private static final int NUM_CTRL_CODES = 0x47;
    private static final int LINE_00 = 0x1000;
    private static final int JMP_03 =  0x1003;
    private static final int JMP_CC_04 = 0x1004;
    // private static final int CREDITS_09 = 0x1009;
    // private static final int CREDITS_0C = 0x100C;
    private static final int CHOICE_19 = 0x1019;
    private static final int CHOICE_1A = 0x101A;
    private static final int END_CHOICE_1C = 0x101C;
    private static final int NOP_1D = 0x101D;
    private static final int NAME_SAN_20 = 0x1020;
    private static final int NAME_21 = 0x1021;
    // private static final int SET_FLAG_22 = 0x1022;
    private static final int CLEAR_25  = 0x1025;
    private static final int CLEAR_27  = 0x1027;

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static RandomAccessFile romFile;
    private static BufferedWriter scriptOutput;
    private static BufferedWriter goryDetails;
    private static boolean shouldWriteDetails;

    // -------------------------------------------------------------------------
    // Lists for how many and what types of arguments are there for control codes
    // -------------------------------------------------------------------------

    private static int numArgs[] = new int[NUM_CTRL_CODES];
    private static int argTypes[] = new int[NUM_CTRL_CODES];
    private static final int CTRL_CODE_ARG_TBL = 0x0741E;
    private static final int CHAR_ARG = 0;
    private static final int PTR_ARG = 1;

    private static void getCtrlCodeArgTable() throws IOException {
        romFile.seek(CTRL_CODE_ARG_TBL);
        for (int i = 0; i < NUM_CTRL_CODES; i++) {
            numArgs[i] = romFile.readUnsignedByte();
            argTypes[i] = romFile.readUnsignedByte();
        }
    }

    // -------------------------------------------------------------------------
    // Table file's data structures and code
    // -------------------------------------------------------------------------

    private static ArrayList<Integer> tableHexValues;
    // private static ArrayList<String> encodings;
    private static HashMap<Integer, String> encodings;
    private static final int NUM_ENCODINGS = 1700;
    // private static final String NO_TABLE_MATCH = "N/A";

    private static void readTableFile(String tableFilename) throws IOException {
        // assume file is sorted in increasing order by character code value
        BufferedReader tableFileStream = new BufferedReader(new FileReader(tableFilename));
        tableHexValues = new ArrayList<>(NUM_ENCODINGS);
        encodings = new HashMap<>(NUM_ENCODINGS);

        // basic format of a table file line is "[hex value]=[character]\n"
        String line;
        final String EQUALS = "=";
        while ((line = tableFileStream.readLine()) != null) {
            if (line.equals(""))
                continue;

            String split[] = line.split(EQUALS);

            // ignore special combination table entries that keep the script
            // simpler, like for ".\"" or "?!"
            if (split[0].length() > 4)
                continue;

            int value = Integer.parseInt(split[0], 16);
            tableHexValues.add(value);

            // possible for a table entry to be for the equal sign "="
            if (split.length == 1) {
                encodings.put(value, EQUALS);
            }
            else {
                encodings.put(value, split[1]);
            }
        }

        System.out.println("Finished parsing table file.");
        tableFileStream.close();
        return;
    }

    private static String getEncoding(int data) {
        // data &= 0x1FFF;
        // int index = Collections.binarySearch(tableHexValues, data);

        String encoding = encodings.get(data & 0x1FFF);
        // String encoding = NO_TABLE_MATCH;
        // if (index >= 0) {
            // encoding = encodings.get(index);
        // }
        return encoding;
    }

    // -------------------------------------------------------------------------
    // Data for the Huffman tree structure
    // -------------------------------------------------------------------------

    private static final int HUFF_LEFT_OFFSET = 0x141E1;
    private static final int HUFF_RIGHT_OFFSET = 0x14D4F;
    private static final int HUFF_TABLE_ENTRIES = 0x5B7;

    // important: these hard-coded values for script start were changed to match
    // what the game has as the first start point
    // private static final int SCRIPT_START_BANK_OFFSET = 0xE401;
    // private static final int SCRIPT_START_BANK_NUMBER = 0x15;
    private static int scriptStartBankOffset;
    private static int scriptStartBankNumber;

    private static int[] huffLeftTrees = new int[HUFF_TABLE_ENTRIES];
    private static int[] huffRightTrees = new int[HUFF_TABLE_ENTRIES];

    private static int bitOffset;
    private static int huffmanBuffer;

    // using this because I gave up trying to use RandomAccessFile's internal file
    // pointer for checking pointer targets (EMBWRITEs) and outputting "new page"
    // pointers in script; they would always be off by one
    private static int currByteOffset;

    private static void getHuffmanTreeData() throws IOException {
        romFile.seek(HUFF_LEFT_OFFSET);
        int data = 0;
        for (int i = 0; i < HUFF_TABLE_ENTRIES; i++) {
            data = romFile.readUnsignedByte();
            data |= (romFile.readUnsignedByte() << 8);
            huffLeftTrees[i] = data;
        }

        romFile.seek(HUFF_RIGHT_OFFSET);
        for (int i = 0; i < HUFF_TABLE_ENTRIES; i++) {
            data = romFile.readUnsignedByte();
            data |= (romFile.readUnsignedByte() << 8);
            huffRightTrees[i] = data;
        }
        System.out.println("Finished getting Huffman tree's contents.");
    }

    // -------------------------------------------------------------------------
    // "API" for reading raw binary data from the script
    // -------------------------------------------------------------------------

    private static int getCPUOffset(int romOffset) {
        int bankOffset = (romOffset & 0xFFFF) | 0x8000;
        int bankNum = 1 + (romOffset - bankOffset) / 0x8000;

        int cpuOffset = (bankNum << 16) | bankOffset;
        return cpuOffset;
    }

    // uses the independent "current byte offset"; used for navigating the script
    private static int getCPUOffsetVar() {
        return getCPUOffset(currByteOffset);
    }

    // uses the RandomAccessFile's file pointer; used for start points
    private static int getCPUOffsetFP() throws IOException {
        return getCPUOffset((int) (romFile.getFilePointer() & 0xFFFFFF));
    }

    // read a byte without advancing file pointer
    private static int peekByte() throws IOException {
        int output = romFile.readUnsignedByte();
        romFile.seek(romFile.getFilePointer() - 1);
        return output;
    }

    private static int readCharacter() throws IOException {
        // start going left/right from the root of the Huffman tree
        int huffTreeValue = HUFF_TABLE_ENTRIES - 1;
        int startOffset = getCPUOffsetVar();
        int oldBitOffset = bitOffset;
        String huffCode = "";
        int length = 0;

        // Huffman leaf node (character) indicated by a SET MSB in tree value
        while ((huffTreeValue & 0x8000) == 0) {
            // read another byte if exhausted all 8 bits in buffer
            // important note: by default, getFilePointer() will point to the byte
            // AFTER the current Huffman code when we call readUnsignedByte()
            // we want to keep the file pointer AT the current byte of the Huffman
            // code, so to read the next byte, we must advance the pointer by 1
            // and peek the next/"current" byte
            if (bitOffset == 0) {
                romFile.readUnsignedByte();
                huffmanBuffer = peekByte();
            }

            // LSB = 1 -> left tree ------ LSB = 0 -> right tree
            boolean useLeftTree = (huffmanBuffer & 0x1) == 1;

            // update state of and position in the Huffman buffer
            bitOffset = (bitOffset + 1) & 0x7;
            huffmanBuffer >>= 1;

            // update current position in the ROM
            if (bitOffset == 0) {
                currByteOffset++;
            }

            if (useLeftTree) {
                huffTreeValue = huffLeftTrees[huffTreeValue];
                huffCode += "1";
            }
            else {
                huffTreeValue = huffRightTrees[huffTreeValue];
                huffCode += "0";
            }

            // put spaces between every 8 bits in a Huffman code
            if ((++length & 0x7) == 0) {
                huffCode += " ";
            }
        }

        if (shouldWriteDetails) {
            String format = "%06X-%d: %4X = 0b%s\n";
            goryDetails.write(String.format(format, startOffset, oldBitOffset, huffTreeValue & 0x1FFF, huffCode));
        }
        return huffTreeValue & 0x1FFF;
    }

    private static int readPointer() throws IOException {
        // the 24-bit pointer can be spread across four bytes in the ROM
        // example:    [0000]0000 [11111111] [22222222] 3333[3333]
        // big endian: 3333[3333] [22222222] [11111111] [0000]0000

        // all possible cases:
        // range from: 3[3333333] [22222222] [11111111] [0]0000000 (offset 7)
        //       to:   33[333333] [22222222] [11111111] [00]000000 (offset 6) 
        //       to:   333[33333] [22222222] [11111111] [000]00000 (offset 5) 
        //       to:   3333[3333] [22222222] [11111111] [0000]0000 (offset 4) 
        //       to:   33333[333] [22222222] [11111111] [00000]000 (offset 3) 
        //       to:   333333[33] [22222222] [11111111] [000000]00 (offset 2) 
        //       to:   3333333[3] [22222222] [11111111] [0000000]0 (offset 1) 
        //       to:   33333333   [22222222] [11111111] [00000000] (offset 0) 
        // byte 0's part of the pointer is already in huffmanBuffer
        // the rest of it was already consumed from a previous Huffman code (character)
        // upon exiting, value of huffmanBuffer must be byte 3, shifted right "bitOffset" times

        int cpuOffset = getCPUOffsetVar();

        // advance past byte 0, get 3 bytes, stop file pointer at the third byte
        romFile.readUnsignedByte();
        int byte1 = romFile.readUnsignedByte();
        int byte2 = romFile.readUnsignedByte();
        int byte3 = peekByte();
        currByteOffset += 3;

        // combine the raw data into one variable, and truncate to 24 bits
        // purpose of shifting out 8 if byte aligned: the Huffman buffer will
        // contain 00 from byte before the pointer i.e. we don't care about it
        // for calculating the pointer
        boolean byteAligned = (bitOffset & 0x7) == 0;
        int shiftAmount = byteAligned ? 8 : bitOffset;
        int rawData = huffmanBuffer;
        rawData |= byte1 << (1 * 8 - shiftAmount);
        rawData |= byte2 << (2 * 8 - shiftAmount);
        rawData |= byte3 << (3 * 8 - shiftAmount);
        rawData = rawData & 0xFFFFFF;

        if (shouldWriteDetails) {
            String format = "[%02X %02X %02X %02X] -> (%02X%02X%02X << %d) | %02X = %06X\n";
            goryDetails.write(String.format(format, huffmanBuffer, byte1, byte2, byte3, byte3, byte2, byte1, 8 - shiftAmount, huffmanBuffer, rawData));
        }

        // calculate the pointer info encoded in the three bytes of raw data
        // [76] [5432] [1076543 21076543] [210]; top two MSBs unused
        int ptrBitOffset = rawData & 0x7;
        int ptrNumBytes = (rawData >> 3) & 0x7FFF;
        int ptrNumBanks = (rawData >> 18) & 0xF;

        // return a Huffman CPU pointer encoded like the start points: 21+3 bits
        int ptrBankOffset = (scriptStartBankOffset + ptrNumBytes) | 0x8000;
        int ptrBank = scriptStartBankNumber + ptrNumBanks;
        int pointer = (ptrBank << 16) + ptrBankOffset;
        pointer = (pointer << 3) | ptrBitOffset;

        // write the script pointer's location, its data, and its decoded target
        huffmanBuffer = byte3 >> shiftAmount;
        if (shouldWriteDetails) {
            String format2 = "%06X-%d: %06X -> %06X-%d\n";
            goryDetails.write(String.format(format2, cpuOffset, bitOffset & 0x7, rawData, pointer >> 3, ptrBitOffset));
            goryDetails.write(String.format("Huffman buffer: %02X\n", huffmanBuffer));
        }
        return pointer;
    }

    private static int readStartPoint() throws IOException { 
        int byte0 = romFile.readUnsignedByte();
        int byte1 = romFile.readUnsignedByte();
        int byte2 = romFile.readUnsignedByte();
        return (byte2 << 16) | (byte1 << 8) | byte0;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static void printChar(int charValue) throws IOException {
        // obtain the corresponding character from the table file
        // print to the script output text file
        String encoding = getEncoding(charValue);
        if (encoding == null) {
            String format = "ERROR - character value 0x%04X @ 0x%06X-%d ($%06X-%d) not in table file!\n";
            throw new IOException(String.format(format, charValue, currByteOffset, bitOffset, getCPUOffsetVar(), bitOffset));
        }
        scriptOutput.write(encoding);
    }

    private static void printArg(int charEncoding) throws IOException {
        // print the two bytes of the encoding individually
        int hiByte = charEncoding >> 8;
        int lowByte = charEncoding & 0xFF;

        String format = "<$%02X><$%02X>";
        scriptOutput.write(String.format(format, hiByte, lowByte));
    }

    private static void printPtr(int ptrValue) throws IOException {
        // used this for debugging, but may hinder readability in script dump
        // String format1 = "// PTR -> [$%06X-%d]";
        // scriptOutput.newLine();
        // scriptOutput.write(String.format(format1, ptrValue >> 3, ptrValue & 0x7));

        String format2 = "#EMBSET(%04d)";
        scriptOutput.newLine();
        scriptOutput.write(String.format(format2, currPtrNum++));
    }

    private static void printROMFilePos() throws IOException {
        int cpuOffset = getCPUOffsetVar();

        // calculate convenient data for playtesting: you can alter the three
        // script start points located at $02A627 - $02A62F to immediately jump
        // anywhere you want in the script on a new file or by selecting "restart"
        // on continue file prompt; this is the data to put there (repeat 3 times)
        int startPoint = (cpuOffset << 3) | bitOffset;
        int startLow = startPoint & 0xFF;
        int startMid = (startPoint >> 8) & 0xFF;
        int startHi  = (startPoint >> 16) & 0xFF;

        String format = "//[$%06X-%d] -> [%02X %02X %02X]";
        scriptOutput.write(String.format(format, cpuOffset, bitOffset & 0x7, startLow, startMid, startHi));
        scriptOutput.newLine();
    }

    private static int getFileOffset(int cpuOffset) {
        int bankNum = cpuOffset >> 16;
        int bankOffset = cpuOffset & 0xFFFF;
        return 0x8000 * (bankNum - 1) + bankOffset;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static final int START_POINT_LOCATION = 0x12627;
    private static final int NUM_START_POINTS = 3;

    private static ArrayList<HuffScriptPointer> scriptPointers;
    private static int currPtrNum;
    private static final int NUM_POINTERS = 2500;

    private static void getScriptStartPoints() throws IOException {
        scriptPointers = new ArrayList<>(NUM_POINTERS);
        romFile.seek(START_POINT_LOCATION);
        for (int i = 0; i < NUM_START_POINTS; i++) {
            int ptrLocation = getCPUOffsetFP();
            int ptr = readStartPoint();

            int ptrBitOffset = ptr & 0x7;
            int ptrCPUAddr = ptr >> 3;
            // the first start point should be to the beginning of the script
            if (i == 0) {
                scriptStartBankNumber = (ptrCPUAddr >> 16) & 0xFF;
                scriptStartBankOffset = (ptrCPUAddr) & 0xFFFF;
            }

            scriptPointers.add(new HuffScriptPointer(ptrCPUAddr, ptrBitOffset, ptrLocation, bitOffset, i));
        }
    }

    // not all the pointers in the script necessarily point to right after a
    // CLEAR code, JMP code, or CHOICE code; this collects all of them
    private static void getScriptPointers(int scriptStart, int scriptEnd) throws IOException {
        romFile.seek(scriptStart);
        currByteOffset = scriptStart;

        // it is important to PEEK at the byte here instead of READ it, to not
        // advance the file pointer for pointer locations and "start of page"
        // print outs in the script dump
        huffmanBuffer = peekByte();

        // indicate start of script with bit offset of "0", but do not update
        // Huffman buffer from having an actual bit offset of 0
        bitOffset = 8;

        // go to start of script and start reading characters
        System.out.print("Now getting pointers from script...");
        while (romFile.getFilePointer() < scriptEnd) {
            // pointers in the script can only appear directly after certain
            // control codes, as in not after actual characters
            int charEncoding = readCharacter();
            if (charEncoding >= LINE_00) {
                // choices are special cases that the regular algorithm doesn't cover
                if (charEncoding == CHOICE_19 || charEncoding == CHOICE_1A) {
                    // both choice codes use three args and a variable number of
                    // pointers, which is based on the first arg
                    int arg0 = readCharacter();
                    readCharacter();
                    readCharacter();

                    int numPtrs = (arg0 & 0x7) - 1;
                    for (int i = 0; i < numPtrs; i++) {
                        int ptrLocation = getCPUOffsetVar();
                        int ptr = readPointer();

                        int ptrCPUAddr = ptr >> 3;
                        int ptrBitOffset = ptr & 0x7;
                        scriptPointers.add(new HuffScriptPointer(ptrCPUAddr, ptrBitOffset, ptrLocation, bitOffset, scriptPointers.size()));
                    }
                }

                // otherwise, get # of arguments and interpret each one properly
                else {
                    int codeNum = charEncoding & 0xFF;
                    int argCount = numArgs[codeNum];
                    int argType  = argTypes[codeNum];

                    for (int i = 0; i < argCount; i++) {
                        switch (argType & 0x1) {
                            case CHAR_ARG:
                                readCharacter();
                                break;
                            case PTR_ARG:
                                int ptrLocation = getCPUOffsetVar();
                                int ptr = readPointer();

                                int ptrCPUAddr = ptr >> 3;
                                int ptrBitOffset = ptr & 0x7;
                                scriptPointers.add(new HuffScriptPointer(ptrCPUAddr, ptrBitOffset, ptrLocation, bitOffset, scriptPointers.size()));
                                break;
                        }
                        argType >>= 1;
                    }
                }
            }
            // do nothing if not a control code
        }
        System.out.println(" Done");
    }

    private static void outputScriptPointers() throws IOException {
        BufferedWriter pointerListOutput = new BufferedWriter(new FileWriter("script/analysis/script pointers list.txt"));
        String format = "Ptr #%4d @ $%06X-%d -> $%06X-%d";
        for (HuffScriptPointer ptr : scriptPointers) {
            pointerListOutput.write(String.format(format, ptr.getPtrNum(), ptr.getPtrLocation(), ptr.getPtrLocationBitOffset(), ptr.getPtrValue(), ptr.getPtrValueBitOffset()));
            pointerListOutput.newLine();
        }
        pointerListOutput.flush();
        pointerListOutput.close();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static void addAtlasHeader(String tableFilename) throws IOException {
        scriptOutput.write("#VAR(Table, TABLE)");
        scriptOutput.newLine();

        // use whatever table file was included when running this tool
        String tableReferenceFormat = "#ADDTBL(\"%s\", Table)";
        scriptOutput.write(String.format(tableReferenceFormat, tableFilename));
        scriptOutput.newLine();

        scriptOutput.write("#ACTIVETBL(Table)");
        scriptOutput.newLine();

        scriptOutput.write("#SMA(\"LOROM00\")");
        scriptOutput.newLine();

        scriptOutput.write("#EMBTYPE(\"LOROM00\", 24, $0)");
        scriptOutput.newLine();
        scriptOutput.newLine();

        String commentLine = "// -----------------------------------------------------------------------------";
        scriptOutput.write(commentLine);
        scriptOutput.newLine();

        scriptOutput.write(commentLine);
        scriptOutput.newLine();
        scriptOutput.newLine();

        scriptOutput.write("// Expand ROM from 1 MB to 2 MB");
        scriptOutput.newLine();

        scriptOutput.write("#JMP($1FFFFF)");
        scriptOutput.newLine();

        scriptOutput.write("<$00>");
        scriptOutput.newLine();
        scriptOutput.newLine();

        scriptOutput.write("// Update the THREE start points");
        scriptOutput.newLine();

        scriptOutput.write("#JMP($12627)");
        scriptOutput.newLine();

        scriptOutput.write("#EMBSET(0000)");
        scriptOutput.newLine();

        scriptOutput.write("#EMBSET(0001)");
        scriptOutput.newLine();

        scriptOutput.write("#EMBSET(0002)");
        scriptOutput.newLine();
        scriptOutput.newLine();

        scriptOutput.write(commentLine);
        scriptOutput.newLine();

        scriptOutput.write(commentLine);
        scriptOutput.newLine();
        scriptOutput.newLine();

        scriptOutput.write("// Reinsert script in second MB of ROM");
        scriptOutput.newLine();

        scriptOutput.write("#JMP($100000)");
        // scriptOutput.newLine();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static boolean currPointerMatches(int listPos, HuffScriptPointer currHuffPtr) throws IOException {
        boolean inArrayBounds = listPos < scriptPointers.size();
        boolean matchOffsets = getFileOffset(currHuffPtr.getPtrValue()) == currByteOffset;
        boolean matchBitOffsets = currHuffPtr.getPtrValueBitOffset() == (bitOffset & 0x7);

        return inArrayBounds && matchOffsets && matchBitOffsets;
    }
    
    private static boolean isCharEncodingText(int charEncoding) {
        if (charEncoding < LINE_00) {
            return true;
        }
        boolean isText = false;
        switch (charEncoding) {
            case LINE_00:
            case END_CHOICE_1C:
            case NAME_SAN_20:
            case NAME_21:
                isText = true;
                break;
        }
        return isText;
    }

    private static void dumpScript(int scriptStart, int scriptEnd) throws IOException {
        // go to start of script and print the starting position to output script file
        romFile.seek(scriptStart);
        currByteOffset = scriptStart;

        // indicate start of script with bit offset of "0", but do not update
        // Huffman buffer from having an actual bit offset of 0
        bitOffset = 8;

        // printROMFilePos();
        huffmanBuffer = peekByte();

        // keep track of what number to use for the EMBSETs (direct)
        currPtrNum = NUM_START_POINTS;

        // keep track of what number to use for the EMBWRITEs (indirect, use as
        // index into script pointer list, and get that pointer's assigned number)
        int currPosInScriptPtrList = 0;

        // use this to write current CPU offset after finding an EMBWRITE, but
        // only if we hadn't just written it after a control code where we do
        boolean justWrotePointerComment = false;

        int prevCharEncoding = 0xFFFF;
        boolean printedFirstCharForLine = false;
        boolean onNewLine = true;

        // go to start of script and start reading characters
        while (romFile.getFilePointer() < scriptEnd && currPosInScriptPtrList < scriptPointers.size()) {
            // check if at a target for an embedded pointer, i.e. an EMBWRITE
            HuffScriptPointer currHuffPtr = scriptPointers.get(currPosInScriptPtrList);
            // if (shouldWriteDetails) {
                // goryDetails.write("\nChecking: " + currHuffPtr.toString() + "\n");
            // }

            while (currPointerMatches(currPosInScriptPtrList, currHuffPtr)) {
                // write current position in ROM, but only do it as many times as
                // necessary, which is to say not like:
                // [text] [ptr] EMBWRITE(n) [ptr] EMBWRITE(x) [text]
                if (shouldWriteDetails) {
                    goryDetails.write("Match! " + currHuffPtr.toString() + "\n");
                }
                if (!justWrotePointerComment) {
                    scriptOutput.newLine();
                    // avoid double line breaks after this control code
                    if (prevCharEncoding != END_CHOICE_1C) {
                        scriptOutput.newLine();
                    }
                    printROMFilePos();
                    justWrotePointerComment = true;
                }
                String format = "#EMBWRITE(%04d)";
                scriptOutput.write(String.format(format, currHuffPtr.getPtrNum()));
                scriptOutput.newLine();

                currPosInScriptPtrList++;
                if (currPosInScriptPtrList < scriptPointers.size()) {
                    currHuffPtr = scriptPointers.get(currPosInScriptPtrList);
                    if (shouldWriteDetails) {
                        goryDetails.write("\nChecking: " + currHuffPtr.toString() + "\n");
                    }
                }
            }

            // get a character and print it to the text file
            // also do a line break before it if a line in script begins with a
            // bunch of non-text control codes
            int charEncoding = readCharacter();
            boolean isText = isCharEncodingText(charEncoding);
            if (isText && onNewLine && !printedFirstCharForLine) {
                if (!justWrotePointerComment && !isCharEncodingText(prevCharEncoding)) {
                    scriptOutput.newLine();
                }
                printedFirstCharForLine = true;
                onNewLine = false;
            }
            printChar(charEncoding);

            // if got a control code, have to print its arguments as raw bytes
            // and/or print any pointers
            if (charEncoding >= LINE_00) {
                // choices are special cases that the regular algorithm doesn't cover
                if (charEncoding == CHOICE_19 || charEncoding == CHOICE_1A) {
                    // both choice codes use three args and a variable number of
                    // pointers, which is based on the first arg
                    int arg0 = readCharacter();
                    int arg1 = readCharacter();
                    int arg2 = readCharacter();

                    printArg(arg0);
                    printArg(arg1);
                    printArg(arg2);

                    int numPtrs = (arg0 & 0x7) - 1;
                    for (int i = 0; i < numPtrs; i++) {
                        int ptr = readPointer();
                        printPtr(ptr);
                    }

                    if ((bitOffset & 0x7) == 0) {
                        if (shouldWriteDetails) {
                            goryDetails.write("Bit aligned ptr(s) special case!" + "\n");
                        }
                        romFile.readUnsignedByte();
                        huffmanBuffer = peekByte();
                        // currByteOffset++;
                        bitOffset = 8;
                    }
                }

                // get # of arguments and interpret each one properly
                else {
                    int codeNum = charEncoding & 0xFF;
                    int argCount = numArgs[codeNum];
                    int argType  = argTypes[codeNum];

                    for (int i = 0; i < argCount; i++) {
                        switch (argType & 0x1) {
                            case CHAR_ARG:
                                int arg = readCharacter();
                                printArg(arg);
                                break;
                            case PTR_ARG:
                                int ptr = readPointer();
                                printPtr(ptr);
                                if ((bitOffset & 0x7) == 0) {
                                    if (shouldWriteDetails) {
                                        goryDetails.write("Bit aligned ptr(s) special case!" + "\n");
                                    }
                                    romFile.readUnsignedByte();
                                    huffmanBuffer = peekByte();
                                    bitOffset = 8;
                                }
                                break;
                        }
                        argType >>= 1;
                    }
                }

                // do special stuff for printing to output script
                switch (charEncoding) {
                    // add line breaks in dump after these control codes
                    case LINE_00:
                    // case CREDITS_09:
                    // case CREDITS_0C:
                    case END_CHOICE_1C:
                    // case SET_FLAG_22:
                        scriptOutput.newLine();

                        justWrotePointerComment = false;
                        onNewLine = true;
                        printedFirstCharForLine = false;
                        break;

                    // print CPU offset for script after these control codes
                    case JMP_03:
                    case JMP_CC_04:
                    case CHOICE_19:
                    case CHOICE_1A:
                    case CLEAR_25:
                    case CLEAR_27:
                        scriptOutput.newLine();
                        scriptOutput.newLine();
                        printROMFilePos();

                        justWrotePointerComment = true;
                        onNewLine = true;
                        printedFirstCharForLine = false;
                        break;

                    // add special script position indicating credits
                    case NOP_1D:
                        scriptOutput.newLine();
                        scriptOutput.newLine();
                        scriptOutput.write("// --------------------");
                        scriptOutput.newLine();
                        String format = "[$%06X-%d]";
                        int cpuOffset = getCPUOffsetVar();
                        scriptOutput.write(String.format("// ROLL CREDITS @ " + format, cpuOffset, bitOffset));
                        scriptOutput.newLine();
                        scriptOutput.newLine();
                        justWrotePointerComment = false;
                        onNewLine = true;
                        printedFirstCharForLine = false;

                        // indicate progress in command line output
                        System.out.println(String.format("Got to ending at " + format, cpuOffset, bitOffset));
                        goryDetails.write(String.format("Got to ending at " + format + "\n", cpuOffset, bitOffset));
                        break;

                    // setting this typically doesn't matter, but it is possible
                    // (see $1A88E5-1 or $1CD8AE-5) for text to go like:
                    // [CLEAR_FADE_OUT][SOME_CTRL_CODE][EMBEDDED_PTR]
                    default:
                        justWrotePointerComment = false;
                        // onNewLine = false;
                        // printedFirstCharForLine = false;
                        break;
                }
            }
            else {
                // set script status flags when a text character
                justWrotePointerComment = false;
                onNewLine = false;
                printedFirstCharForLine = true;
            }
            prevCharEncoding = charEncoding;
        }

        scriptOutput.write("// END OF SCRIPT");
        romFile.close();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public static void main(String args[]) throws IOException {
        if (args.length != 5) {
            System.out.println("Sample usage: java HuffScriptDumper rom_name table_file output_file script_start script_end");
            return;
        }

        String romFilename = args[0];
        String tableFilename = args[1];
        // for an unmodified JP ROM, script goes from AE401 to F95A7
        int scriptStart = Integer.parseInt(args[3], 16);
        int scriptEnd = Integer.parseInt(args[4], 16);

        if (scriptStart >= scriptEnd) {
            System.out.println("Error - script start point must be before script end point");
            return;
        }

        romFile = new RandomAccessFile(romFilename, "r");
        // scriptOutput = new BufferedWriter(new FileWriter("script/script dump.txt"));
        scriptOutput = new BufferedWriter(new FileWriter(args[2]));
        goryDetails = new BufferedWriter(new FileWriter("script/analysis/detailed output.txt"));

        readTableFile(tableFilename);
        getCtrlCodeArgTable();
        getHuffmanTreeData();

        shouldWriteDetails = false;
        getScriptStartPoints();
        getScriptPointers(scriptStart, scriptEnd);
        // sort script pointers by their values i.e. where they point to (EMBWRITEs)
        // they are assigned numbers based on where to write them (EMBSETs)
        Collections.sort(scriptPointers);
        outputScriptPointers();

        addAtlasHeader(tableFilename);
        // shouldWriteDetails = true;
        dumpScript(scriptStart, scriptEnd);

        romFile.close();
        scriptOutput.flush();
        scriptOutput.close();
        goryDetails.flush();
        goryDetails.close();
    }
}
