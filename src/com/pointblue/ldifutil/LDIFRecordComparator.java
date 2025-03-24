package com.pointblue.ldifutil;

import java.io.*;
import java.util.*;

/**
 * The `LDIFRecordComparator` class compares records from two LDIF files.
 * It reads the input LDIF files, parses the records, and compares them to identify differences.
 */
public class LDIFRecordComparator {

    /**
     * The main method to execute the LDIF record comparison.
     *
     * @param args Command line arguments. Expects 2 arguments:
     *             <ldif-file1> <ldif-file2>
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java com.pointblue.ldifutil.LDIFRecordComparator <ldif-file1> <ldif-file2>");
            System.exit(1);
        }

        String ldifFile1 = args[0];
        String ldifFile2 = args[1];

        Map<String, Map<String, List<String>>> records1 = parseLDIF(ldifFile1);
        Map<String, Map<String, List<String>>> records2 = parseLDIF(ldifFile2);

        compareRecords(records1, records2);
    }

    /**
     * Parses an LDIF file and returns a map of records.
     *
     * @param fileName The name of the LDIF file to parse.
     * @return A map where the key is the DN and the value is a map of attributes and their values.
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
     * @param record The current record.
     * @param attribute The attribute name.
     * @param value The attribute value.
     */
    private static void addValueToRecord(Map<String, List<String>> record, String attribute, StringBuilder value) {
        if (attribute.isEmpty() || value.length() == 0) return;
        record.computeIfAbsent(attribute, k -> new ArrayList<>()).add(value.toString());
    }

    /**
     * Compares two sets of LDIF records and prints the differences.
     *
     * @param records1 The first set of LDIF records.
     * @param records2 The second set of LDIF records.
     */
    private static void compareRecords(Map<String, Map<String, List<String>>> records1, Map<String, Map<String, List<String>>> records2) {
        Set<String> allDNs = new HashSet<>(records1.keySet());
        allDNs.addAll(records2.keySet());

        for (String dn : allDNs) {
            Map<String, List<String>> record1 = records1.get(dn);
            Map<String, List<String>> record2 = records2.get(dn);

            if (record1 == null || record2 == null || !areRecordsEqual(record1, record2)) {
                System.out.println(dn);
            }
        }
    }

    /**
     * Checks if two records are equal.
     *
     * @param record1 The first record.
     * @param record2 The second record.
     * @return `true` if the records are equal, `false` otherwise.
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