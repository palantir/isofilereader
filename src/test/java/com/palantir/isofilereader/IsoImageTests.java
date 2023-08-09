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

import com.palantir.isofilereader.isofilereader.IsoFileReader;
import com.palantir.isofilereader.isofilereader.iso.IsoFormatInternalDataFile;
import com.palantir.isofilereader.isofilereader.iso.types.AbstractVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IsoImageTests {

    @Test
    void getImageHeader() {
        File isoFile = new File("./src/test/resources/small.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            AbstractVolumeDescriptor[] headers = iso.getTraditionalIsoReader().getVolumeDescriptors();
            Assertions.assertNotNull(headers);
            Assertions.assertNotEquals(headers.length, 0);
        } catch (Exception e) {
            Assertions.fail("Could not get header");
        }
    }

    @Test
    void getImageHeaders() {
        File isoFile = new File("./src/test/resources/small.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            AbstractVolumeDescriptor[] header = iso.getTraditionalIsoReader().getVolumeDescriptors();
            Assertions.assertNotNull(header);
        } catch (Exception e) {
            Assertions.fail("Could not get header");
        }
    }

    @Test
    void getProcessedImageFiles() {
        File isoFile = new File("./src/test/resources/small.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            IsoFormatInternalDataFile[] records = iso.getAllFilesAsIsoFormatInternalDataFile();
            Assertions.assertNotNull(records);
            Helpers.treePrint(iso.getSeparatorChar(), records);
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getImageFiles() {
        File isoFile = new File("./src/test/resources/small.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            IsoFormatDirectoryRecord[] records = iso.getAllFileRecordsInIsoRaw();
            Assertions.assertNotNull(records);
            for (IsoFormatDirectoryRecord singleRecord : records) {
                System.out.print(singleRecord.getParent());
                System.out.println(File.separatorChar + singleRecord.getFileIdentifierAsString());
            }
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getImageFilesBytes() {
        File isoFile = new File("./src/test/resources/small.iso");
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

                long dataLength = singleRecord.getDataLengthAsLong();
                rawIso.seek(singleRecord.getLocOfExtAsLong() * IsoFormatConstant.BYTES_PER_SECTOR);
                byte[] rawData = new byte[(int) dataLength];
                Assertions.assertEquals((int) dataLength, rawIso.read(rawData, 0, (int) dataLength));

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
        checkFolderMd5(tempDir);
    }

    @SuppressWarnings("StringSplitter")
    private void checkFolderMd5(File folder) {
        for (File subFile : Objects.requireNonNull(folder.listFiles())) {
            if (subFile.isDirectory()) {
                checkFolderMd5(subFile);
                continue;
            }
            if (subFile.isFile() && subFile.getName().endsWith("md5")) {
                String md5Info = null;
                try {
                    md5Info = Files.readString(subFile.toPath());
                } catch (IOException e) {
                    Assertions.fail("Failed to read md5 info", e);
                }
                Assertions.assertNotNull(md5Info);
                Assertions.assertFalse(md5Info.isBlank());

                String newFileName = subFile.getAbsolutePath().replace(".md5", ".txt");
                File dataFile = new File(newFileName);
                if (!dataFile.exists()) {
                    newFileName = subFile.getAbsolutePath().replace(".md5", ".dat");
                    dataFile = new File(newFileName);
                    if (!dataFile.exists()) {
                        Assertions.fail("Could not find data file");
                    }
                }

                // If we are here we have the file
                String newlyCreatedMd5 = getMD5StringFromFile(dataFile);

                System.out.println(subFile.getName() + " hash should be: " + md5Info.split("=")[1].trim());
                System.out.println(subFile.getName() + " hash is       : " + newlyCreatedMd5);
                Assertions.assertEquals(md5Info.split("=")[1].trim(), newlyCreatedMd5);
            }
        }
    }

    private String getMD5StringFromFile(File dataFile) {
        String newlyCreatedMd5 = "";
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(dataFile))) {
            MessageDigest complete = MessageDigest.getInstance("MD5");
            final byte[] data = new byte[1024];
            int count;

            while ((count = in.read(data, 0, 1024)) != -1) {
                complete.update(data, 0, count);
            }
            BigInteger bigInt = new BigInteger(1, complete.digest());
            StringBuilder hashText = new StringBuilder(bigInt.toString(16));
            // https://stackoverflow.com/questions/415953/how-can-i-generate-an-md5-hash
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            newlyCreatedMd5 = hashText.toString();
        } catch (Exception e) {
            Assertions.fail(e);
        }
        return newlyCreatedMd5;
    }

    @Test
    @SuppressWarnings("StrictUnusedVariable")
    void getInternalFileData() {
        File isoFile = new File("./src/test/resources/small.iso");
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            IsoFormatInternalDataFile[] isoInternalDataFile = iso.getAllFilesAsIsoFormatInternalDataFile();
            Assertions.assertNotNull(isoInternalDataFile);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
