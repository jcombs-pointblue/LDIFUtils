package com.pointblue.ldifutil;

import java.io.*;
import java.util.*;

/**
 * The `LDIFAttributeExtractor` class extracts specified attributes from an LDIF file.
 * It reads the input LDIF file, extracts the specified attribute values, and prints them along with their DNs.
 */
public class LDIFAttributeExtractor {

    /**
     * The main method to execute the attribute extraction.
     *
     * @param args Command line arguments. Expects 2 arguments:
     *             <input-file> <attribute-to-extract>
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java com.pointblue.ldifutil.LDIFAttr2DirAttrCompare <input-file> <attribute-to-extract>");
            System.exit(1);
        }

        String inputFile = args[0];
        String attributeToExtract = args[1].toLowerCase();  // Case-insensitive attribute name

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            boolean inRecord = false;
            String currentDN = "";
            boolean inMultiLineValue = false;
            List<String> attributeValues = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    // New record starts
                    if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                        printDNAndValues(currentDN, attributeValues);
                    }
                    currentDN = line.trim();
                    inRecord = true;
                    inMultiLineValue = false;
                    attributeValues.clear();
                } else if (line.isEmpty()) {
                    // End of record
                    if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                        printDNAndValues(currentDN, attributeValues);
                    }
                    inRecord = false;
                    inMultiLineValue = false;
                } else if (inRecord) {
                    if (line.startsWith(" ")) {
                        // Multi-line value continuation
                        if (inMultiLineValue && !attributeValues.isEmpty()) {
                            String lastValue = attributeValues.remove(attributeValues.size() - 1);
                            attributeValues.add(lastValue + "\n" + line.trim());
                        }
                    } else {
                        // New attribute line
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String attributeName = parts[0].trim().toLowerCase();
                            if (attributeName.equals(attributeToExtract)) {
                                inMultiLineValue = parts[1].trim().isEmpty();
                                attributeValues.add(parts[1].trim());
                            } else {
                                inMultiLineValue = false;
                            }
                        } else {
                            // Malformed line, ignore or handle as needed
                        }
                    }
                }
            }
            // Handle the last record if not ended with an empty line
            if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                printDNAndValues(currentDN, attributeValues);
            }
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prints the DN and the extracted attribute values.
     *
     * @param dn The distinguished name (DN) of the record.
     * @param values The list of extracted attribute values.
     */
    private static void printDNAndValues(String dn, List<String> values) {
        System.out.println(dn + ":");
        for (String value : values) {
            System.out.println("  - " + value);
        }
    }
}