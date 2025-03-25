package com.pointblue.ldifutil;

import java.io.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

/**
 * The `LDIFAttr2DirAttrCompare` class compares an attribute from an LDIF file with the corresponding attribute in an LDAP directory.
 * It reads the LDIF file, extracts the specified attribute, and compares it with the attribute in the LDAP directory.
 */
public class LDIFAttr2DirAttrCompare {

    /**
     * The main method to execute the comparison.
     *
     * @param args Command line arguments. Expects 6 arguments:
     *             <input-file> <attribute-to-extract> <ldap-url> <base-dn> <ldap-username> <ldap-password>
     */
    public static void main(String[] args) {
        if (args.length != 6) {
            System.out.println("Usage: java com.pointblue.ldifutil.LDIFAttr2DirAttrCompare <input-file> <attribute-to-extract> <ldap-url> <base-dn> <ldap-username> <ldap-password>");
            System.exit(1);
        }

        String inputFile = args[0];
        String attributeToExtract = args[1].toLowerCase();
        String ldapUrl = args[2];
        String baseDN = args[3];
        String ldapUsername = args[4];
        String ldapPassword = args[5];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            boolean inRecord = false;
            String currentDN = "";
            boolean inMultiLineValue = false;
            List<String> attributeValues = new ArrayList<>();

            Hashtable<String, String> env = new Hashtable<>();
            if(ldapUrl.startsWith("ldaps://")) {
                env.put(javax.naming.Context.SECURITY_PROTOCOL, "ssl");
                env.put("java.naming.ldap.factory.socket",
                        "com.pointblue.ldifutil.JndiSocketFactory");
            }
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
            env.put(Context.SECURITY_CREDENTIALS, ldapPassword);

            DirContext ctx = new InitialDirContext(env);

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("dn:")) {
                    if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                        compareAndPrint(currentDN, attributeValues, attributeToExtract, ctx, baseDN);
                    }
                    currentDN = line.trim();
                    if(!currentDN.endsWith(baseDN)){
                        line = reader.readLine();
                        currentDN += line.trim();
                    }
                    inRecord = true;
                    inMultiLineValue = false;
                    attributeValues.clear();
                } else if (line.isEmpty()) {
                    if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                        compareAndPrint(currentDN, attributeValues, attributeToExtract, ctx, baseDN);
                    }
                    inRecord = false;
                    inMultiLineValue = false;
                } else if (inRecord) {
                    if (line.startsWith(" ")) {
                        if (inMultiLineValue && !attributeValues.isEmpty()) {
                            String lastValue = attributeValues.remove(attributeValues.size() - 1);
                            attributeValues.add(lastValue + "\n" + line.trim());
                        }
                    } else {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String attributeName = parts[0].trim().toLowerCase();
                            if (attributeName.equals(attributeToExtract)) {
                                inMultiLineValue = parts[1].trim().isEmpty();
                                attributeValues.add(parts[1].trim());
                            } else {
                                inMultiLineValue = false;
                            }
                        }
                    }
                }
            }
            if (!currentDN.isEmpty() && !attributeValues.isEmpty()) {
                compareAndPrint(currentDN, attributeValues, attributeToExtract, ctx, baseDN);
            }
            ctx.close();
        } catch (IOException | NamingException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Compares the attribute values from the LDIF file with the corresponding attribute in the LDAP directory.
     *
     * @param dn The distinguished name (DN) of the entry.
     * @param ldifValues The attribute values from the LDIF file.
     * @param attributeName The name of the attribute to compare.
     * @param ctx The LDAP directory context.
     * @param baseDN The base DN for the LDAP search.
     * @throws NamingException If an error occurs while accessing the LDAP directory.
     */
    private static void compareAndPrint(String dn, List<String> ldifValues, String attributeName, DirContext ctx, String baseDN) throws NamingException {
        String entryDN = dn.split(":", 2)[1].trim();
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.OBJECT_SCOPE);
        controls.setReturningAttributes(new String[]{attributeName});

        NamingEnumeration<?> results = ctx.search(entryDN, "(objectClass=*)", controls);
        if (results.hasMore()) {
            SearchResult result = (SearchResult) results.next();
            Attributes attrs = result.getAttributes();
            Attribute attr = attrs.get(attributeName);
            System.out.println(dn + ":");

            if (attr != null) {
                List<String> directoryValues = new ArrayList<>();
                NamingEnumeration<?> values = attr.getAll();
                while (values.hasMore()) {
                    directoryValues.add(values.next().toString());
                }

                for (String ldifValue : ldifValues) {
                    boolean matchFound = false;
                    for (String dirValue : directoryValues) {
                        if (ldifValue.equals(dirValue)) {
                            matchFound = true;
                            break;
                        }
                    }
                    System.out.println("  - LDIF: " + ldifValue + " - Match in directory: " + (matchFound ? "Yes" : "No"));
                }

                for (String dirValue : directoryValues) {
                    if (!ldifValues.contains(dirValue)) {
                        System.out.println("  - Directory only: " + dirValue);
                    }
                }
            } else {
                System.out.println("  - Attribute not found in directory.");
            }
        } else {
            System.out.println("  - Entry not found in directory.");
        }
    }
}