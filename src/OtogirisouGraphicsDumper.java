import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

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

    private static void dumpDataForGfxID(int gfxID) throws IOException {
        int asmCodePtr    = listOfASMCodePtrs[gfxID];
        int structListPtr = listOfStructListPtrs[gfxID];
        int sfxIdListPtr  = listOfSfxIdListPtrs[gfxID];
        String description = gfxDescriptions[gfxID];

        GraphicsStructureList structList = new GraphicsStructureList(outputFolder, gfxID, structListPtr, asmCodePtr, sfxIdListPtr, description);
        structList.dumpData();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public static void main(String args[]) throws IOException {
        listOfASMCodePtrs    = getListOfASMCodePtrs();
        listOfStructListPtrs = getListOfStructListPtrs();
        listOfSfxIdListPtrs  = getListOfSfxIdListPtrs();
        gfxDescriptions = getGfxIDDescriptions();

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
        }
    }
    
}
