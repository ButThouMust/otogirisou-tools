@prompt $g
javac src\huffman\DumpHuffmanCodesFromROM.java src\header_files\HelperMethods.java
java -classpath .\src huffman/DumpHuffmanCodesFromROM "rom\Otogirisou (Japan).sfc" "tables\otogirisou jp table.tbl" "tables\raw JP huffman tree data.txt" "tables\JP huffman code dump.txt"
