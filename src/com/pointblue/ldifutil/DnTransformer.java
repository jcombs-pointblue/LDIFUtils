package com.pointblue.ldifutil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

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

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), StandardCharsets.UTF_8)) {

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
        // Support both "dn: <value>" and "dn:: <base64>" forms; preserve the form on output.
        boolean isBase64 = dnLine.startsWith("dn::");
        String dnValue = isBase64
                ? new String(Base64.getDecoder().decode(dnLine.substring(4).trim()), StandardCharsets.UTF_8)
                : dnLine.substring(3).trim();

        int commaIndex = dnValue.indexOf(',');
        if (commaIndex == -1) {
            // No comma found, this is a top-level entry, just return the original line
            return dnLine;
        }

        String leftmostComponent = dnValue.substring(0, commaIndex);
        String transformedDn = leftmostComponent + "," + newDnPath;

        return isBase64
                ? "dn:: " + Base64.getEncoder().encodeToString(transformedDn.getBytes(StandardCharsets.UTF_8))
                : "dn: " + transformedDn;
    }
}
