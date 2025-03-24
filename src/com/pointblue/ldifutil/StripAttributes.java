package com.pointblue.ldifutil;

import java.io.*;

/**
 * The `StripAttributes` class processes an LDIF file to remove specified attributes.
 * It reads the input LDIF file, removes the specified attributes, and writes the result to an output file.
 */
public class StripAttributes {

    /**
     * The main method to execute the attribute stripping.
     *
     * @param args Command line arguments. Expects 3 arguments:
     *             <input-file> <output-file> <attributes-to-remove>
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java StripAttributes <input-file> <output-file> <attributes-to-remove>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];
        String[] attributesToRemove = args[2].split(",");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            boolean inRecord = false;
            boolean inMultiLineValue = false;
            String currentAttribute = "";

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    inRecord = true;
                    inMultiLineValue = false;
                    currentAttribute = "";
                    writer.write(line + System.lineSeparator());
                } else if (line.isEmpty()) {
                    inRecord = false;
                    inMultiLineValue = false;
                    currentAttribute = "";
                    writer.write(System.lineSeparator());
                } else if (inRecord) {
                    if (line.startsWith(" ")) {
                        // This line is a continuation of a multi-line attribute
                        if (!inMultiLineValue || !shouldRemove(currentAttribute, attributesToRemove)) {
                            writer.write(line + System.lineSeparator());
                        }
                    } else {
                        // New attribute line
                        inMultiLineValue = false;
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            currentAttribute = parts[0].trim().toLowerCase();
                            inMultiLineValue = parts[1].trim().isEmpty();  // If value is empty, next line might be continuation
                            if (!shouldRemove(currentAttribute, attributesToRemove)) {
                                writer.write(line + System.lineSeparator());
                            }
                        } else {
                            // Malformed line, write it as is
                            writer.write(line + System.lineSeparator());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("An error occurred while reading or writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determines if the given attribute should be removed.
     *
     * @param attribute The attribute to check.
     * @param attributesToRemove The list of attributes to remove.
     * @return `true` if the attribute should be removed, `false` otherwise.
     */
    private static boolean shouldRemove(String attribute, String[] attributesToRemove) {
        for (String attr : attributesToRemove) {
            if (attribute.equals(attr.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}