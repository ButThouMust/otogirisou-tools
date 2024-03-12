import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GraphicsStructureList {
    // private GraphicsStructure[] structureList;
    private String folder;
    private int numStructs;
    private int gfxID;
    private int ptrToStructListSize;
    private int ptrToASMCode;
    private int ptrToAutoSfxIdList;
    private String description;
    private RandomAccessFile romStream;

    public GraphicsStructureList(String folder, int gfxID, int ptrToStructListSize, int ptrToASMCode, int ptrToAutoSfxIdList, String description) throws IOException {
        this.folder = folder;
        this.gfxID = gfxID;
        this.ptrToStructListSize = ptrToStructListSize;
        this.ptrToASMCode = ptrToASMCode;
        this.ptrToAutoSfxIdList = ptrToAutoSfxIdList;
        this.description = description;
        initializeList();
    }

    private void initializeList() throws IOException {
        if (!HelperMethods.isValidRomOffset(ptrToStructListSize)) {
            String format = "Invalid RAM address - $%06X does not map to ROM";
            throw new IOException(String.format(format, ptrToStructListSize));
        }
        
        romStream = new RandomAccessFile("rom/Otogirisou (Japan).sfc", "r");
        romStream.seek(HelperMethods.getFileOffset(ptrToStructListSize));

        numStructs = romStream.readUnsignedByte();
        // structureList = new GraphicsStructure[size];
    }

    public void dumpData() throws IOException {
        // I wouldn't bother with the GFX ID decimal values if Windows properly
        // sorted folders with names prefixed with hex values
        String outputFolderFormat = "%s/%03d GFX ID 0x%02X @ $%06X -- %s";
        String outputFolder = String.format(outputFolderFormat, folder, gfxID, gfxID, ptrToStructListSize, description);
        Files.createDirectories(Paths.get(outputFolder));

        String logNameFormat = outputFolder + "/! 0x%02X - $%06X LIST log.txt";
        String logName = String.format(logNameFormat, gfxID, ptrToStructListSize);
        BufferedWriter logFile = new BufferedWriter(new FileWriter(logName));

        logFile.write(String.format("Graphics ID 0x%02X dumper log\n", gfxID));
        logFile.write(String.format("Description: %s\n", description));
        logFile.write(String.format("ASM code @ $%06X; ", ptrToASMCode));

        long firstStructPtr = romStream.getFilePointer();
        printAutoSFXList(logFile);
        romStream.seek(firstStructPtr);

        logFile.write(String.format("List @ $%06X has %d structures", ptrToStructListSize, numStructs));
        // all of the 9 IDs for the credits use the exact same data
        // it's redundant to decompress and dump all the same data 9 times over
        if (gfxID >= 0x5E && gfxID <= 0x65) {
            logFile.write("\nPlease see the log file for graphics ID 0x5D.");
        }
        else {
            for (int i = 0; i < numStructs; i++) {
                int structRAMOffset = HelperMethods.getRAMOffset((int) romStream.getFilePointer());
                // System.out.println("Structure @ $" + Integer.toHexString(structRAMOffset));
                GraphicsStructure struct = new GraphicsStructure(structRAMOffset);
                struct.setOutputFolder(outputFolder);
                struct.dumpData();
                romStream.seek(romStream.getFilePointer() + HelperMethods.PTR_SIZE + struct.getNumBytesToSkip());

                logFile.newLine();
                logFile.newLine();
                struct.printSummaryToLog(logFile);
            }
        }

        romStream.close();
        logFile.close();
    }

    private void printAutoSFXList(BufferedWriter logFile) throws IOException {
        romStream.seek(HelperMethods.getFileOffset(ptrToAutoSfxIdList));
        int numSFXIDs = romStream.readUnsignedByte();

        String bytesList = "";
        for (int i = 0; i < numSFXIDs; i++) {
            int sfxID = romStream.readUnsignedByte();
            bytesList += String.format("%02X", sfxID);
            if (i < numSFXIDs - 1) {
                bytesList += " ";
            }
        }

        logFile.write(String.format("%02d automatic SFX IDs @ $%06X: [%s]\n\n", numSFXIDs, ptrToAutoSfxIdList, bytesList));
    }
}
