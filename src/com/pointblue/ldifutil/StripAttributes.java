package com.pointblue.ldifutil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The `StripAttributes` class processes an LDIF file to remove specified attributes.
 * It reads the input LDIF file, removes the specified attributes (including any wrapped
 * continuation lines that belong to them), and writes the result to an output file.
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
            System.out.println("Usage: java com.pointblue.ldifutil.StripAttributes <input-file> <output-file> <attributes-to-remove>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];
        String[] attributesToRemove = args[2].split(",");

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), StandardCharsets.UTF_8)) {

            String line;
            boolean inRecord = false;
            String currentAttribute = "";

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    inRecord = true;
                    currentAttribute = "";
                    writer.write(line + System.lineSeparator());
                } else if (line.isEmpty()) {
                    inRecord = false;
                    currentAttribute = "";
                    writer.write(System.lineSeparator());
                } else if (inRecord) {
                    if (line.startsWith(" ")) {
                        // Continuation belongs to the current attribute (or to the DN, if
                        // currentAttribute is "" because we just saw the dn: line).
                        if (!shouldRemove(currentAttribute, attributesToRemove)) {
                            writer.write(line + System.lineSeparator());
                        }
                    } else {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            currentAttribute = parts[0].trim().toLowerCase();
                            if (!shouldRemove(currentAttribute, attributesToRemove)) {
                                writer.write(line + System.lineSeparator());
                            }
                        } else {
                            currentAttribute = "";
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
