package com.pointblue.ldifutil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

/**
 * The `LDIFAttr2DirAttrCompare` class compares an attribute from an LDIF file with the
 * corresponding attribute in a live LDAP directory. Trusts all TLS certificates on
 * `ldaps://` URLs (see DummyTrustManager).
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
        // baseDN (args[3]) is accepted for CLI compatibility but not used; the full DN from
        // each LDIF entry is passed directly to ctx.search().
        String ldapUsername = args[4];
        String ldapPassword = args[5];

        Hashtable<String, String> env = new Hashtable<>();
        if (ldapUrl.startsWith("ldaps://")) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put("java.naming.ldap.factory.socket", "com.pointblue.ldifutil.JndiSocketFactory");
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
        env.put(Context.SECURITY_CREDENTIALS, ldapPassword);

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8)) {
                String line;
                String currentDnLine = "";
                boolean inRecord = false;
                boolean inDN = false;
                boolean inTargetValue = false;
                List<String> attributeValues = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("dn:")) {
                        if (!currentDnLine.isEmpty() && !attributeValues.isEmpty()) {
                            compareAndPrint(currentDnLine, attributeValues, attributeToExtract, ctx);
                        }
                        currentDnLine = line;
                        inRecord = true;
                        inDN = true;
                        inTargetValue = false;
                        attributeValues.clear();
                    } else if (line.isEmpty()) {
                        if (!currentDnLine.isEmpty() && !attributeValues.isEmpty()) {
                            compareAndPrint(currentDnLine, attributeValues, attributeToExtract, ctx);
                        }
                        inRecord = false;
                        inDN = false;
                        inTargetValue = false;
                        currentDnLine = "";
                        attributeValues.clear();
                    } else if (inRecord) {
                        if (line.startsWith(" ")) {
                            String continuation = line.substring(1);
                            if (inDN) {
                                currentDnLine += continuation;
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
                if (!currentDnLine.isEmpty() && !attributeValues.isEmpty()) {
                    compareAndPrint(currentDnLine, attributeValues, attributeToExtract, ctx);
                }
            }
        } catch (IOException | NamingException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ctx != null) {
                try { ctx.close(); } catch (NamingException ignored) { }
            }
        }
    }

    /** Returns the plain-text DN for a `dn:` or `dn::` (base64) line. */
    private static String extractDN(String dnLine) {
        if (dnLine.startsWith("dn::")) {
            String b64 = dnLine.substring(4).trim();
            return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
        }
        return dnLine.substring(3).trim();
    }

    /**
     * Compares the attribute values from the LDIF file with the corresponding attribute in the LDAP directory.
     *
     * @param dnLine The full `dn:` (or `dn::`) line, with continuation lines already unfolded.
     * @param ldifValues The attribute values from the LDIF file.
     * @param attributeName The name of the attribute to compare.
     * @param ctx The LDAP directory context.
     */
    private static void compareAndPrint(String dnLine, List<String> ldifValues, String attributeName, DirContext ctx) throws NamingException {
        String entryDN = extractDN(dnLine);
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.OBJECT_SCOPE);
        controls.setReturningAttributes(new String[]{attributeName});

        NamingEnumeration<SearchResult> results = null;
        try {
            results = ctx.search(entryDN, "(objectClass=*)", controls);
            System.out.println(dnLine + ":");
            if (results.hasMore()) {
                SearchResult result = results.next();
                Attributes attrs = result.getAttributes();
                Attribute attr = attrs.get(attributeName);

                if (attr != null) {
                    List<String> directoryValues = new ArrayList<>();
                    NamingEnumeration<?> values = attr.getAll();
                    try {
                        while (values.hasMore()) {
                            directoryValues.add(values.next().toString());
                        }
                    } finally {
                        try { values.close(); } catch (NamingException ignored) { }
                    }

                    for (String ldifValue : ldifValues) {
                        boolean matchFound = directoryValues.contains(ldifValue);
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
        } finally {
            if (results != null) {
                try { results.close(); } catch (NamingException ignored) { }
            }
        }
    }
}
