/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.isofilereader.isofilereader;

import com.palantir.isofilereader.isofilereader.iso.IsoFormatInternalDataFile;
import com.palantir.isofilereader.isofilereader.iso.TraditionalIsoReader;
import com.palantir.isofilereader.isofilereader.iso.types.AbstractVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatEnhancedVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatPrimaryVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.udf.UdfFormatException;
import com.palantir.isofilereader.isofilereader.udf.UdfInternalDataFile;
import com.palantir.isofilereader.isofilereader.udf.UdfIsoReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IsoFileReader implements AutoCloseable {
    private final File isoFile;
    private final TraditionalIsoReader traditionalIsoReader;
    private final List<RandomAccessFile> openFileHandles = new ArrayList<>();
    private int udfModeInUse = 0; // 0 is not initialized, 1 is do not use, 2 is use. This is used to manually override
    // the auto-detection of UDF.
    private final UdfIsoReader udfIsoReader;

    /**
     * Create a new file reader with the file attached, this constructor will automatically scan the iso for which
     * headers to use.
     *
     * @param isoFile file to use
     * @throws IOException in attempting find the correct headers to use, a IO exception occurred
     */
    public IsoFileReader(File isoFile) throws IOException {
        this.isoFile = isoFile;
        this.traditionalIsoReader = new TraditionalIsoReader(isoFile);
        this.udfIsoReader = new UdfIsoReader(isoFile);
        findOptimalSettings();
    }

    /**
     * Create a new file reader with the file attached, this constructor uses the input setting for the headers to use.
     *
     * @param isoFile file to use
     * @param setting header setting to use, formatted as "#,#,#"
     */
    public IsoFileReader(File isoFile, String setting) {
        this.isoFile = isoFile;
        this.traditionalIsoReader = new TraditionalIsoReader(isoFile);
        this.udfIsoReader = new UdfIsoReader(isoFile);
        implementGivenSetting(setting);
    }

    /**
     * Close for auto closing, if the user has used getIsoWithAutoClose, then all file handles are closed.
     */
    @Override
    public void close() {
        for (RandomAccessFile temp : openFileHandles) {
            if (temp != null && temp.getChannel().isOpen()) {
                try {
                    temp.close();
                } catch (IOException e) {
                    // whatever
                }
            }
        }
    }

    /**
     * Get the traditionalIsoReader from inside the Generic IsoFileReader. This allows lower level access to
     * traditional ISO internals.
     *
     * @return traditional reader
     */
    public TraditionalIsoReader getTraditionalIsoReader() {
        return traditionalIsoReader;
    }

    /**
     * Replace the default system file/folder separator with a specific characters. By default, the library uses your
     * system separator (linux /, Windows \) this allows to override that.
     *
     * @param passedChar char to use as file separator
     */
    public void useSeparatorChar(char passedChar) {
        traditionalIsoReader.setSeparatorChar(passedChar);
        udfIsoReader.setSeparatorChar(passedChar);
    }

    /**
     * Separator characters can be changed, this allows functions that need them to get them.
     *
     * @return char of separator
     */
    public char getSeparatorChar() {
        return traditionalIsoReader.getSeparatorChar();
    }

    /**
     * Direct access to the underlying Udf reader, this allows for lower level image access.
     *
     * @return The reader object itself
     */
    public UdfIsoReader getUdfIsoReader() {
        return udfIsoReader;
    }

    /**
     * Get all the files in an image as an array. Will return null on read errors.
     *
     * @return Array of all file objects
     * @throws IOException If the file fails to open or read you can get an IOException
     */
    public IsoFormatDirectoryRecord[] getAllFileRecordsInIsoRaw() throws IOException {
        IsoFormatDirectoryRecord currentFileDirectoryRecord = traditionalIsoReader.getRootDirectoryOfCurrentToC();
        return traditionalIsoReader.getIsoDirectoryRecords(
                currentFileDirectoryRecord.getLocOfExtAsLong(), currentFileDirectoryRecord.getDataLengthAsLong(), "");
    }

    /**
     * Get all the files in an image as an array.
     *
     * @return Array of all internal file metadata
     * @throws IOException If the file fails to open or read you can get an IOException
     */
    public IsoFormatInternalDataFile[] getAllFilesAsIsoFormatInternalDataFile() throws IOException {
        IsoFormatDirectoryRecord currentFileDirectoryRecord = traditionalIsoReader.getRootDirectoryOfCurrentToC();
        IsoFormatInternalDataFile rootLevel = new IsoFormatInternalDataFile(
                currentFileDirectoryRecord, traditionalIsoReader.isUseRockRidgeOverStandard());
        rootLevel.addChildren(traditionalIsoReader.getInternalDataFiles(
                getRawIsoWithAutoClose(),
                currentFileDirectoryRecord.getLocOfExtAsLong(),
                currentFileDirectoryRecord.getDataLengthAsLong(),
                ""));
        IsoFormatInternalDataFile[] tempArray = new IsoFormatInternalDataFile[1];
        tempArray[0] = rootLevel;
        return tempArray;
    }

    /**
     * Get all internal files as UdfInternalDataFile in an array.
     *
     * @return Array of all internal file metadata
     * @throws UdfFormatException failure to read the table of contents
     */
    public UdfInternalDataFile[] getAllFilesAsUdfInternalDataFiles() throws UdfFormatException {
        return udfIsoReader.getAllFiles();
    }

    /**
     * No matter the internal format of the image, get all the directory records for this ISO. This will return
     * InternalIsoFile, which will be an instance of either {@link IsoFormatInternalDataFile} or {@link UdfInternalDataFile}.
     *
     * @return InternalIsoFile directory records
     * @throws IOException file read error
     * @throws UdfFormatException UDF table of contents read error
     */
    public GenericInternalIsoFile[] getAllFiles() throws IOException, UdfFormatException {
        if (isUdfModeInUse()) {
            return getAllFilesAsUdfInternalDataFiles();
        } else {
            return getAllFilesAsIsoFormatInternalDataFile();
        }
    }

    /**
     * Get raw access to the iso for file operations. YOU NEED TO CLOSE THIS!
     *
     * @return RandomAccessFile access with read to the file
     * @throws FileNotFoundException if the file is not found this can error
     */
    public RandomAccessFile getRawIso() throws FileNotFoundException {
        return new RandomAccessFile(isoFile, "r");
    }

    /**
     * This is the same as getRawIso but tracks which file handles are open for auto closing, safer to use, but if a lot
     * of file handles are being used can decrease perf.
     *
     * @return get a RandomAccessFile handle
     * @throws FileNotFoundException if file cant be found then this is thrown.
     */
    public RandomAccessFile getRawIsoWithAutoClose() throws FileNotFoundException {
        RandomAccessFile temp = new RandomAccessFile(isoFile, "r");
        openFileHandles.add(temp);
        return temp;
    }

    /**
     * We need a way to scan for best file names and use that, either primary, enhanced, or rock ridge in primary or
     * enhanced. Some ISOs will have multiple enhanced tables and each needs checked for if it has better names.
     * Depending on the vendor who mastered it, the image may have different headers that offer better names.
     * <a href="https://web.archive.org/web/20170404043745/http://www.ymi.com/ymi/sites/default/files/pdf/Rockridge.pdf">Rock Ridge</a>
     * Note this is done with the default constructor.
     *
     * @throws IOException Error reading from the ISO in getting the headers
     */
    public void findOptimalSettings() throws IOException {
        if (udfModeInUse == 0 && udfIsoReader.checkForUdfData()) {
            // UDF is a newer standard and at the current time we will assume a disk with UDF would prefer that.
            udfModeInUse = 2;
            return;
        }

        AbstractVolumeDescriptor[] headers = traditionalIsoReader.getVolumeDescriptors();
        if (headers.length == 0) {
            return;
        }

        int bestTableSoFar = -1;
        int longestFileNameFound = -1;
        boolean enableRockRidge = false;
        for (int i = 0; i < headers.length; i++) {
            AbstractVolumeDescriptor vol = headers[i];
            IsoFormatDirectoryRecord rootIsoDirectoryRecord;
            switch (vol.getVolumeDescriptorTypeAsInt()) {
                case AbstractVolumeDescriptor.IsoEnhancedVolumeDescriptor:
                    rootIsoDirectoryRecord = ((IsoFormatEnhancedVolumeDescriptor) vol)
                            .getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
                    break;
                case AbstractVolumeDescriptor.IsoPrimaryVolumeDescriptor:
                default:
                    rootIsoDirectoryRecord = ((IsoFormatPrimaryVolumeDescriptor) vol)
                            .getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
                    break;
            }
            if (!new String(vol.getStandardIdentifier(), StandardCharsets.UTF_8).equals("CD001")) {
                // This table is not a standard table, or at least a standard we understand.
                continue;
            }

            IsoFormatInternalDataFile[] rootLevelDiscFolder = traditionalIsoReader.getInternalDataFiles(
                    getRawIsoWithAutoClose(),
                    rootIsoDirectoryRecord.getLocOfExtAsLong(),
                    rootIsoDirectoryRecord.getDataLengthAsLong(),
                    "");
            if (rootLevelDiscFolder != null) {
                // Checking for rock ridge
                int temp = traditionalIsoReader.scanForNmEntries(rootLevelDiscFolder);
                if (temp > longestFileNameFound) {
                    enableRockRidge = true;
                    bestTableSoFar = i;
                    longestFileNameFound = temp;
                }
                // Checking with enhanced descriptors without rock ridge
                temp = traditionalIsoReader.getLongestFileNameWithoutRockRidge(rootLevelDiscFolder);
                if (temp > longestFileNameFound) {
                    enableRockRidge = false;
                    bestTableSoFar = i;
                    longestFileNameFound = temp;
                }
            }
        }
        traditionalIsoReader.setUseRockRidgeOverStandard(enableRockRidge);
        traditionalIsoReader.setTableOfContentsInUse(bestTableSoFar);
    }

    /**
     * Get the current setting in use internal header settings. These are formatted as 3 numbers with commas between
     * them. The first is if UDF mode is in use, second is if RockRidge is in use, third is Table ID to use.
     *
     * @return String
     */
    public String getCurrentSetting() {
        if (udfModeInUse == 2) {
            return "1,0,0";
        }

        String setting = "0" + ",";
        setting += traditionalIsoReader.isUseRockRidgeOverStandard() ? "1," : "0,";
        setting += traditionalIsoReader.getTableOfContentsInUse();
        return setting;
    }

    private void implementGivenSetting(String setting) {
        String[] splitSetting = setting.split("\\s*,\\s*", -1);

        if (splitSetting[0].equals("1")) {
            udfModeInUse = 2;
            return;
        }

        traditionalIsoReader.setUseRockRidgeOverStandard(splitSetting[1].equals("1"));
        traditionalIsoReader.setTableOfContentsInUse(Integer.parseInt(splitSetting[2]));
    }

    /**
     * Retrieve a byte array from a directory record passed in, either ISO or UDF. Lookout for large files in memory can
     * be dangerous.
     *
     * @param file directory record to get bytes for
     * @return byte array of the data
     * @throws IOException failed to read underlying ISO file
     */
    public byte[] getFileBytes(GenericInternalIsoFile file) throws IOException {
        long dataSize = file.getSize();
        byte[] data = new byte[(int) dataSize];
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = getRawIso();
            randomAccessFile.seek(file.getLogicalSectorLocation() * IsoFormatConstant.BYTES_PER_SECTOR);
            int read = randomAccessFile.read(data, 0, (int) dataSize);
            if (read != (int) dataSize) {
                throw new IOException("Failed to read correct amount of data.");
            }
        } finally {
            if (randomAccessFile != null && randomAccessFile.getChannel().isOpen()) {
                randomAccessFile.close();
            }
        }
        return data;
    }

    /**
     * Check if the reader is functioning in UDF mode.
     *
     * @return true if so
     */
    public boolean isUdfModeInUse() {
        return udfModeInUse == 2;
    }

    /**
     * Set if we want the reader to run in UDF mode.
     *
     * @param udfModeInUse UDF enabled
     */
    public void setUdfModeInUse(boolean udfModeInUse) {
        if (udfModeInUse) {
            this.udfModeInUse = 2;
        } else {
            this.udfModeInUse = 1;
        }
    }

    /**
     * Get a file stream of the specific GenericInternalIsoFile in the image. This allows for streaming of large
     * files. This method creates a new RandomAccessFile.
     *
     * @param file file to access
     * @return InputStream
     * @throws IOException can occur when failing to read underlying media
     */
    public InputStream getFileStream(GenericInternalIsoFile file) throws IOException {
        return new IsoInputStream(getRawIsoWithAutoClose(), file);
    }

    /**
     * Get a file stream of the specific GenericInternalIsoFile in the image. This allows for streaming of large files.
     * This method DOES NOT create a new RandomAccessFile.
     *
     * @param file random access file to use
     * @param subFile GenericInternalIsoFile to get
     * @return InputStream
     * @throws IOException can occur when failing to read underlying media
     */
    public InputStream getFileStream(RandomAccessFile file, GenericInternalIsoFile subFile) throws IOException {
        return new IsoInputStream(file, subFile);
    }

    /**
     * Searches the collection of files a file given as a representation. This is useful if you know the file you want.
     *
     * @param files file collection to search
     * @param filename filename with either \ or / file separators in use
     * @return the file in question
     */
    public Optional<GenericInternalIsoFile> getSpecificFileByName(GenericInternalIsoFile[] files, String filename) {
        String paddedFilename = filename;
        if (!paddedFilename.startsWith(File.separator)) {
            paddedFilename = File.separator + paddedFilename;
        }

        String normalizedFilename = paddedFilename.replace('\\', '/');
        if ("/".equals(normalizedFilename)) {
            return Optional.of(files[0]);
        }

        String[] splitPath = normalizedFilename.split("/");
        return fileSearcher(files, splitPath, 0);
    }

    private Optional<GenericInternalIsoFile> fileSearcher(GenericInternalIsoFile[] files, String[] nameSplit, int pos) {
        for (GenericInternalIsoFile singleFile : files) {
            String name = singleFile.getFileName();
            if (name.contains(";")) {
                // If you get the most basic of names, in 8.3, sometimes there is a ;# at the end
                name = name.split(";", -1)[0];
            }
            if (nameSplit[pos].equals(name)) {
                // File does match at this name location
                if (pos == (nameSplit.length - 1)) {
                    return Optional.of(singleFile);
                } else {
                    int tempPos = pos + 1;
                    return fileSearcher(singleFile.getChildren(), nameSplit, tempPos);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Convert a tree structure of files into a flat array.
     *
     * @param records Tree style file records
     * @return flat list of files
     */
    public List<GenericInternalIsoFile> convertTreeFilesToFlatList(GenericInternalIsoFile[] records) {
        List<GenericInternalIsoFile> returnData = new ArrayList<>();
        for (GenericInternalIsoFile singleRecord : records) {
            if (singleRecord.isDirectory() && singleRecord.getChildren() != null) {
                // Directory
                returnData.addAll(convertTreeFilesToFlatList(singleRecord.getChildren()));
            } else {
                // File we want
                returnData.add(singleRecord);
            }
        }
        return returnData;
    }

    /**
     * Initialization Vectors are for systems that use the same images very frequently and do not want to have the
     * overhead of constantly reading the table of contents. The idea is you can get the IV of the image and the IV
     * of the file, then next time you go to read the file, if the quick IV check passes, then just read the specific
     * file at that location.
     *
     * @return string of the IV of this image
     * @throws IOException failure to read inside the image where needed
     * @throws NoSuchAlgorithmException failure to load MD5 in this JDK
     */
    public String getInitializationVectorForImage() throws IOException, NoSuchAlgorithmException {
        RandomAccessFile file = getRawIsoWithAutoClose();
        return getInitializationVectorForImageWithPassedFile(file);
    }

    private static String getInitializationVectorForImageWithPassedFile(RandomAccessFile file)
            throws IOException, NoSuchAlgorithmException {
        String iv = "I1|";
        int bytesToRead = 2048;
        iv += bytesToRead;
        iv += "|";
        int numberOfReadLocations = 10;
        iv += numberOfReadLocations;
        iv += "|";

        iv += file.length();
        iv += "|";

        MessageDigest md = MessageDigest.getInstance("MD5");
        updateHashWithData(md, file, bytesToRead, numberOfReadLocations);

        byte[] bytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        iv += sb.toString();
        return iv;
    }

    private static void updateHashWithData(
            MessageDigest md, RandomAccessFile file, int bytesToRead, int numberOfReadLocations) throws IOException {
        for (long loc = 0; loc < numberOfReadLocations; loc += (file.length() / numberOfReadLocations) + 1) {
            file.seek(loc);
            byte[] byteArray = new byte[bytesToRead];
            int bytesCount = 0;

            bytesCount = file.read(byteArray);
            md.update(byteArray, 0, bytesCount);
        }
    }

    /**
     * Get the Initialization Vector for the file, this speeds up file retrieval in high repeating read situations.
     *
     * @return string of the IV
     */
    public String getFileInitializationVectorForFile(GenericInternalIsoFile genericInternalIsoFile)
            throws IOException, NoSuchAlgorithmException {
        String iv = "F1|";
        int bytesToRead = 2048;
        iv += bytesToRead;
        iv += "|";
        int numberOfReadLocations = 4;
        iv += numberOfReadLocations;
        iv += "|";

        InputStream is = getFileStream(genericInternalIsoFile);

        iv += genericInternalIsoFile.getSize();
        iv += "|";

        iv += genericInternalIsoFile.getLogicalSectorLocation();
        iv += "|";

        iv += genericInternalIsoFile.getFullFileName('/');
        iv += "|";

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(genericInternalIsoFile.getFullFileName('/').getBytes(StandardCharsets.UTF_8));
        String md5 = updateHashWithDataInputStream(md, is, bytesToRead, numberOfReadLocations);

        iv += md5;
        return iv;
    }

    private static String updateHashWithDataInputStream(
            MessageDigest md, InputStream file, int bytesToRead, int numberOfReadLocations) throws IOException {
        for (long loc = 0; loc < numberOfReadLocations; loc += (file.available() / numberOfReadLocations) + 1) {
            file.reset();
            if (file.skip(loc) != 0) {
                byte[] byteArray = new byte[bytesToRead];
                int bytesCount = file.read(byteArray);
                md.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static Optional<byte[]> getFileDataWithIVsFromFile(File file, String imageIv, String fileIv)
            throws IOException, NoSuchAlgorithmException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        Optional<byte[]> data = getFileDataWithIVs(randomAccessFile, imageIv, fileIv);
        if (randomAccessFile.getChannel().isOpen()) {
            randomAccessFile.close();
        }
        return data;
    }

    /**
     * Retrieve ta files bytes by presenting an image file, with a known image IV and a file IV. If both IVs can be
     * verified then the data is received and sent back, if the IVs fail, then an empty optional is returned and the
     * image should be fully accessed and parsed instead.
     * Note: This will use the RandomAccessFile to access and seek that file! This is not Thread safe for
     * multithreading!
     *
     * @param rafFile Raw image as a RandomAccessFile
     * @param imageIv Initialization vector of the image, gotten from another time when the full library was used
     * @param fileIv Initialization vector of the file, gotten from another time when the full library was used
     * @return Either bytes of the file, or empty if IVs fail
     * @throws IOException Opening the image can fail resulting in a IOException
     * @throws NoSuchAlgorithmException MD5 is used to verify the IV, if MD5 is not in the local JDK this will fail
     */
    public static Optional<byte[]> getFileDataWithIVs(RandomAccessFile rafFile, String imageIv, String fileIv)
            throws IOException, NoSuchAlgorithmException {
        /*
           Example
           I-IV: I1|2048|10|1310720|345bd27a7de3762f50b260f197023c13
           F-IV: F1|2048|4|53|550|/test2/aligned.md5|281864d2591d72115a41593c788cda4c
        */
        if (!getInitializationVectorForImageWithPassedFile(rafFile).equals(imageIv)) {
            return Optional.empty();
        }
        String[] oldFiv = fileIv.split("\\|", -1);

        String fiv = reconstructFileIv(
                rafFile,
                Integer.parseInt(oldFiv[1]), // Bytes To Read
                Integer.parseInt(oldFiv[2]), // Places To Read
                Long.parseLong(oldFiv[3]), // Size
                Long.parseLong(oldFiv[4]), // Logical Sector
                oldFiv[5]); // File Name
        if (!fiv.equals(fileIv)) {
            return Optional.empty();
        }

        long dataSize = Long.parseLong(oldFiv[3]);
        byte[] data = new byte[(int) dataSize];
        try {
            rafFile.seek(Long.parseLong(oldFiv[4]) * IsoFormatConstant.BYTES_PER_SECTOR);
            int read = rafFile.read(data, 0, (int) dataSize);
            if (read != (int) dataSize) {
                throw new IOException("Failed to read correct amount of data.");
            }
        } finally {
            if (rafFile.getChannel().isOpen()) {
                rafFile.close();
            }
        }
        return Optional.of(data);
    }

    private static String reconstructFileIv(
            RandomAccessFile rafFile,
            int bytesToRead,
            int numberOfReadLocations,
            long size,
            long logicalSector,
            String filename)
            throws IOException, NoSuchAlgorithmException {
        String iv = "F1|" + bytesToRead + "|" + numberOfReadLocations + "|" + size + "|" + logicalSector + "|"
                + filename + "|";
        // F1|2048|4|53|550|/test2/aligned.md5|281864d2591d72115a41593c788cda4c
        IsoInputStream newInputStream =
                new IsoInputStream(rafFile, logicalSector * IsoFormatConstant.BYTES_PER_SECTOR, size);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(filename.getBytes(StandardCharsets.UTF_8));
        String md5 = updateHashWithDataInputStream(md, newInputStream, bytesToRead, numberOfReadLocations);

        iv += md5;
        return iv;
    }

    /**
     * Get the data in a file from an image, initialized by the image and file IVs, returning an InputStream. This
     * function is not thread safe,since it will access your RandomAccessFile given and use that in the InputStream.
     *
     * @param rafFile RandomAccessFile that will back the input stream
     * @param imageIv Initialization vector of the image, gotten from another time when the full library was used
     * @param fileIv Initialization vector of the file, gotten from another time when the full library was used
     * @return Optional of either the InputStream requested or an empty optional with nothing in it
     * @throws IOException This can occur when failing to read from the image
     * @throws NoSuchAlgorithmException MD5 is used to verify IVs
     */
    public static Optional<InputStream> getFileDataAsStreamWithIVs(
            RandomAccessFile rafFile, String imageIv, String fileIv) throws IOException, NoSuchAlgorithmException {
        /*
           Example
           I-IV: I1|2048|10|1310720|345bd27a7de3762f50b260f197023c13
           F-IV: F1|2048|4|53|550|/test2/aligned.md5|281864d2591d72115a41593c788cda4c
        */
        if (!getInitializationVectorForImageWithPassedFile(rafFile).equals(imageIv)) {
            return Optional.empty();
        }
        String[] oldFiv = fileIv.split("\\|", -1);

        String fiv = reconstructFileIv(
                rafFile,
                Integer.parseInt(oldFiv[1]), // Bytes To Read
                Integer.parseInt(oldFiv[2]), // Places To Read
                Long.parseLong(oldFiv[3]), // Size
                Long.parseLong(oldFiv[4]), // Logical Sector
                oldFiv[5]); // File Name
        if (!fiv.equals(fileIv)) {
            return Optional.empty();
        }

        IsoInputStream isoInputStream = new IsoInputStream(
                rafFile, Long.parseLong(oldFiv[4]) * IsoFormatConstant.BYTES_PER_SECTOR, Long.parseLong(oldFiv[3]));
        return Optional.of(isoInputStream);
    }

    /**
     * Get the data in a file from an image, initialized by the image and file IVs, returning an InputStream. This
     * function is thread safe, creating a new RandomAccessFile when called, user must use InputStream.close() when
     * done.
     *
     * @param file A file which will back the input stream
     * @param imageIv Initialization vector of the image, gotten from another time when the full library was used
     * @param fileIv Initialization vector of the file, gotten from another time when the full library was used
     * @return Optional of either the InputStream requested or an empty optional with nothing in it
     * @throws IOException This can occur when failing to read from the image
     * @throws NoSuchAlgorithmException MD5 is used to verify IVs
     */
    public static Optional<InputStream> getFileDataAsStreamWithIVsFromFile(File file, String imageIv, String fileIv)
            throws IOException, NoSuchAlgorithmException {
        RandomAccessFile rafFile = new RandomAccessFile(file, "r");
        return getFileDataAsStreamWithIVs(rafFile, imageIv, fileIv);
    }
}
