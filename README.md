# williams-sonoma

This code was built using Java 8. It will read in a specified file, which should contain one zip code range per line, with the format like [00000,11111]. It will print the merged ranges to standard out. A few basic tests are included to verify functionality.

## Compiling:
javac ZipCodeRangeCondenser.java

## Running the code:
java ZipCodeRangeCondenser -i zip_ranges.txt

The input filename can be whatever you like. The file zip_ranges.txt is included with some sample ranges.
You can pass the -d flag to show verbose debugging output.

## Running the tests:
java ZipCodeRangeCondenser -test
