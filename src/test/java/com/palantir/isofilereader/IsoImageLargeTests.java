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

package com.palantir.isofilereader;

import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileEntry;
import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileSystem;
import com.palantir.isofilereader.isofilereader.GenericInternalIsoFile;
import com.palantir.isofilereader.isofilereader.IsoFileReader;
import com.palantir.isofilereader.isofilereader.iso.IsoFormatInternalDataFile;
import com.palantir.isofilereader.isofilereader.iso.types.AbstractVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import com.palantir.isofilereader.isofilereader.udf.UdfFormatException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IsoImageLargeTests {

    @Test
    void getImageHeaders() {
        File isoFile = new File("./test_isos/rocky.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            AbstractVolumeDescriptor[] header = iso.getTraditionalIsoReader().getVolumeDescriptors();
            Assertions.assertNotNull(header);
        } catch (Exception e) {
            Assertions.fail("Could not get header");
        }
    }

    @Test
    void getProcessedImageFiles() {
        File isoFile = new File("./test_isos/LongFileName_ISO_only._nojoliet.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            System.out.println(iso.getCurrentSetting());
            iso.setUdfModeInUse(false);
            System.out.println(iso.getCurrentSetting());
            iso.findOptimalSettings();
            System.out.println(iso.getCurrentSetting());
            // I know these images don't contain UDF data, so testing they do not enable the mode.
            Assertions.assertFalse(iso.isUdfModeInUse());
            System.out.println(iso.getTraditionalIsoReader().getTableOfContentsInUse());
            IsoFormatDirectoryRecord rootRecord = iso.getTraditionalIsoReader().getRootDirectoryOfCurrentToC();
            System.out.println("Enhanced loc of ext: " + rootRecord.getLocOfExtAsLong());
            IsoFormatInternalDataFile[] records = iso.getAllFilesAsIsoFormatInternalDataFile();
            Assertions.assertNotNull(records);
            treePrint(iso.getSeparatorChar(), records);
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    private void treePrint(char sepChar, IsoFormatInternalDataFile[] records) {
        for (IsoFormatInternalDataFile singleRecord : records) {
            if (singleRecord.isDirectory() && singleRecord.getChildren() != null) {
                System.out.println(singleRecord.getFullFileName(sepChar));
                treePrint(sepChar, singleRecord.getChildren());
            } else {
                if (singleRecord.getUnderlyingRecord().isPresent()
                        && !singleRecord.getUnderlyingRecord().get().isTopLevelIdentifier()) {
                    System.out.println(singleRecord.getFullFileName(sepChar));
                }
            }
        }
    }

    @Test
    void getImageFiles() {
        File isoFile = new File("./test_isos/rocky.iso");
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            Assertions.assertFalse(iso.isUdfModeInUse());
            IsoFormatDirectoryRecord[] records = iso.getAllFileRecordsInIsoRaw();
            Assertions.assertNotNull(records);
            for (IsoFormatDirectoryRecord singleRecord : records) {
                System.out.print(singleRecord.getParent());
                System.out.println(File.separatorChar + singleRecord.getFileIdentifierAsString());
            }
            System.out.println("get as internaldatafiles");
            for (IsoFormatInternalDataFile singleRecord : iso.getAllFilesAsIsoFormatInternalDataFile()) {
                System.out.println("-" + singleRecord.getFullFileName('/'));
                printRecursive(singleRecord, "");
            }
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    private void printRecursive(IsoFormatInternalDataFile singleRecord, String header) {
        String localHeaderInfo = "-" + header;
        for (IsoFormatInternalDataFile single : singleRecord.getChildren()) {
            if (single.isDirectory()) {
                printRecursive(single, localHeaderInfo);
            }
            System.out.println(localHeaderInfo + single.getFullFileName('/'));
        }
    }

    @Test
    void getImageFilesAllFileNameTypes() {
        File isoFile = new File("./test_isos/rocky.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            AbstractVolumeDescriptor[] headers = iso.getTraditionalIsoReader().getVolumeDescriptors();
            for (int i = 0; i < headers.length; i++) {
                AbstractVolumeDescriptor vol = headers[i];
                System.out.println("Volume Header, type: " + vol.getVolumeDescriptorTypeAsInt() + ", Version: "
                        + vol.getVolumeDescriptorVersion());
                iso.getTraditionalIsoReader().setTableOfContentsInUse(i);
                iso.getTraditionalIsoReader().setUseRockRidgeOverStandard(false);
                IsoFormatInternalDataFile[] records = iso.getAllFilesAsIsoFormatInternalDataFile();
                Assertions.assertNotNull(records);
                treePrint(iso.getSeparatorChar(), records);

                iso.getTraditionalIsoReader().setUseRockRidgeOverStandard(true);
                records = iso.getAllFilesAsIsoFormatInternalDataFile();
                Assertions.assertNotNull(records);
                treePrint(iso.getSeparatorChar(), records);
            }
        } catch (IOException e) {
            Assertions.fail("Could not pull file names in all modes", e);
        }
    }

    @Test
    void getImageFilesBytes() {
        File isoFile = new File("./test_isos/rocky.iso");
        File tempDir = null;
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            tempDir = File.createTempFile(Long.toString(System.currentTimeMillis()), "");
            tempDir.deleteOnExit();
            System.out.println("Creating temp folder: " + tempDir.getAbsolutePath());
            if (!tempDir.delete() || !tempDir.mkdirs()) {
                Assertions.fail("Could not make working directory");
            }
            IsoFormatDirectoryRecord[] records = iso.getAllFileRecordsInIsoRaw();
            Assertions.assertNotNull(records);

            RandomAccessFile rawIso = iso.getRawIso();
            for (IsoFormatDirectoryRecord singleRecord : records) {
                if (singleRecord.isDirectory()) {
                    continue;
                }
                System.out.println("Writing: " + singleRecord.getParent() + File.separatorChar
                        + singleRecord.getFileIdentifierAsString());

                File writingFile = new File(tempDir.getPath()
                        + singleRecord.getParent()
                        + File.separatorChar
                        + singleRecord.getFileIdentifierAsString());

                // This conversion to int will fail at files over 2gb
                long dataLength = singleRecord.getDataLengthAsLong();
                rawIso.seek(singleRecord.getLocOfExtAsLong() * IsoFormatConstant.BYTES_PER_SECTOR);
                byte[] rawData = new byte[(int) dataLength];
                Assertions.assertEquals(rawIso.read(rawData, 0, (int) dataLength), dataLength);

                if (!writingFile.getParentFile().exists()) {
                    if (!writingFile.getParentFile().mkdirs()) {
                        Assertions.fail("Could not create folder for files");
                    }
                }
                FileOutputStream outputStream = new FileOutputStream(writingFile);
                outputStream.write(rawData);
                outputStream.flush();
                outputStream.close();
            }
            rawIso.close();
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
        Assertions.assertNotNull(tempDir);

        // TODO(#666): fix this, it may have only been on official centos disk //checkFolderMd5(tempDir);
    }

    @Test
    void getBestModeForIso() {
        File isoFile = new File("./test_isos/rocky.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            AbstractVolumeDescriptor[] headers = iso.getTraditionalIsoReader().getVolumeDescriptors();
            Assertions.assertFalse(iso.isUdfModeInUse());
            System.out.println(
                    "Best iso file name mode: " + iso.getTraditionalIsoReader().getTableOfContentsInUse());
            boolean usingEnhancedDescriptors =
                    headers[iso.getTraditionalIsoReader().getTableOfContentsInUse()].getVolumeDescriptorTypeAsInt() > 1;
            System.out.println("Enhanced Descriptors: " + usingEnhancedDescriptors);
            System.out.println(
                    "Rock Ridge          : " + iso.getTraditionalIsoReader().isUseRockRidgeOverStandard());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void workingWithExtendedAttributesImageFiles() {
        File isoFile = new File("./test_isos/rocky.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            IsoFormatInternalDataFile[] records = iso.getAllFilesAsIsoFormatInternalDataFile();
            Assertions.assertNotNull(records);
            List<GenericInternalIsoFile> files = iso.convertTreeFilesToFlatList(records);
            for (GenericInternalIsoFile file : files) {
                if (file instanceof IsoFormatInternalDataFile) {
                    // This should always be true with this image
                    IsoFormatInternalDataFile internalDataFile = (IsoFormatInternalDataFile) file;
                    System.out.println(internalDataFile.getFileName());
                    System.out.println(
                            internalDataFile.getUnderlyingRecord().get().getFileFlags());
                    // I have yet to find a traditional ISO with a file flag of 4 to have permissions on that file
                } else {
                    Assertions.fail();
                }
            }
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getFileData() {
        File isoFile = new File("./test_isos/rocky.iso");
        IsoFormatInternalDataFile[] records = null;
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            Assertions.assertFalse(iso.isUdfModeInUse());
            records = iso.getAllFilesAsIsoFormatInternalDataFile();
        } catch (IOException e) {
            Assertions.fail(e);
        }
        Assertions.assertNotNull(records);
        for (IsoFormatInternalDataFile fileInIso : records) {
            if (fileInIso.getFullFileName(File.separatorChar).equals("/GPL")) {
                try (IsoFileReader iso = new IsoFileReader(isoFile)) {
                    byte[] data = iso.getFileBytes(fileInIso);
                    System.out.println(new String(data, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    Assertions.fail(e);
                }
            }
        }
    }

    private boolean compareToPreviousLibrary(File iso, boolean tryWorseTables) {
        try (IsoFileReader isoImage = new IsoFileReader(iso)) {
            isoImage.setUdfModeInUse(false); // We do not want to allow Udf if it exists on image
            isoImage.findOptimalSettings();
            Iso9660FileSystem discFs = new Iso9660FileSystem(iso, true);
            // To be compatible with the old system, we need to use the enhanced table, but only the first version, if
            // not then the primary table
            if (tryWorseTables) {
                AbstractVolumeDescriptor[] headers =
                        isoImage.getTraditionalIsoReader().getVolumeDescriptors();
                int headerLoc = findBestWorstTable(headers);
                isoImage.getTraditionalIsoReader().setTableOfContentsInUse(headerLoc);
            }

            GenericInternalIsoFile[] directoryRecord = isoImage.getAllFiles();
            for (Iso9660FileEntry singleFile : discFs) {
                if (singleFile.isDirectory()) {
                    // We dont need to compare data for a folder
                    continue;
                }

                Optional<GenericInternalIsoFile> matchingRecord =
                        isoImage.getSpecificFileByName(directoryRecord, translateFileNameForOtherLibrary(singleFile));
                String path = singleFile.getPath();
                if (matchingRecord.isEmpty()) {
                    if (tryWorseTables) {
                        Assertions.fail("Could not find : " + path + " that exists in java iso tools");
                        return false;
                    } else {
                        // Some of the images need to use worse tables to work with the other library, if we cant find
                        // a file, lets fall over tables and try again
                        System.out.println(
                                "Could not find : " + path + " that exists in java iso tools, trying worse tables");
                        return compareToPreviousLibrary(iso, true);
                    }
                }

                if (matchingRecord.get().getSize() == singleFile.getSize()) {
                    System.out.print("Match found for: " + matchingRecord.get().getFullFileName(File.separatorChar)
                            + ", scanning...");
                    if (compareLibraryData(singleFile, discFs, matchingRecord.get(), isoImage)) {
                        System.out.println(" file data matched!");
                    } else {
                        Assertions.fail("Files data did not match");
                        return false;
                    }
                } else {
                    Assertions.fail("Could not find file that matched with the same size for "
                            + matchingRecord.get().getParent()
                            + File.separatorChar + matchingRecord.get().getFileName());
                }
            }
            discFs.close();
        } catch (IOException | UdfFormatException e) {
            Assertions.fail(e);
            return false;
        }
        return true;
    }

    private String translateFileNameForOtherLibrary(Iso9660FileEntry singleFile) {
        String searchingForFileName = singleFile.getPath();
        if (searchingForFileName.isBlank()) {
            searchingForFileName = singleFile.getName(); // java iso tools treats
            // root folder as a blank
            // name
        } else {
            if (singleFile.isDirectory() && searchingForFileName.endsWith("/")) {
                searchingForFileName = searchingForFileName.substring(0, searchingForFileName.length() - 1);
                // They put a '/' at the end
            }
        }
        return searchingForFileName;
    }

    private int findBestWorstTable(AbstractVolumeDescriptor[] headers) {
        int primaryTableHeader = -1;
        int v1EnhancedHeader = -1;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].getVolumeDescriptorTypeAsInt() == 1 && primaryTableHeader == -1) {
                primaryTableHeader = i;
                continue;
            }

            if (headers[i].getVolumeDescriptorTypeAsInt() == 2 && headers[i].getVolumeDescriptorVersion() == 1) {
                v1EnhancedHeader = i;
            }
        }
        if (v1EnhancedHeader != -1) {
            return v1EnhancedHeader;
        }
        return primaryTableHeader;
    }

    private boolean compareLibraryData(
            Iso9660FileEntry oldLibrary,
            Iso9660FileSystem discFs,
            GenericInternalIsoFile newLibrary,
            IsoFileReader isoImage)
            throws IOException {
        InputStream oldLibraryInput = discFs.getInputStream(oldLibrary);
        RandomAccessFile newLibraryInput = isoImage.getRawIsoWithAutoClose();
        System.out.println("New System Seeking Logical Sector: " + newLibrary.getLogicalSectorLocation());
        newLibraryInput.seek(newLibrary.getLogicalSectorLocation() * 2048);
        long posToEnd = oldLibrary.getSize();
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];
        int buf1ReadLength;
        int buf2ReadLength;
        while (posToEnd > 0) {
            if (posToEnd >= 2048) {
                buf1ReadLength = newLibraryInput.read(buffer1, 0, 2048);
                buf2ReadLength = oldLibraryInput.read(buffer2, 0, 2048);
                posToEnd -= 2048;
            } else {
                buf1ReadLength = newLibraryInput.read(buffer1, 0, (int) posToEnd);
                buf2ReadLength = oldLibraryInput.read(buffer2, 0, (int) posToEnd);
                posToEnd -= (int) posToEnd;
            }
            if (!Arrays.equals(buffer1, buffer2) || buf2ReadLength != buf1ReadLength) {
                Assertions.fail("Files read with different data at POS: " + (oldLibrary.getSize() - posToEnd) + "ish ");
                return false;
            }
        }
        oldLibraryInput.close();
        newLibraryInput.close();
        return true;
    }

    @Test
    void getAllImagesAndTest() {
        File folderOfImages = new File("./test_isos/");
        File[] allImageFiles = folderOfImages.listFiles();
        Assertions.assertNotNull(allImageFiles);
        for (File singleFile : allImageFiles) {
            // TODO(#): We should see if the disk is UDF, if so, skip
            if (!singleFile.getName().endsWith("iso") || "windows.iso".equals(singleFile.getName())) {
                continue;
            }
            System.out.println("Comparing Image: " + singleFile.getName());
            compareToPreviousLibrary(singleFile, false);
        }
    }

    @Test
    void getSpecificFileMd5() {
        File isoFile = new File("./test_isos/rocky.iso");
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            GenericInternalIsoFile[] files = iso.getAllFiles();
            Assertions.assertNotNull(files);
            Optional<GenericInternalIsoFile> bootWim = iso.getSpecificFileByName(files, "/images/install.img");
            Assertions.assertTrue(bootWim.isPresent());
            System.out.println(bootWim.get().getFullFileName(File.separatorChar));
            System.out.println("Starting Sector for Data " + bootWim.get().getLogicalSectorLocation());
            System.out.println("File Size: " + bootWim.get().getSize());
            InputStream bootWimFileStream = iso.getFileStream(bootWim.get());
            String generatedMd5 = Helpers.getMd5FromStream(bootWimFileStream);

            Assertions.assertEquals("5e4052974afb36003d35170a9fba5c22", generatedMd5);
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }
}
