
// code adapted from scan.cpp from Guest

package huffman;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;
import java.io.BufferedReader;

import static header_files.HelperMethods.*;

public class DumpHuffmanCodesFromROM {

    // *************************************************************************
    // Options for how to output to text files
    // *************************************************************************

    // print the data either CSV-style (false) or "pretty-print" style (true)
    private static boolean PRETTY_PRINT = false;

    // print the Huffman code data sorted by table value (true) or sorted mostly
    // by Huffman code length (false)
    private static boolean SORT_BY_TBL_VALUE = true;

    // *************************************************************************
    // Data structure for describing general Huffman nodes
    // *************************************************************************

    private static class HuffmanNode {
        // data for this node's L/R subtrees
        short l_data;
        short r_data;

        // ROM offsets for this node's L/R subtrees; find L/R data here
        long leftHuffOffset;
        long rightHuffOffset;

        // Huffman codes (i.e. tree traversal paths) to this node's L/R subtrees
        // use String here because Huffman codes are variable-length and can
        // start with a 0; numerical types would disregard any starting 0s
        String leftHuffCode; 
        String rightHuffCode;
    }
    private static ArrayList<HuffmanNode> treeData;

    // *************************************************************************
    // Data structure for describing specifically the Huffman leaf nodes
    // *************************************************************************

    private static class HuffmanLeaf implements Comparable<HuffmanLeaf>{
        short data;
        long huffOffset;
        String huffCode;
        String encoding;

        private HuffmanLeaf(short data, long huffOffset, String huffCode) {
            this.data = (short) (data & 0x1FFF);
            this.huffOffset = huffOffset;
            this.huffCode = huffCode;

            String encoding = tableFileMap.get(this.data);
            if (encoding == null) {
                encoding = String.format("[$%04X]", this.data);
            }
            this.encoding = encoding;
        }

        // by default, compare leaves by their data values ("table file sort")
        public int compareTo(HuffmanLeaf other) {
            return data - other.data;
        }

        public String toString() {
            String format = PRETTY_PRINT ? "Huff code for %4X '%s'\tis %2d bits @ 0x%5X: %s\n"
                                        : "%4X,%s,%2d,0x%5X,%s\n";
            return String.format(format, data, encoding, huffCode.length(), huffOffset, huffCode);
        }
    }
    private static ArrayList<HuffmanLeaf> huffLeaves;

    // *************************************************************************
    // Code for reading in the contents of the table file
    // *************************************************************************

    // Given a table file encoding value, get the corresponding text encoding.
    private static HashMap<Short, String> tableFileMap;

    private static void readTableFile(String tableFilename) throws IOException {
        BufferedReader tableFileStream = new BufferedReader(new FileReader(tableFilename));

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

            short value = Short.parseShort(split[0], 16);
            String encoding = "";
            // possible for a table entry to be for the equal sign "="
            if (split.length == 1) {
                encoding = EQUALS;
            }
            else {
                encoding = split[1];
            }
            tableFileMap.put(value, encoding);
        }

