```markdown
# LDIF Utilities

This project contains several utilities for processing and comparing LDIF (LDAP Data Interchange Format) files. Below is a description of each utility class and its functionality.

## `StripAttributes`

The `StripAttributes` class processes an LDIF file to remove specified attributes. It reads the input LDIF file, removes the specified attributes, and writes the result to an output file.

### Usage

```sh
java com.pointblue.ldifutil.StripAttributes <input-file> <output-file> <attributes-to-remove>
```

### Example

```sh
java com.pointblue.ldifutil.StripAttributes input.ldif output.ldif cn,sn
```

## `LDIFRecordComparator`

The `LDIFRecordComparator` class compares records from two LDIF files. It reads the input LDIF files, parses the records, and compares them to identify differences.

### Usage

```sh
java com.pointblue.ldifutil.LDIFRecordComparator <ldif-file1> <ldif-file2>
```

### Example

```sh
java com.pointblue.ldifutil.LDIFRecordComparator file1.ldif file2.ldif
```

## `LDIFAttributeExtractor`

The `LDIFAttributeExtractor` class extracts specified attributes from an LDIF file. It reads the input LDIF file, extracts the specified attribute values, and prints them along with their DNs.

### Usage

```sh
java com.pointblue.ldifutil.LDIFAttributeExtractor <input-file> <attribute-to-extract>
```

### Example

```sh
java com.pointblue.ldifutil.LDIFAttributeExtractor input.ldif mail
```

## `LDIFAttributeComparator`

The `LDIFAttributeComparator` class provides functionality to compare attributes between two LDIF files. It reads the LDIF files, parses the records, and compares the specified attribute or all attributes if none is specified.

### Usage

```sh
java com.pointblue.ldifutil.LDIFAttributeComparator <ldif-file1> <ldif-file2> [<attribute-to-compare>]
```

### Example

```sh
java com.pointblue.ldifutil.LDIFAttributeComparator file1.ldif file2.ldif mail
```

## `LDIFAttr2DirAttrCompare`

The `LDIFAttr2DirAttrCompare` class compares an attribute from an LDIF file with the corresponding attribute in an LDAP directory. It reads the LDIF file, extracts the specified attribute, and compares it with the attribute in the LDAP directory.

### Usage

```sh
java com.pointblue.ldifutil.LDIFAttr2DirAttrCompare <input-file> <attribute-to-extract> <ldap-url> <base-dn> <ldap-username> <ldap-password>
```

### Example

```sh
java com.pointblue.ldifutil.LDIFAttr2DirAttrCompare input.ldif mail ldap://localhost:389 dc=example,dc=com cn=admin password
```
```
