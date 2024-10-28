@prompt $g
javac src\huffman\DumpHuffmanScript.java src\huffman\HuffScriptPointer.java src\header_files\HelperMethods.java
java -classpath .\src huffman/DumpHuffmanScript "rom\Otogirisou (Japan).sfc" "tables\otogirisou jp table.tbl" "script\DUMP - JP script auto commented.txt" ae401 f95a7 --auto-comment
