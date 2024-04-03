import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class OtogirisouGraphicsDumper {

    private static final int NUM_STRUCT_LIST_PTRS = 0x9F;
    private static final int PTR_TO_LIST_OF_ASM_CODE_PTRS = 0x0ED69;
    private static final int PTR_TO_LIST_OF_STRUCT_LIST_PTRS = 0x0EEA7;
    private static final int PTR_TO_LIST_OF_SFX_ID_LIST_PTRS = 0x0EFE5;

    private static String outputFolder;

    private static int listOfASMCodePtrs[];
    private static int listOfStructListPtrs[];
    private static int listOfSfxIdListPtrs[];
    private static String gfxDescriptions[];

    private static HashSet<GraphicsStructure> uniqueGfxStructures;

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static int[] getListOfGfxDataPtrs(int listPtr) throws IOException {
        int structListPtrs[] = new int[NUM_STRUCT_LIST_PTRS];
        RandomAccessFile romFile = new RandomAccessFile("rom/Otogirisou (Japan).sfc", "r");
        romFile.seek(listPtr);
        for (int i = 0; i < structListPtrs.length; i++) {
            int loromPtr = romFile.readUnsignedByte();
            loromPtr |= (romFile.readUnsignedByte() << 8);
            structListPtrs[i] = loromPtr;
        }

        romFile.close();
        return structListPtrs;
    }

    private static int[] getListOfASMCodePtrs() throws IOException {
        return getListOfGfxDataPtrs(PTR_TO_LIST_OF_ASM_CODE_PTRS);
    }

    private static int[] getListOfStructListPtrs() throws IOException {
        return getListOfGfxDataPtrs(PTR_TO_LIST_OF_STRUCT_LIST_PTRS);
    }

    private static int[] getListOfSfxIdListPtrs() throws IOException {
        return getListOfGfxDataPtrs(PTR_TO_LIST_OF_SFX_ID_LIST_PTRS);
    }

    private static String[] getGfxIDDescriptions() throws IOException {
        BufferedReader gfxIDFile = new BufferedReader(new FileReader("graphics/gfx id descriptions.txt"));
        String gfxIDDescriptions[] = new String[NUM_STRUCT_LIST_PTRS];

        String line = "";
        for (int i = 0; i < gfxIDDescriptions.length; i++) {
            line = gfxIDFile.readLine();
            String split[] = line.split("\t");
            int idNum = Integer.parseInt(split[0], 16);
            String description = split[1];
            gfxIDDescriptions[idNum] = description;
        }
        gfxIDFile.close();
        return gfxIDDescriptions;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static HashMap<Integer, GraphicsStructure.StructureType> getIndepStructsList() {
        HashMap<Integer, GraphicsStructure.StructureType> indepStructsList = new HashMap<>();
        GraphicsStructure.StructureType tile    = GraphicsStructure.StructureType.TILE;
        GraphicsStructure.StructureType tilemap = GraphicsStructure.StructureType.TILEMAP;
        GraphicsStructure.StructureType palette = GraphicsStructure.StructureType.PALETTE;

        indepStructsList.put(0x03B910, tile);
        indepStructsList.put(0x03B982, palette);

        indepStructsList.put(0x079653, tile);
        indepStructsList.put(0x07997E, tilemap);

        indepStructsList.put(0x07C7FE, tile);
        indepStructsList.put(0x07C84F, palette);

        indepStructsList.put(0x07DCF3, palette);
        indepStructsList.put(0x07DD1F, palette);

        indepStructsList.put(0x07F38B, palette);
        indepStructsList.put(0x07F431, palette);
        indepStructsList.put(0x07F4D7, palette);

        indepStructsList.put(0x0889E5, tilemap);

        return indepStructsList;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static void dumpDataForGfxID(int gfxID) throws IOException {
        int asmCodePtr    = listOfASMCodePtrs[gfxID];
        int structListPtr = listOfStructListPtrs[gfxID];
        int sfxIdListPtr  = listOfSfxIdListPtrs[gfxID];
        String description = gfxDescriptions[gfxID];

        GraphicsStructureList structList = new GraphicsStructureList(outputFolder, gfxID, structListPtr, asmCodePtr, sfxIdListPtr, description);
        structList.dumpData();

        uniqueGfxStructures.addAll(structList.getGfxStructHashSet());
    }

    private static void dumpDataForIndepStruct(int dataPtr, GraphicsStructure.StructureType type) throws IOException {
        GraphicsStructure gfxStruct = new GraphicsStructure(dataPtr, type);
        gfxStruct.setOutputFolderIndepStruct(outputFolder);
        gfxStruct.dumpData();
    }

    private static void generateListOfUniqueStructs() throws IOException {
        BufferedWriter uniqueStructOutput = new BufferedWriter(new FileWriter(outputFolder + "\\! LIST of unique structs.txt"));

        // get a sorted list of all unique structures that have been processed
        ArrayList<GraphicsStructure> uniqueStructsArrayList = new ArrayList<>(uniqueGfxStructures);
        Collections.sort(uniqueStructsArrayList);

        int lastDataPtr = 0;
        int lastDataEndPtr = -1;
        final int GFX_BLOCK_START_ADDR = 0x2DA00;

        String format1 = "Struct @ $%4X: %-7s data from $%06X to $%06X\n";
        String format2 = "Note: found range not covered by structures: $%06X to $%06X\n";
        for (GraphicsStructure gfxStruct : uniqueStructsArrayList) {
            int dataPtr = gfxStruct.getDataPointer();
            int dataEndPtr = gfxStruct.getDataEndPointer();

            if (dataPtr != lastDataPtr) {
                // if there are any gaps in the ROM not covered by a structure
                // somewhere (except the one at $00E00C), point it out
                boolean consecutiveData = (dataPtr == lastDataEndPtr + 1);
                if (lastDataEndPtr > GFX_BLOCK_START_ADDR && !consecutiveData) {
                    String line2 = String.format(format2, lastDataEndPtr + 1, dataPtr - 1);
                    uniqueStructOutput.write(line2);
                }

                int structLocation = gfxStruct.getStructureLocation();
                String type = gfxStruct.getType().toString();

                // log the first structure that points to a specific block of data
                String line = String.format(format1, structLocation, type, dataPtr, dataEndPtr);
                uniqueStructOutput.write(line);
            }
            lastDataPtr = dataPtr;
            lastDataEndPtr = dataEndPtr;
        }

        uniqueStructOutput.flush();
        uniqueStructOutput.close();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public static void main(String args[]) {
        try {
            listOfASMCodePtrs    = getListOfASMCodePtrs();
            listOfStructListPtrs = getListOfStructListPtrs();
            listOfSfxIdListPtrs  = getListOfSfxIdListPtrs();
            gfxDescriptions = getGfxIDDescriptions();
            uniqueGfxStructures = new HashSet<>();

            outputFolder = "gfx dump";

            // dumping ALL the graphics data takes a while to run, so giving option
            // to only dump the graphics data necessary for the translation
            if (args.length != 0 && args[0].equals("-quick")) {
                // 0x5A-0x5C: title screen, file select screen, name entry screen
                // 0x5D:      credits tilesets, and tilemap for 1st credit
                // 0x66-0x74: rest of the credits tilemaps
                // 0x7B:      graphic for Nami's name
                // Add to this list as you see fit.
                outputFolder = outputFolder + " - quick";
                int gfxIDs[] = {0x5A, 0x5B, 0x5C, 0x5D, 0x66, 0x67, 0x68, 0x69, 0x6A,
                                0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73,
                                0x74, 0x7B};
                for (int gfxID : gfxIDs) {
                    dumpDataForGfxID(gfxID);
                }
            }
            else {
                outputFolder = outputFolder + " - full";
                for (int gfxID = 0; gfxID < NUM_STRUCT_LIST_PTRS; gfxID++) {
                    dumpDataForGfxID(gfxID);
                }
                generateListOfUniqueStructs();

                // also dump a handful of independent graphics structures
                // that are not covered by the structure lists
                HashMap<Integer, GraphicsStructure.StructureType> indepStructs = getIndepStructsList();
                for (Integer dataPtr : indepStructs.keySet()) {
                    dumpDataForIndepStruct(dataPtr, indepStructs.get(dataPtr));
                }
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
}