        tableFileStream.close();
    }

    // *************************************************************************
    // Code for reading in the raw Huffman tree data, and outputting it to text
    // *************************************************************************

    private static RandomAccessFile romStream;

    private static int getNumHuffmanEntries() throws IOException {
        // get number of entries in the Huffman tree
        romStream.seek(PTR_TO_NUM_HUFFMAN_ENTRIES);
        int sizeOfHuffmanTree = romStream.readUnsignedByte();
        sizeOfHuffmanTree |= romStream.readUnsignedByte() << 8;

        int numHuffEntries = (sizeOfHuffmanTree >> 1) + 1;
        return numHuffEntries;
    }

    private static void getTreeData(String romFilename, int numHuffEntries) throws IOException {
        // get bank offset of ptr to left Huffman trees; convert to ROM offset
        romStream.seek(PTR_TO_HUFF_LEFT_OFFSET);
        int leftTreeOffset = romStream.readUnsignedByte();
        leftTreeOffset |= romStream.readUnsignedByte() << 8;
        leftTreeOffset |= 0x2 << 16; // assume in bank 02
        leftTreeOffset = getFileOffset(leftTreeOffset);

        // same for the right Huffman trees
        romStream.seek(PTR_TO_HUFF_RIGHT_OFFSET);
        int rightTreeOffset = romStream.readUnsignedByte();
        rightTreeOffset |= romStream.readUnsignedByte() << 8;
        rightTreeOffset |= 0x2 << 16;
        rightTreeOffset = getFileOffset(rightTreeOffset);

        // start reading data for the left subtrees for the Huffman tree
        romStream.seek(leftTreeOffset);
        for (int i = 0; i < numHuffEntries; i++) {
            treeData.add(new HuffmanNode());
            HuffmanNode node = treeData.get(i);

            // data is stored in little-endian order; convert to big-endian
            node.leftHuffOffset = romStream.getFilePointer();
            node.l_data = (short) romStream.readUnsignedByte();
            node.l_data |= (romStream.readUnsignedByte() << 8);

            node.leftHuffCode = "";
        }

        romStream.seek(rightTreeOffset);
        for (int i = 0; i < numHuffEntries; i++) {
            HuffmanNode node = treeData.get(i);

            node.rightHuffOffset = romStream.getFilePointer();
            node.r_data = (short) romStream.readUnsignedByte();
            node.r_data |= (romStream.readUnsignedByte() << 8);

            node.rightHuffCode = "";
        }
    }

    // describes the raw Huffman tree data with a standard format on every line
    // prints: tree index number, left/right tree values and ROM offsets
    private static String treeDataLineOutput(int index, HuffmanNode node) {
        String format = PRETTY_PRINT ? "tree @ %03X: %4X @ 0x%5X, %4X @ 0x%5X\n"
                                     : "%03X,%4X,0x%5X,%4X,0x%5X\n";
        return String.format(format, index, node.l_data, node.leftHuffOffset, node.r_data, node.rightHuffOffset);
    }

    // outputs all the lines as described above to a text file
    private static void treeDataFileOutput(String treeOutputFilename) throws IOException {
        FileWriter treeOutputWriter = new FileWriter(treeOutputFilename);

        for (int i = 0; i < HUFF_TABLE_ENTRIES; i++) {
            HuffmanNode node = treeData.get(i);
            treeOutputWriter.write(treeDataLineOutput(i, node));
            treeOutputWriter.flush();
        }
        treeOutputWriter.close();
    }

    // *************************************************************************
    // Code for traversing the Huffman tree and outputting the codes to text
    // *************************************************************************

    private static void getHuffLeaves() {
        // get all the Huffman leaves by doing a depth-first traversal
        Stack<HuffmanNode> stack = new Stack<HuffmanNode>();
        HuffmanNode huffmanRoot = treeData.get(treeData.size() - 1);
        stack.push(huffmanRoot);

        while (!stack.isEmpty()) {
            HuffmanNode node = stack.pop();

            // Java has no "unsigned short" type, so weird comparison:
            // 0x0000 thru 0x8000 = 0 thru 32768;   inner node, push
            // 0x8001 thru 0xFFFF = -32767 thru -1; leaf node, done
            // examine left subtree, push onto stack if data value is < 0x8000
            String leftHuff = node.leftHuffCode + LEFT_BIT;
            if (node.l_data >= 0) {
                HuffmanNode leftNode = treeData.get(node.l_data);
                leftNode.leftHuffCode = leftHuff;
                leftNode.rightHuffCode = leftHuff;
                stack.push(leftNode);
            }
            // if not, this is a leaf node, so create a HuffmanLeaf object
            else {
                node.leftHuffCode = leftHuff;
                HuffmanLeaf leaf = new HuffmanLeaf(node.l_data, node.leftHuffOffset, node.leftHuffCode);
                huffLeaves.add(leaf);
            }

            // same process but with the right subtree
            String rightHuff = node.rightHuffCode + RIGHT_BIT;
            if (node.r_data >= 0) {
                HuffmanNode rightNode = treeData.get(node.r_data);
                rightNode.leftHuffCode = rightHuff;
                rightNode.rightHuffCode = rightHuff;
                stack.push(rightNode);
            }
            else {
                node.rightHuffCode = rightHuff;
                HuffmanLeaf leaf = new HuffmanLeaf(node.r_data, node.rightHuffOffset, node.rightHuffCode);
                huffLeaves.add(leaf);
            }
        }
    }

    private static void huffCodesFileOutput(String huffCodesOutput) throws IOException {
        FileWriter huffCodeWriter = new FileWriter(huffCodesOutput);

        if (SORT_BY_TBL_VALUE) {
            Collections.sort(huffLeaves);
        }
        for (HuffmanLeaf leaf : huffLeaves) {
            huffCodeWriter.write(leaf.toString());
        }
        huffCodeWriter.flush();
        huffCodeWriter.close();
    }

    // *************************************************************************
    // *************************************************************************

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Sample usage: java DumpHuffmanCodesFromROM romFile tableFile treeRawDataOutput huffCodesOutput");
            return;
        }
        String romFilename = args[0];
        String tableFilename = args[1];
        String treeRawDataOutput = args[2];
        String huffCodesOutput = args[3];

        tableFileMap = new HashMap<>();
        readTableFile(tableFilename);

        romStream = new RandomAccessFile(romFilename, "r");
        int numHuffEntries = getNumHuffmanEntries();
        treeData = new ArrayList<>(numHuffEntries);
        huffLeaves = new ArrayList<>(numHuffEntries);
        getTreeData(romFilename, numHuffEntries);
        romStream.close();

        getHuffLeaves();

        treeDataFileOutput(treeRawDataOutput);
        huffCodesFileOutput(huffCodesOutput);
    }
}
