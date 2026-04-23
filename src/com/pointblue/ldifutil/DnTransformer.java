package com.pointblue.ldifutil;

import java.io.*;

/**
 * The `DnTransformer` class processes an LDIF file and transforms the DN (Distinguished Name) paths.
 * For each entry, it copies the entry to a new file but replaces the DN path with one specified by a parameter.
 * The CN (or leaf-most) part of the name remains the same. This is a simple, direct, transformation
 * of the DN path while preserving the leftmost component of the DN. So, the LDIF should only include entries
 * from a single container.
 * 
 * Example: "cn=jcombs,ou=foo,o=bar" would become "cn=jcombs,ou=do,o=re"
 */
public class DnTransformer {

    /**
     * The main method to execute the DN transformation.
     *
     * @param args Command line arguments. Expects 3 arguments:
     *             <input-file> <output-file> <new-dn-path>
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java DnTransformer <input-file> <output-file> <new-dn-path>");
            System.out.println("Example: java DnTransformer input.ldif output.ldif \"ou=do,o=re\"");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];
        String newDnPath = args[2];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            StringBuilder currentDnLine = null;
            boolean inDnLine = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    // Start of a new DN line
                    if (inDnLine && currentDnLine != null) {
                        // Process the previous DN line if there was one
                        String transformedLine = transformDnLine(currentDnLine.toString(), newDnPath);
                        writer.write(transformedLine + System.lineSeparator());
                    }
                    // Initialize for the new DN line
                    currentDnLine = new StringBuilder(line);
                    inDnLine = true;
                } else if (inDnLine && line.startsWith(" ")) {
                    // This is a continuation of the DN line
                    currentDnLine.append(line.substring(1)); // Append without the leading space
                } else {
                    // Not a DN line or continuation
                    if (inDnLine && currentDnLine != null) {
                        // Process the previous DN line
                        String transformedLine = transformDnLine(currentDnLine.toString(), newDnPath);
                        writer.write(transformedLine + System.lineSeparator());
                        inDnLine = false;
                        currentDnLine = null;
                    }
                    // Write the current line as is
                    writer.write(line + System.lineSeparator());
                }
            }

            // Handle the case where the file ends with a DN line
            if (inDnLine && currentDnLine != null) {
                String transformedLine = transformDnLine(currentDnLine.toString(), newDnPath);
                writer.write(transformedLine + System.lineSeparator());
            }

            System.out.println("DN transformation completed successfully.");

        } catch (IOException e) {
            System.err.println("An error occurred while reading or writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Transforms a DN line by keeping the leftmost component and replacing the rest with the new path.
     *
     * @param dnLine The original DN line from the LDIF file.
     * @param newDnPath The new DN path to use.
     * @return The transformed DN line.
     */
    private static String transformDnLine(String dnLine, String newDnPath) {
        // Extract the DN value part (after "dn: ")
        String dnValue = dnLine.substring(4).trim();

        // Find the first comma which separates the leftmost component from the rest
        int commaIndex = dnValue.indexOf(',');

        if (commaIndex == -1) {
            // No comma found, this is a top-level entry, just return the original line
            return dnLine;
        }

        // Extract the leftmost component (e.g., "cn=jcombs")
        String leftmostComponent = dnValue.substring(0, commaIndex);

        // Combine the leftmost component with the new path
        String transformedDn = leftmostComponent + "," + newDnPath;

        // Return the complete transformed line
        return "dn: " + transformedDn;
    }
}
