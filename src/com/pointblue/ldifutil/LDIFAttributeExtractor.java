package com.pointblue.ldifutil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * The `LDIFAttributeExtractor` class extracts specified attributes from an LDIF file.
 * It reads the input LDIF file, extracts the specified attribute values (including any
 * wrapped continuation lines that belong to them), and prints them along with their DNs.
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
            System.out.println("Usage: java com.pointblue.ldifutil.LDIFAttributeExtractor <input-file> <attribute-to-extract>");
            System.exit(1);
        }

        String inputFile = args[0];
        String attributeToExtract = args[1].toLowerCase();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8)) {
            String line;
            String currentDN = "";
            boolean inRecord = false;
            boolean inDN = false;
            boolean inTargetValue = false;
            List<String> attributeValues = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                        printDNAndValues(currentDN, attributeValues);
                    }
                    currentDN = line.trim();
                    inRecord = true;
                    inDN = true;
                    inTargetValue = false;
                    attributeValues.clear();
                } else if (line.isEmpty()) {
                    if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                        printDNAndValues(currentDN, attributeValues);
                    }
                    inRecord = false;
                    inDN = false;
                    inTargetValue = false;
                    currentDN = "";
                    attributeValues.clear();
                } else if (inRecord) {
                    if (line.startsWith(" ")) {
                        // Per RFC 2849, unfolding consumes the newline and the single leading space.
                        String continuation = line.substring(1);
                        if (inDN) {
                            currentDN += continuation;
                        } else if (inTargetValue && !attributeValues.isEmpty()) {
                            int lastIdx = attributeValues.size() - 1;
                            attributeValues.set(lastIdx, attributeValues.get(lastIdx) + continuation);
                        }
                    } else {
                        inDN = false;
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String attributeName = parts[0].trim().toLowerCase();
                            if (attributeName.equals(attributeToExtract)) {
                                inTargetValue = true;
                                attributeValues.add(parts[1].trim());
                            } else {
                                inTargetValue = false;
                            }
                        } else {
                            inTargetValue = false;
                        }
                    }
                }
            }
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
