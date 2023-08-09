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

import com.palantir.isofilereader.isofilereader.GenericInternalIsoFile;
import com.palantir.isofilereader.isofilereader.IsoFileReader;
import com.palantir.isofilereader.isofilereader.udf.UdfFormatException;
import com.palantir.isofilereader.isofilereader.udf.UdfInternalDataFile;
import com.palantir.isofilereader.isofilereader.udf.types.toc.GenericDescriptor;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("StrictUnusedVariable")
public class UdfImageTests {

    @Test
    void enablesUdfMode() {
        File isoFile = new File("./test_isos/windows.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            Assertions.assertTrue(iso.isUdfModeInUse());
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getAllFilesAndTestReadMicrosoft() {
        File isoFile = new File("./test_isos/windows.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            Assertions.assertTrue(iso.isUdfModeInUse());

            GenericInternalIsoFile[] files = iso.getAllFiles();
            Helpers.treePrint(File.separatorChar, files);
            Assertions.assertTrue(files[0].getChildren()[0] instanceof UdfInternalDataFile);
            if (files[0].getChildren()[0] instanceof UdfInternalDataFile) {
                System.out.println("UDF");
            } else {
                System.out.println("ISO");
            }
            System.out.println("Parent no matter format: " + files[0].getChildren()[0].getFileName());

            System.out.println("Length: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getLengthInAllocationDescriptorAsInt());
            System.out.println("Loc: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getLocationInAllocationDescriptorAsInt());

            System.out.println("File Permissions: ");
            System.out.println("User ID: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getUidAsInt());
            System.out.println("Group ID: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getGidAsInt());
            // File Permissions are 13741
            System.out.println("Other Can Execute: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canOtherExecute());
            System.out.println("Other Can Read: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canOtherRead());
            System.out.println("Other Can Write: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canOtherWrite());
            System.out.println("Other Can Change Attributes: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canOtherChangeAttributes());
            System.out.println("Other Can Delete: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canOtherDelete());
            System.out.println("Group Can Execute: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canGroupExecute());
            System.out.println("Group Can Read: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canGroupRead());
            System.out.println("Group Can Write: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canGroupWrite());
            System.out.println("Group Can Change Attributes: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canGroupChangeAttributes());
            System.out.println("Group Can Delete: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canGroupDelete());
            System.out.println("User Can Execute: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canUserExecute());
            System.out.println("User Can Read: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canUserRead());
            System.out.println("User Can Write: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canUserWrite());
            System.out.println("User Can Change Attributes: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canUserChangeAttributes());
            System.out.println("User Can Delete: "
                    + ((UdfInternalDataFile) files[0].getChildren()[0])
                            .getThisFileEntry()
                            .getFilePerms()
                            .canUserDelete());

            byte[] dataFromFile = iso.getFileBytes(files[0].getChildren()[0]);
            String test = new String(dataFromFile, StandardCharsets.UTF_8);
            System.out.println(test);
            String correctText = "[AutoRun.Amd64]\r\n"
                    + "open=setup.exe\r\n"
                    + "icon=setup.exe,0\r\n"
                    + "\r\n"
                    + "[AutoRun]\r\n"
                    + "open=sources\\SetupError.exe x64\r\n"
                    + "icon=sources\\SetupError.exe,0\r\n";
            Assertions.assertEquals(correctText, test);
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getAllFilesAndTestRead() {
        File isoFile = new File("./src/test/resources/small_only_udf_260.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            Assertions.assertTrue(iso.isUdfModeInUse());

            GenericInternalIsoFile[] files = iso.getAllFiles();
            Helpers.treePrint(File.separatorChar, files);
            if (files[0].getChildren()[0] instanceof UdfInternalDataFile) {
                System.out.println("UDF");
            } else {
                System.out.println("ISO");
            }

            GenericInternalIsoFile singleFile = files[0].getChildren()[0].getChildren()[0];
            System.out.println("File we are tracking: " + singleFile);
            System.out.println("Parent no matter format: " + singleFile.getFileName());

            System.out.println("Length: "
                    + ((UdfInternalDataFile) singleFile).getThisFileEntry().getLengthInAllocationDescriptorAsInt());
            System.out.println("Loc: "
                    + ((UdfInternalDataFile) singleFile).getThisFileEntry().getLocationInAllocationDescriptorAsInt());

            byte[] dataFromFile = iso.getFileBytes(singleFile);

            byte[] emptyArray = new byte[(int) singleFile.getSize()];
            InputStream inputStream = iso.getFileStream(singleFile);
            int length = inputStream.read(emptyArray, 0, (int) singleFile.getSize());
            String test = new String(dataFromFile, StandardCharsets.UTF_8);
            System.out.println(test);
            // Stopped here, we need some reader classes added to IsoFileReader to allow common InternalIsoFile fetches
            // Also UdfInternalDataFile needs a lot fo classes implemented
            // Also don't allow getFileBytes to try to pull a folder
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getImageDescriptors() {
        File isoFile = new File("./test_isos/windows.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            List<GenericDescriptor> descriptors = iso.getUdfIsoReader().getDiscDescriptors();
            Assertions.assertNotNull(descriptors);
            Assertions.assertNotEquals(descriptors.size(), 0);
        } catch (Exception e) {
            Assertions.fail("Could not get header");
        }
    }

    @Test
    void getImageDescriptorsWithPrimingReader() {
        File isoFile = new File("./test_isos/windows.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            List<GenericDescriptor> descriptors = iso.getUdfIsoReader().getDiscDescriptors();
            Assertions.assertNotNull(descriptors);
            Assertions.assertNotEquals(descriptors.size(), 0);
        } catch (Exception e) {
            Assertions.fail("Could not get header");
        }
    }

    @Test
    void getProcessedImageFiles() {
        File isoFile = new File("./test_isos/windows.iso");
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            UdfInternalDataFile[] rootFiles = iso.getUdfIsoReader().getAllFiles();
            Assertions.assertNotNull(rootFiles);
            Helpers.treePrint(iso.getSeparatorChar(), rootFiles);
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getSpecificFiles() {
        File isoFile = new File("./test_isos/windows.iso");
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            GenericInternalIsoFile[] files = iso.getAllFiles();
            Assertions.assertNotNull(files);
            Optional<GenericInternalIsoFile> bootWim = iso.getSpecificFileByName(files, "/sources/boot.wim");
            Assertions.assertTrue(bootWim.isPresent());
            System.out.println(bootWim.get().getFullFileName(File.separatorChar));
            System.out.println("Starting Sector for Data " + bootWim.get().getLogicalSectorLocation());
            System.out.println("File Size: " + bootWim.get().getSize());
            InputStream bootWimFileStream = iso.getFileStream(bootWim.get());

            File tempo = Files.createTempFile("outputFile", ".wim").toFile();

            System.out.println("Output temp: " + tempo.getAbsolutePath());
            byte[] data = bootWimFileStream.readAllBytes();
            Files.write(tempo.toPath(), data);

            Assertions.assertEquals(673876802, tempo.length());

            String getMd5 = getMD5StringFromFile(tempo);
            Assertions.assertEquals("dccd2c03c8834f7c6548a41926dda178", getMd5);
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void transferToTest() {
        File isoFile = new File("./test_isos/windows.iso");
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            GenericInternalIsoFile[] files = iso.getAllFiles();
            Assertions.assertNotNull(files);
            Optional<GenericInternalIsoFile> bootWim = iso.getSpecificFileByName(files, "/sources/boot.wim");
            Assertions.assertTrue(bootWim.isPresent());
            InputStream bootWimFileStream = iso.getFileStream(bootWim.get());

            File tempo = Files.createTempFile("outputFile", ".wim").toFile();
            System.out.println("Output temp: " + tempo.getAbsolutePath());
            FileOutputStream fileOutputStream = new FileOutputStream(tempo);
            bootWimFileStream.transferTo(fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException | UdfFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getSpecificFileMd5() {
        File isoFile = new File("./test_isos/windows.iso");
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            GenericInternalIsoFile[] files = iso.getAllFiles();
            Assertions.assertNotNull(files);
            Optional<GenericInternalIsoFile> bootWim = iso.getSpecificFileByName(files, "/sources/boot.wim");
            Assertions.assertTrue(bootWim.isPresent());

            // The TRUTH
            // Mac in EST says      Sep  6 21:08:39 2019
            // SERVER IN PST says   2019-09-06 18:08:39.433000000 -0700
            // That means UTC should be 2019-09-07 01:08:39
            // The file with no timezone mods says
            // This is passing... On the mac...

            System.out.println(bootWim.get().getFullFileName(File.separatorChar));
            System.out.println("Starting Sector for Data " + bootWim.get().getLogicalSectorLocation());
            System.out.println("File Size: " + bootWim.get().getSize());

            String date = String.valueOf(bootWim.get().getDateAsDate().get());
            System.out.println(date);
            String input = "Thu Sep 08 00:07:42 EDT 2022";
            SimpleDateFormat parser = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");
            Date knownDate = parser.parse(input);
            Assertions.assertEquals(String.valueOf(knownDate), date);
            int timezoneType = ((UdfInternalDataFile) bootWim.get())
                    .getThisFileEntry()
                    .getModificationTime()
                    .getTypeOfTimestamp();
            Assertions.assertEquals(1, timezoneType);
            System.out.println("Timezone Type: " + timezoneType);

            InputStream bootWimFileStream = iso.getFileStream(bootWim.get());
            String generatedMd5 = Helpers.getMd5FromStream(bootWimFileStream);

            Assertions.assertEquals("dccd2c03c8834f7c6548a41926dda178", generatedMd5);
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
    }

    @Test
    void getImageFilesBytes() {
        File isoFile = new File("./src/test/resources/small_only_udf_260.iso");
        File tempDir = null;
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            tempDir = File.createTempFile(Long.toString(System.currentTimeMillis()), "");
            tempDir.deleteOnExit();
            System.out.println("Creating temp folder: " + tempDir.getAbsolutePath());
            if (!tempDir.delete() || !tempDir.mkdirs()) {
                Assertions.fail("Could not make working directory");
            }
            GenericInternalIsoFile[] records = iso.getAllFiles();

            Assertions.assertNotNull(records);

            // Stopped here
            // This needs updated for tree format, and IsoInternalFile needs updated to be a tree instead of array
            for (GenericInternalIsoFile singleRecord : records) {
                if (singleRecord.isDirectory()) {
                    continue;
                }
                System.out.println("Writing: " + singleRecord.getFullFileName(File.separatorChar));

                File writingFile = new File(tempDir.getPath() + singleRecord.getFullFileName(File.separatorChar));

                byte[] rawData = iso.getFileBytes(singleRecord);

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
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }
        Assertions.assertNotNull(tempDir);
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
}
