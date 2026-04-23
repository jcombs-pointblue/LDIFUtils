package com.pointblue.ldifutil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * The `LDIFAttributeComparator` class provides functionality to compare attributes between two LDIF files.
 * It reads the LDIF files, parses the records, and compares the specified attribute or all attributes if none is specified.
 * Multi-valued attributes are compared as unordered sets (LDAP attribute values have no inherent order).
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
     * Parses an LDIF file and returns a map of records keyed by the (decoded) DN.
     * Handles folded DN and attribute lines per RFC 2849 and base64-encoded `dn::` entries.
     */
    private static Map<String, Map<String, List<String>>> parseLDIF(String fileName) {
        Map<String, Map<String, List<String>>> records = new HashMap<>();
        Map<String, List<String>> currentRecord = null;
        StringBuilder currentDN = new StringBuilder();
        String currentAttribute = "";
        StringBuilder currentValue = new StringBuilder();
        boolean inDN = false;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    if (currentDN.length() > 0) {
                        addValueToRecord(currentRecord, currentAttribute, currentValue);
                        records.put(decodeDN(currentDN.toString()), currentRecord);
                    }
                    currentDN.setLength(0);
                    currentDN.append(line);
                    currentRecord = new HashMap<>();
                    currentAttribute = "";
                    currentValue = new StringBuilder();
                    inDN = true;
                } else if (line.isEmpty()) {
                    if (currentDN.length() > 0) {
                        addValueToRecord(currentRecord, currentAttribute, currentValue);
                        records.put(decodeDN(currentDN.toString()), currentRecord);
                        currentDN.setLength(0);
                        currentRecord = null;
                        currentAttribute = "";
                        currentValue = new StringBuilder();
                    }
                    inDN = false;
                } else if (currentRecord != null) {
                    if (line.startsWith(" ")) {
                        String continuation = line.substring(1);
                        if (inDN) {
                            currentDN.append(continuation);
                        } else {
                            currentValue.append(continuation);
                        }
                    } else {
                        inDN = false;
                        addValueToRecord(currentRecord, currentAttribute, currentValue);
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            currentAttribute = parts[0].trim().toLowerCase();
                            currentValue = new StringBuilder(parts[1].trim());
                        } else {
                            currentAttribute = "";
                            currentValue = new StringBuilder();
                        }
                    }
                }
            }
            if (currentDN.length() > 0) {
                addValueToRecord(currentRecord, currentAttribute, currentValue);
                records.put(decodeDN(currentDN.toString()), currentRecord);
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }

    private static String decodeDN(String dnLine) {
        if (dnLine.startsWith("dn::")) {
            return new String(Base64.getDecoder().decode(dnLine.substring(4).trim()), StandardCharsets.UTF_8);
        }
        return dnLine.substring(3).trim();
    }

    private static void addValueToRecord(Map<String, List<String>> record, String attribute, StringBuilder value) {
        if (attribute.isEmpty() || value.length() == 0) return;
        record.computeIfAbsent(attribute, k -> new ArrayList<>()).add(value.toString());
    }

    private static void compareRecords(Map<String, Map<String, List<String>>> records1, Map<String, Map<String, List<String>>> records2, String attributeToCompare) {
        Set<String> allDNs = new HashSet<>(records1.keySet());
        allDNs.addAll(records2.keySet());

        for (String dn : allDNs) {
            Map<String, List<String>> record1 = records1.get(dn);
            Map<String, List<String>> record2 = records2.get(dn);

            if (attributeToCompare == null) {
                if (record1 == null || record2 == null || !areRecordsEqual(record1, record2)) {
                    System.out.println(dn);
                }
            } else {
                List<String> values1 = record1 != null ? record1.get(attributeToCompare) : null;
                List<String> values2 = record2 != null ? record2.get(attributeToCompare) : null;
                if (values1 == null && values2 == null) {
                    continue; // attribute absent in both — not a difference
                }
                if (values1 == null || values2 == null || !valuesEqual(values1, values2)) {
                    System.out.println(dn);
                }
            }
        }
    }

    private static boolean areRecordsEqual(Map<String, List<String>> record1, Map<String, List<String>> record2) {
        if (record1.size() != record2.size()) return false;
        for (Map.Entry<String, List<String>> entry : record1.entrySet()) {
            List<String> values2 = record2.get(entry.getKey());
            if (values2 == null || !valuesEqual(entry.getValue(), values2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean valuesEqual(List<String> a, List<String> b) {
        return new HashSet<>(a).equals(new HashSet<>(b));
    }
}
