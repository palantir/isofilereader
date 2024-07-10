# IsoFileReader 💿

IsoFileReader is an implementation of European Computer Manufacturers Association (ECMA) Standard 119. From the standard
 document "ECMA-119 is technically identical with ISO-9660", making this library able to read any ISO image that meets
 ECMA-119/ISO-9660 as well. Later UDF support was added to read images with the newer format. UDF support is less
 thoroughly tested than traditional ECMA-119/ISO-9660 support. Some lower level classes do not have nice Java wrappers,
 they give raw access to the byte arrays; I welcome PRs for improvements.

This library was created to have a pure Java library to read ISO files and extract data that can also read files
Extended Attributes. CentOS and other open source projects have started to ship images with file names over the standard
64 and 128 characters. This means if the Extended Records are not incorporated there are truncated file names. I was using
https://github.com/stephenc/java-iso-tools, but that project was only using some of the enhanced tables for the original
ISO-9660/ECMA-119 format; that project has also been discontinued.

Standards: [ECMA-119/ISO 9660 Standard](https://www.ecma-international.org/wp-content/uploads/ECMA-119_3rd_edition_december_2017.pdf), [ECMA-167/ISO_IEC 13346/Original UDF Standard](https://www.ecma-international.org/wp-content/uploads/ECMA-167_3rd_edition_june_1997.pdf), [Rock Ridge](https://web.archive.org/web/20170404043745/http://www.ymi.com/ymi/sites/default/files/pdf/Rockridge.pdf), [UDF 2.60](http://www.osta.org/specs/pdf/udf260.pdf)

## Features ⚙️

* Written in Java 11 (There are no real blockers from going below to 8 other than tests I believe)
* Supports ECMA-119/ISO-9660 and UDF files
* High level classes for simple use, low level available for advanced use-cases
* File data can be retrieved as byte arrays, or InputStreams to lower memory requirements
* Initialization Vectors for quick files access with pre-indexing
* No runtime dependencies, just testing dependencies. Pure Java, 100% Portable

## Development

### Building 🔨

Building is normal gradle,
```shell script
./gradlew clean build check
```

### Publishing

Put a release onto the release branch, and tag it. Github UI doesn't seem to allow this in one stroke, so I was PRing
into release then adding a version tag, and re-kick the Circle job.

### Check testing dependency versions

There is a fun gradle plugin, run below code to see if there are updates. A nice part of this library is its pure Java,
dependencies are just for testing.
```shell script
./gradlew dependencyUpdates
```

There is also a plugin installed to check the dependency graphs health, use the following to check the state:
```shell script
./gradlew projectHealth
```

## Usage 📦

### Installing

[Releases on Maven Central](https://mvnrepository.com/artifact/com.palantir.isofilereader/isofilereader)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation group: 'com.palantir', name: 'isofilereader', version: '$version'
}
```

### Class Structure

`GenericInternalIsoFile` is the standard file returned by IsoFileReader. `IsoFormatInternalDataFile` and
`UdfInternalDataFile` are extensions of `GenericInternalIsoFile`. For most use cases `GenericInternalIsoFile`
can be used, if needed you can access those files as their more specific types and get additional metadata.

`IsoFileReader` is the helper class that handles accessing the sub-readers of `TraditionalIsoReader` and `UdfIsoReader`.

#### ECMA-119/ISO-9660

`IsoFormatInternalDataFile` encapsulates `IsoFormatDirectoryRecord` (the Raw Directory Records). `TraditionalIsoReader` parses these ISOs.

#### UDF

(udf.types.files) `FileEntry` and (udf.types.files) `FileIdentifierDescriptor` are the raw records that are encapsulated in
`UdfInternalDataFile`.

### Reading Generic Image

Using the default constructor of `new IsoFileReader(iso);` automatically runs `findOptimalSettings()` against the image.
You can also import an image, then modify how it is processed; this mostly refers to if you purposefully want to disable
UDF or Rock Ridge usage.

```java
// For an image which has UDF + ECMA-119/ISO-9660 data
File isoFile = new File("./src/test/resources/small_only_udf_260.iso");
try (IsoFileReader isoFileReader = new IsoFileReader(isoFile)) {
    // This will default to using UDF because that was detected
    isoFileReader.setUdfModeInUse(false);
    isoFileReader.findOptimalSettings();
    // Now will be using the best table with Rock Ridge in the image

    isoFileReader.getTraditionalIsoReader().setUseRockRidgeOverStandard(false);
    // We also now have disabled Rock Ridge reading
    GenericInternalIsoFile[] files = isoFileReader.getAllFiles();
    ...
} catch (IOException | UdfFormatException e) {
    throw new RuntimeException(e);
}
```

### General Example

```java
File isoFile = new File("./src/test/resources/small_only_udf_260.iso");
try (IsoFileReader isoFileReader = new IsoFileReader(isoFile)) {
    GenericInternalIsoFile[] files = isoFileReader.getAllFiles();
    // These files will be in a tree, you can use the following for a flat array if you want that
    List<GenericInternalIsoFile> flatList = isoFileReader.convertTreeFilesToFlatList(files);
    for (GenericInternalIsoFile cycleFile : flatList) {
        // This gets data for each file, if your files are large, you probably want to stream instead
        byte[] data = isoFileReader.getFileBytes(cycleFile);

        // To not read all the data into memory at once, and read 2048 bytes at a time
        byte[] array = new byte[2048];
        InputStream stream = isoFileReader.getFileStream(cycleFile);
        int count = stream.read(array);
        // Array now has the first 2048 bytes

        // Another option is having an OutputStream ready, such as a file/web request
        ByteArrayOutputStream holdingBuffer = new ByteArrayOutputStream();
        long lengthOfDataRead = stream.transferTo(holdingBuffer);
        byte[] returnedData = holdingBuffer.toByteArray();
    }
} catch (IOException | UdfFormatException e) {
    throw new RuntimeException(e);
}
```

### Different ways to read files

Reading the directory as `IsoFormatInternalDataFile[] records = isoFileReader.getAllFilesAsInternalDataFiles();` gives an array of
files in the root directory, then each of those has child entries for files within it. An alternative is
`GenericInternalIsoFile[] records = isoFileReader.getAllFiles();`, `IsoFormatInternalDataFiles`. With the `IsoFormatInternalDataFiles`
you can `getUnderlyingRecord()` and access the `IsoFormatDirectoryRecord` within them, which allows all raw metadata
access. If you want all the files in a List over the hierarchical default format, use:
`List<GenericInternalIsoFile> allFilesAsList = isoImage.convertTreeFilesToFlatList(isoImage.getAllFilesAsIsoFormatInternalDataFile());`.

#### Reading ECMA-119/ISO 9660

```java
File isoFile = new File("./src/test/resources/CentOS-7-x86_64-Minimal-1908.iso");
try (IsoFileReader isoFileReader = new IsoFileReader(isoFile)) {
    IsoFormatInternalDataFile[] records = isoFileReader.getAllFilesAsIsoFormatInternalDataFile();
    // If we know the first/only file on disc is a text document
    // Reading with this function forces ISO handling
    byte[] data = isoFileReader.getIsoFormatInternalDataFileBytes(records[0]);
    System.out.println(new String(data));
} catch (IOException e) {
    //Iso was either not found, or error reading the file
}
```

#### Reading UDF

```java
File isoFile = new File("./test_isos/windows.iso");
try (IsoFileReader isoFileReader = new IsoFileReader(isoFile)) {
    UdfInternalDataFile[] rootFiles = isoFileReader.getUdfIsoReader().getAllFiles();

    // You can also mix and match the fetch methods
    GenericInternalIsoFile[] files = isoFileReader.getAllFiles();
    GenericInternalIsoFile bootWim = isoFileReader.getSpecificFileByName(files, "/sources/boot.wim");
    System.out.println(bootWim.getFullFileName(File.separatorChar));

    FileIdentifierDescriptor udfData = ((UdfInternalDataFile) bootWim).getThisFileDescriptor();
} catch (Exception e) {
    Assertions.fail("Failed to read image", e);
}
```

#### File Permissions

All the test images I have found do not set permissions on ECMA-119/ISO-9660 images. This was an added on attribute that
seems to not frequently get used, if someone has an image that uses them I am happy to take a look. The standard says
those files should have a flag in the 4th bit, and then point to extended attributes. UDF has permissions as a first
class attribute. Below is an example of reading UDF file permissions.

```java
File isoFile = new File("./test_isos/windows.iso");
try (IsoFileReader isoFileReader = new IsoFileReader(isoFile)) {
    GenericInternalIsoFile[] files = isoFileReader.getAllFiles();
    GenericInternalIsoFile bootWim = isoFileReader.getSpecificFileByName(files, "/sources/boot.wim");
    if (bootWim instanceof UdfInternalDataFile) {
        System.out.println("User Can Execute: "
            + ((UdfInternalDataFile) bootWim).getThisFileEntry().getFilePerms().canUserExecute());
    }
} catch (Exception e) {
    Assertions.fail("Failed to read image", e);
}
```

#### Initialization Vectors

The idea of Initialization Vectors (IVs) is to speed up access to files within images that are frequently accessed. If
an image is repeatably access, then the Image IV and File IV can be stored. These are facts about the image and then the
file which attempt to prove you are working on the same image as previously indexed. More about the format is below.
While a few steps are used to verify the file, this cannot be a guarantee; if your use case has image files changing a
lot then it is better to fully initialize the library and read the table of contents, vs using IVs. File data can be
quickly retrieved with static methods within IsoFileReader.

This has proven to be much faster at data retrieval than any library scanning the table of contents, because it can skip
that whole stage. In applications where images are indexed, this can give high confidence, while lowering compute and
disk IO needs.

##### Example File Read with IVs

```java
File isoFile = new File("./installdisc.iso");
String imageIv = "I1|2048|10|4139925504|7b5fe66f47d7b09dba115d0153a807c0";
String[] filesIv = {
    "F1|2048|4|354|67921|/.treeinfo|86b2c63b1e47ed585759b9005c6885e1",
    "F1|2048|4|134217728|2385|/images/efiboot.img|4577102270ea904d709f913146cfb782",
    "F1|2048|4|52893200|67925|/isolinux/initrd.img|446aea6f7e4e2342e338fa61e160c8d0"
};

for (String fileIv : filesIv) {
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(isoFile, "r")) {
        Optional<byte[]> data = IsoFileReader.getFileDataWithIVs(randomAccessFile, imageIv, fileIv);
        if (data.isPresent()) {
            String md5 = getMD5Hash(data.get());
        }
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

##### IV Format

###### IV Image Format

`I1|2048|10|4139925504|7b5fe66f47d7b09dba115d0153a807c0` is an example of an Image IV. They start with I1 to say Image
and version 1 of IV (currently 1 exists), then how large the data samples will be. Each field is separated by pipes.
Followed by how many samples to take; with the default of 10, the ISO will be sampled at 10 evenly distributed
locations. Then the total size of the image. Finally, the hash of the data sampled from those location at that sample
size.

###### IV File Format

`F1|2048|4|440725504|96962|/LiveOS/squashfs.img|86ad19438716e14b40255aec4caf98e4` File IVs are similar to the Image IV
except that they add a few more fields. We start with F1 for File and version 1 (there is currently 1 version), followed
by the sample size, and the number of samples. Since sub file sizes vary a lot, and we don't want to use all the time
reading computing samples, files are set to 4 samples currently. Next is the size of the file in bytes, followed by the
LOGICAL sector the data resides in. The logical sector needs to by multiplied by the ISO format constant of bytes per
sector of 2048 to get the absolute location in file. This is followed by the full path of the file in the image, and
ending with the hash of the samples. Before the sample data is MD5 hashed, the name of the file is added to the hashing
algorithm. This is used for situation where you have 2 empty files, and they would hash the same. If all these criteria
match in the image file, then the Reader will return the data or a stream as an Optional. If the optional returns empty,
the library should be fully initialized to read the image instead.

## Technical Notes of Implementation

### Terms

The Primary and Enhanced Volume descriptors are the entry point for an ECMA-119/ISO-9660 standard image. These
descriptors are sometimes called the Table of Contents for the disk. It gives basic information about the image, and
then a pointer to the root folder `IsoFormatDirectoryRecord`. The `IsoFormatDirectoryRecord` are then in a tree format out
from the root with all the files and paths on the image.

### Library notes

Some of the more core functions such as `IsoFormatDirectoryRecord` have a function to pull the raw bytes, then wrapper
functions that are *AsLong(), *AsDate(), *AsString(); these functions handle the conversions of different fields into
Java standard types. If you don't want to worry about a field being UTF-8 vs UTF-18BE vs other standards, then use the
wrapper functions. If you have some other use case, the raw bytes are there for you.

A single file can be represented as a `IsoFormatDirectoryRecord`, but those are the raw notes that are in the image,
`IsoFormatInternalDataFile` is a wrapper around `IsoFormatDirectoryRecord` that makes them easier to work with, if memory is
an issue you can raw process the Directory Records, but for simplicity most of the time `IsoFormatInternalDataFile`
probably wants to be used.

### Rock Ridge and File Names

There are at least 5 places on the ISO file system that a file name could be. One of the, is a file in every directory
called TRANS.TBL that has the short names for files, and then their long names. That will not be covered here and is
only used by some images. The alternatives are to put the file names in either the Primary descriptor portion of the
disc under Directory Records (that supports the very old 8.3 naming scheme), or under supplemental/enhanced volume
descriptors as Directory Records (that supports theoretically up to about 110 characters (with version 2 of the enhanced
descriptors I have seen up to 255 characters here), but most in practice use 64ish). One problem with the enhanced file
name, if the name is over the 64ish characters, then some people truncate the name to the first 64, others do 64 -
extension.
This means "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd1234.txt" either becomes:
"abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd1234" OR
"abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd.txt"

Another system for noting these names is Rock Ridge, this is an extension that uses the "System Use" area of a files
record. This allows however long file names you want. These can be stored in either the primary or enhanced tables. The
function `isoReader.findOptimalSettings();` will check which of these different types of file names exist on the disc; checking
every volume descriptor, then checking for rock ridge under that descriptor. This function will then set the
TableOfContentInUse to the optimal one, and set Rock Ridge to be enabled or disabled.

There are many ways to get file names out of an image. A basic image may have a Primary and Enhanced volume descriptors,
but it also can have several versions of those descriptors. `isoReader.findOptimalSettings()` will look at every table, and find
the longest file names available, with and without Rock Ridge. `IsoFormatDirectoryRecord` (being the raw type) will return
very short names compared to the Enhanced table, or Primary with Rock Ridge on. Using `IsoInternalDataFile` will check
for you if Rock Ridge is enabled at the image level, then give you the best name it can.

The raw type of `IsoFormatDirectoryRecord` can be faster to process than `IsoFormatInternalDataFile`, knowing this you can
quickly convert between them to get the correct filename.

```java
new IsoFormatInternalDataFile(singleRecord, isoFileReader.isUseRockRidgeOverStandard()).getFileName()
```

Below is a table where we were trying to find the file "51286396aa8cf471627d865ed743bc341cb587ead96c5da11e65d818945cd14d
-filelists.sqlite.bz2" using different methods and showing what name comes back for
`IsoFormatDirectoryRecord.getFileIdentifierAsString()` vs `IsoFormatInternalDataFile.getFullFileName('/')`.

Pri is Primary Volume Descriptor. Enh is the Enhanced Volume Descriptor. NRR is no Rock Ridge. RR is Rock Ridge.

|         | `IsoDirectoryRecord`                                                          | `IsoInternalDataFile`                                                                             |
|---------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Pri/NRR | /REPODATA/51286396AA8CF471627D865ED74.BZ;1                                  | /REPODATA/51286396AA8CF471627D865ED74.BZ;1                                                      |
| Enh/NRR | /repodata/51286396aa8cf471627d865ed743bc341cb587ead96c5da11e65d818945cd14d  | /repodata/51286396aa8cf471627d865ed743bc341cb587ead96c5da11e65d818945cd14d                      |
| Pri/RR  | /REPODATA/51286396AA8CF471627D865ED74.BZ;1                                  | /repodata/51286396aa8cf471627d865ed743bc341cb587ead96c5da11e65d818945cd14d-filelists.sqlite.bz2 |
| Enh/RR  | /repodata/51286396aa8cf471627d865ed743bc341cb587ead96c5da11e65d818945cd14d  | /repodata/51286396aa8cf471627d865ed743bc341cb587ead96c5da11e65d818945cd14d                      |

Depending on the authoring tool used to make the ISO, this may not always be true for which way to get filenames is best.
`isoReader.findOptimalSettings()` will scan and find the best settings, the only downside is it can take some time.

## Standards Information

The first 16 sectors of an image are for system use and bootloader information. Logical sectors are set at a standard
size of 2048 bytes. This means the ISO Header begins at 16 * 2048 bytes into the disc, or 32,768 bytes.

Sectors         | Contents
0-15            | Boot loader information
16              | Primary Volume Descriptor
17              | Supplemental/Enhanced Volume Descriptor
18              | Usually a Volume Descriptor Terminator
19              | Possibly M Path Table
20              | Possible L Path Table
                | Directory Records
                | Directory Records for Sub directories
                | Data

## Outstanding Issues/Assumptions

1. Technically an image can use any block size they want, they don't have to use the standard 2048 bytes per logical
block. This is currently not checked for and assumptions of 2048 are used a lot.

2. M Path and L Path tables aren't used, more the file descriptors are for the IsoImage iterators.

3. Permissions work with UDF, but all the images I have for ECMA-119/ISO-9660 don't use them. Thus, UDF has implemented
getters for file permissions, but ECMA-119/ISO 9660 does not.

## Footnotes

https://wiki.osdev.org/ISO_9660

http://www.idea2ic.com/File_Formats/iso9660.pdf

http://www.gnu.org/software/libcdio/doxygen/structiso9660__svd__s.html#a3027ac28d216156294330f74b0d450f9

https://www.kernel.org/doc/html/latest/filesystems/isofs.html

https://www.ecma-international.org/wp-content/uploads/ECMA-35_6th_edition_december_1994.pdf

http://www.brankin.com/main/technotes/Notes_ISO9660.htm
