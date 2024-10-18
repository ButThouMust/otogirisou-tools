@prompt $g

javac src\dump_graphics\*.java src\header_files\HelperMethods.java
java -classpath .\src dump_graphics/ChunsoftPresentsDumper
:: javac src\*Graphics*.java src\HelperMethods.java
java -classpath .\src dump_graphics/OtogirisouGraphicsDumper
