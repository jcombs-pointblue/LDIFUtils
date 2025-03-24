package com.pointblue.ldifutil;

import java.io.*;
import java.util.*;

/**
 * The `LDIFAttributeComparator` class provides functionality to compare attributes between two LDIF files.
 * It reads the LDIF files, parses the records, and compares the specified attribute or all attributes if none is specified.
 */
public class LDIFAttributeComparator {

    /**
     * The main method to execute the comparison.
     *
     * @param args Command line arguments. Expects 2 or 3 arguments:
     *             <ldif-file1> <ldif-file2> [<attribute-to-compare>]
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java com.pointblue.ldifutil.LDIFAttributeComparator <ldif-file1> <ldif-file2> [<attribute-to-compare>]");
            System.exit(1);
        }

        String ldifFile1 = args[0];
        String ldifFile2 = args[1];
        String attributeToCompare = args.length == 3 ? args[2].toLowerCase() : null;

        Map<String, Map<String, List<String>>> records1 = parseLDIF(ldifFile1);
        Map<String, Map<String, List<String>>> records2 = parseLDIF(ldifFile2);

        compareRecords(records1, records2, attributeToCompare);
    }

    /**
     * Parses an LDIF file and returns a map of records.
     *
     * @param fileName The name of the LDIF file to parse.
     * @return A map where the key is the DN and the value is another map of attributes and their values.
     */
    private static Map<String, Map<String, List<String>>> parseLDIF(String fileName) {
        Map<String, Map<String, List<String>>> records = new HashMap<>();
        Map<String, List<String>> currentRecord = null;
        String currentDN = "";
        String currentAttribute = "";
        StringBuilder currentValue = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    if (!currentDN.isEmpty()) {
                        addValueToRecord(currentRecord, currentAttribute, currentValue);
                        records.put(currentDN, currentRecord);
                    }
                    currentDN = line.split(":", 2)[1].trim();
                    currentRecord = new HashMap<>();
                    currentAttribute = "";
                    currentValue = new StringBuilder();
                } else if (line.isEmpty()) {
                    if (!currentDN.isEmpty()) {
                        addValueToRecord(currentRecord, currentAttribute, currentValue);
                        records.put(currentDN, currentRecord);
                        currentDN = "";
                        currentRecord = null;
                        currentAttribute = "";
                        currentValue = new StringBuilder();
                    }
                } else if (currentRecord != null) {
                    if (line.startsWith(" ")) {
                        // Multi-line attribute continuation
                        currentValue.append("\n").append(line.trim());
                    } else {
                        addValueToRecord(currentRecord, currentAttribute, currentValue);
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            currentAttribute = parts[0].trim().toLowerCase();
                            currentValue = new StringBuilder(parts[1].trim());
                        } else {
                            // Handle malformed lines if necessary
                            currentAttribute = "";
                            currentValue = new StringBuilder();
                        }
                    }
                }
            }
            // Handle the last record if the file does not end with a newline
            if (!currentDN.isEmpty()) {
                addValueToRecord(currentRecord, currentAttribute, currentValue);
                records.put(currentDN, currentRecord);
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }

    /**
     * Adds a value to the current record.
     *
     * @param record The current record map.
     * @param attribute The attribute name.
     * @param value The attribute value.
     */
    private static void addValueToRecord(Map<String, List<String>> record, String attribute, StringBuilder value) {
        if (attribute.isEmpty() || value.length() == 0) return;
        record.computeIfAbsent(attribute, k -> new ArrayList<>()).add(value.toString());
    }

    /**
     * Compares the records from two LDIF files.
     *
     * @param records1 The records from the first LDIF file.
     * @param records2 The records from the second LDIF file.
     * @param attributeToCompare The attribute to compare, or null to compare all attributes.
     */
    private static void compareRecords(Map<String, Map<String, List<String>>> records1, Map<String, Map<String, List<String>>> records2, String attributeToCompare) {
        Set<String> allDNs = new HashSet<>(records1.keySet());
        allDNs.addAll(records2.keySet());

        for (String dn : allDNs) {
            Map<String, List<String>> record1 = records1.get(dn);
            Map<String, List<String>> record2 = records2.get(dn);

            if (attributeToCompare == null) {
                // Compare all attributes
                if (record1 == null || record2 == null || !areRecordsEqual(record1, record2)) {
                    System.out.println(dn);
                }
            } else {
                // Compare only the specified attribute
                List<String> values1 = record1 != null ? record1.get(attributeToCompare) : null;
                List<String> values2 = record2 != null ? record2.get(attributeToCompare) : null;

                if (values1 == null || values2 == null || !values1.equals(values2)) {
                    System.out.println(dn);
                }
            }
        }
    }

    /**
     * Checks if two records are equal.
     *
     * @param record1 The first record.
     * @param record2 The second record.
     * @return True if the records are equal, false otherwise.
     */
    private static boolean areRecordsEqual(Map<String, List<String>> record1, Map<String, List<String>> record2) {
        if (record1.size() != record2.size()) return false;

        for (Map.Entry<String, List<String>> entry : record1.entrySet()) {
            List<String> values1 = entry.getValue();
            List<String> values2 = record2.get(entry.getKey());

            if (values2 == null || !values1.equals(values2)) {
                return false;
            }
        }
        return true;
    }
}