# LDIF Utilities

A collection of Java utilities for processing, comparing, and manipulating LDIF (LDAP Data Interchange Format) files.

## Overview

This project provides several command-line utilities to help LDAP administrators and developers work with LDIF files. Each utility performs a specific function, from comparing LDIF records to extracting attributes and comparing with live directory data.

## Requirements

- Java 8 or higher
- JNDI libraries (included with Java)

## Utilities

### StripAttributes

Removes specified attributes from LDIF files.

```sh
java com.pointblue.ldifutil.StripAttributes <input-file> <output-file> <attributes-to-remove>
```

Example:
```sh
java com.pointblue.ldifutil.StripAttributes users.ldif filtered.ldif "userPassword,telephoneNumber,roomNumber"
```

### LDIFRecordComparator

Compares records between two LDIF files and outputs DNs of records that differ.

```sh
java com.pointblue.ldifutil.LDIFRecordComparator <ldif-file1> <ldif-file2>
```

Example:
```sh
java com.pointblue.ldifutil.LDIFRecordComparator original.ldif updated.ldif
```

### LDIFAttributeExtractor

Extracts specified attributes from an LDIF file and displays them with their DN.

```sh
java com.pointblue.ldifutil.LDIFAttributeExtractor <input-file> <attribute-to-extract>
```

Example:
```sh
java com.pointblue.ldifutil.LDIFAttributeExtractor users.ldif mail
```

### LDIFAttributeComparator

Compares specific attributes (or all attributes) between two LDIF files.

```sh
java com.pointblue.ldifutil.LDIFAttributeComparator <ldif-file1> <ldif-file2> [<attribute-to-compare>]
```

Example:
```sh
# Compare only the mail attribute
java com.pointblue.ldifutil.LDIFAttributeComparator file1.ldif file2.ldif mail

# Compare all attributes
java com.pointblue.ldifutil.LDIFAttributeComparator file1.ldif file2.ldif
```

### LDIFAttr2DirAttrCompare

Compares attributes from an LDIF file with corresponding attributes in a live LDAP directory. Trusts all TLS certificates, so it works against self-signed `ldaps://` endpoints.

```sh
java com.pointblue.ldifutil.LDIFAttr2DirAttrCompare <input-file> <attribute-to-extract> <ldap-url> <base-dn> <ldap-username> <ldap-password>
```

Example:
```sh
java com.pointblue.ldifutil.LDIFAttr2DirAttrCompare users.ldif mail ldap://ldap.example.com:389 dc=example,dc=com cn=admin,dc=example,dc=com secretpassword
```

### DnTransformer

Rewrites the DN path of every entry in an LDIF file while preserving the leftmost RDN (e.g. `cn=jcombs`). Useful for moving a container's contents to a new location. The input LDIF should contain entries from a single container, since every DN is rewritten with the same new path.

```sh
java com.pointblue.ldifutil.DnTransformer <input-file> <output-file> <new-dn-path>
```

Example:
```sh
# "cn=jcombs,ou=foo,o=bar" becomes "cn=jcombs,ou=do,o=re"
java com.pointblue.ldifutil.DnTransformer input.ldif output.ldif "ou=do,o=re"
```

## Building

Compile the source files using `javac`:

```sh
javac -d bin src/com/pointblue/ldifutil/*.java
```

Or build a runnable JAR:

```sh
javac -d bin src/com/pointblue/ldifutil/*.java
jar cf ldifutils.jar -C bin .
```

## Running

After compilation, you can run the utilities using the Java command:

```sh
java -cp bin com.pointblue.ldifutil.UtilityName [arguments]
```

Or, using the JAR:

```sh
java -cp ldifutils.jar com.pointblue.ldifutil.UtilityName [arguments]
```

A prebuilt JAR is also attached to each [GitHub release](../../releases).

## License

These utilities are public domain and may be used freely. The is no warrenty, either explicit or implied.
