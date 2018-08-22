import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class ZipCodeRangeCondenser {

    private static boolean debug = false;

    // program entry point
    public static void main (String[] args) {

        ZipCodeRangeCondenser condenser = new ZipCodeRangeCondenser();
        String argument = args[0];

        // run tests if -test argument is passed in
        if ( argument.equals("-test")) {
            condenser.runUnitTests();

        } else {
            // read in the file and merge the zip code ranges
            String filename = argument;
            log("filename=" + filename);
            ArrayList<int[]>  inputRanges = condenser.readRangesFromFile(filename);
            ArrayList<int[]> outputRanges = condenser.mergeInputRanges(inputRanges);
            condenser.printResults(outputRanges);
        }
    }

    public ArrayList<int[]> readRangesFromFile(String filename) {

        // read lines from the file, process strings into integer pair ranges, then merge ranges
        Path file = FileSystems.getDefault().getPath(filename);
        Charset charset = Charset.forName("US-ASCII");
        ArrayList<int[]> inputRanges = new ArrayList<int[]>();
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {

            // read in the file, one line at a time
            String line = null;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                log(line);
                int[] range = getBracketedRange(line, lineNumber);
                if ( range != null ) {
                    log("Line " + lineNumber + ": " + range[0] + ":" + range[1]);
                    inputRanges.add(range);
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputRanges;
    }

    public ArrayList<int[]> mergeInputRanges(ArrayList<int[]> inputRanges) {

        // merge input ranges and place into output ranges
        ArrayList<int[]> outputRanges = new ArrayList<int[]>();

        // exit if there are no input ranges specified
        if ( inputRanges.size() == 0 ) {
            System.out.println("No valid input ranges");
            return outputRanges;
        }

        // add the first input range to the output range set
        outputRanges.add(inputRanges.get(0));

        // loop through the rest of the input ranges
        for ( int i = 1; i < inputRanges.size(); i++) {
            log("i=" + i);
            int[] inputRange = (int[])inputRanges.get(i);
            int inputMin = inputRange[0];
            int inputMax = inputRange[1];
            ArrayList<Integer> intersectionIndices = new ArrayList<Integer>();
            // compare current input range to each of the current output ranges to determine whether they intersect
            for ( int j = 0; j < outputRanges.size(); j++) {
                log("j=" + j);
                int[] outputRange = (int[])outputRanges.get(j);
                int outputMin = outputRange[0];
                int outputMax = outputRange[1];
                // determine whether ranges intersect
                if ( inputMin > outputMax || inputMax < outputMin ) {
                    log( inputMin + ":" + inputMax + " and " + outputMin + ":" + outputMax + " do not intersect");
                } else {
                    log( inputMin + ":" + inputMax + " and " + outputMin + ":" + outputMax + " intersect");
                    // add the index of the output range to the list that will be merged after visiting each output range
                    intersectionIndices.add(j);
                }
            }

            // if any of the previous output ranges intersect with the input range, merge them all together
            if (intersectionIndices.size() > 0) {
                int min = inputMin;
                int max = inputMax;
                int minIndex = Integer.MAX_VALUE;
                log_no_newline("Merging " + inputMin + ":" + inputMax + " with ");
                for ( int k = 0; k < intersectionIndices.size(); k++) {
                    int index = (int)intersectionIndices.get(k);
                    if ( index < minIndex ) minIndex = index;
                    int[] outputRange = (int[])outputRanges.get(index);
                    if ( outputRange[0] < min ) min = outputRange[0];
                    if ( outputRange[1] > max ) max = outputRange[1];
                    log_no_newline( outputRange[0] + ":" + outputRange[1] + ", ");
                }
                log(); // add the newline after the composite line is printed

                // remove all output ranges that were merged
                for ( int k = intersectionIndices.size() - 1; k >= 0; k--) {
                    int index = (int)intersectionIndices.get(k);
                    outputRanges.remove(index);
                }

                // add the new merged range to output ranges
                int[] mergedRange = new int[2];
                mergedRange[0] = min;
                mergedRange[1] = max;
                log("Merged to: " + mergedRange[0] + ":" + mergedRange[1] );
                outputRanges.add(minIndex, mergedRange);

            } else {
                // if no output ranges intersect, just add the new input range
                outputRanges.add(inputRange);
                log("Added new output range: " + inputMin + ":" + inputMax );
            }

            // if debug is turned on, print the current set of output ranges
            log("Output ranges:");
            for ( int k = 0; k < outputRanges.size(); k++) {
                int[] outputRange = (int[])outputRanges.get(k);
                log( "  " + outputRange[0] + ":" + outputRange[1] );
            }

        }

        // sort the output ranges for legibility
        Collections.sort(outputRanges, new RangeSorter());

        return outputRanges;
    }

    public void printResults(ArrayList<int[]> outputRanges) {

        // print the results
        if ( outputRanges == null ) {
            System.out.println("No output ranges were created.");
        } else {
            for ( int i = 0; i < outputRanges.size(); i++) {
                int[] outputRange = (int[])outputRanges.get(i);
                System.out.println( "[" + String.format("%05d", outputRange[0]) +
                                    "," + String.format("%05d", outputRange[1]) + "]" );
            }
        }
    }

    // convert a line of text into an integer pair range, return null if there is a problem with the line
    public static int[] getBracketedRange(String input, int lineNumber) {

        int[] range = new int[2];
        int open  = input.indexOf("[");
        int comma = input.indexOf(",");
        int close = input.indexOf("]");

        // validate [xxxxx,yyyyy] format
        if (open == -1 || comma == -1 || close == -1) {
            return null;
        }
        if ( comma < open || close < comma || close < open ) {
            return null;
        }

        // retrieve the two zip codes
        String first = input.substring(open + 1, comma).trim();
        String second = input.substring(comma + 1, close).trim();

        try {
            range[0] = Integer.parseInt(first);
            range[1] = Integer.parseInt(second);

            // validate the range values
            if ( range[0] < 0 || range[0] > 99999 ) {
                System.out.println( "Error in line " + lineNumber + ":");
                System.out.println( "  Invalid zip code: " + range[0]);
                System.out.println( "  Must be between 00000 and 99999");
                return null;
            }
            if ( range[1] < 0 || range[1] > 99999 ) {
                System.out.println( "Error in line " + lineNumber + ":");
                System.out.println( "  Invalid zip code: " + range[1]);
                System.out.println( "  Must be between 00000 and 99999");
                return null;
            }
            if ( range[0] > range[1]) {
                System.out.println( "Error in line " + lineNumber + ":");
                System.out.println( "  Invalid range: " + input);
                System.out.println( "  First must be less than or equal to second");
                return null;

            }
        } catch ( NumberFormatException e ) {
            System.out.println( "Error in line " + lineNumber + ":");
            System.out.println( "  Non numerical entry: " + input);
            return null;
        }

        return range;
    }

    public void runUnitTests() {
        String testFilename = "testfile.txt";

        // verify that zero length input returns zero length output
        System.out.println("Running test 1");
        writeFile(testFilename, "");
        ArrayList<int[]> inputRanges  = readRangesFromFile(testFilename);
        ArrayList<int[]> outputRanges = mergeInputRanges(inputRanges);
        if ( ! ( outputRanges.size() == 0 ) ) {
            System.out.println("Failed test 1");
        }

        // positive test
        System.out.println("Running test 2");
        writeFile(testFilename, "[94133,94133]\n[94200,94299]\n[94600,94699]");
        inputRanges  = readRangesFromFile(testFilename);
        outputRanges = mergeInputRanges(inputRanges);
        if ( ! ( outputRanges.size() == 3 ) )
            System.out.println("Failed test 2a");
        if ( ! ( Arrays.equals((int[])outputRanges.get(0),
                               new int[] {94133, 94133} ) ) )
            System.out.println("Failed test 2b");
        if ( ! ( Arrays.equals((int[])outputRanges.get(1),
                               new int[] {94200, 94299} ) ) )
            System.out.println("Failed test 2c");
        if ( ! ( Arrays.equals((int[])outputRanges.get(2),
                               new int[] {94600, 94699} ) ) )
            System.out.println("Failed test 2d");

        // positive test
        System.out.println("Running test 3");
        writeFile(testFilename, "[94133,94133]\n[94200,94299]\n[94226,94399]");
        inputRanges  = readRangesFromFile(testFilename);
        outputRanges = mergeInputRanges(inputRanges);
        if ( ! ( outputRanges.size() == 2 ) )
            System.out.println("Failed test 3a");
        if ( ! ( Arrays.equals((int[])outputRanges.get(0),
                               new int[] {94133, 94133} ) ) )
            System.out.println("Failed test 3b");
        if ( ! ( Arrays.equals((int[])outputRanges.get(1),
                               new int[] {94200, 94399} ) ) )
            System.out.println("Failed test 3c");

        // negative test, should not accept the aaaaa entry
        System.out.println("Running test 4");
        writeFile(testFilename, "[aaaaa,94133]\n[94200,94299]\n[94226,94399]");
        inputRanges  = readRangesFromFile(testFilename);
        if ( ! ( inputRanges.size() == 2 ) )
            System.out.println("Failed test 4a");
        if ( ! ( Arrays.equals((int[])inputRanges.get(0),
                               new int[] {94200, 94133} ) ) )
            System.out.println("Failed test 4b");
        if ( ! ( Arrays.equals((int[])inputRanges.get(1),
                               new int[] {94226, 94399} ) ) )
            System.out.println("Failed test 4c");

        // negative test, should not accept the -11111 entry
        System.out.println("Running test 5");
        writeFile(testFilename, "[-11111,94133]\n[94200,94299]\n[94226,94399]");
        inputRanges  = readRangesFromFile(testFilename);
        if ( ! ( inputRanges.size() == 2 ) )
            System.out.println("Failed test 5a");
        if ( ! ( Arrays.equals((int[])inputRanges.get(0),
                               new int[] {94200, 94133} ) ) )
            System.out.println("Failed test 5b");
        if ( ! ( Arrays.equals((int[])inputRanges.get(1),
                               new int[] {94226, 94399} ) ) )
            System.out.println("Failed test 5c");

        try {
            Path file = FileSystems.getDefault().getPath(testFilename);
            Files.delete(file);
        } catch ( Exception e ) {
            System.out.println("Failed to delete file: " + testFilename );
        }
    }

    private void writeFile(String filename, String content) {
        Path file = FileSystems.getDefault().getPath(filename);
        Charset charset = Charset.forName("US-ASCII");
        try (BufferedWriter writer = Files.newBufferedWriter(file, charset)) {
            writer.write(content, 0, content.length());
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    private class RangeSorter implements Comparator<int[]> {
        public int compare(int[] a, int[] b) {
            return a[0] - b[0];
        }
    }
    public static void log( String s ) {
        if ( debug )
            System.out.println(s);
    }
    public static void log() {
        if ( debug )
            System.out.println();
    }
    public static void log_no_newline( String s ) {
        if ( debug )
            System.out.print(s);
    }


}
