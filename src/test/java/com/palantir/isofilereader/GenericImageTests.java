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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GenericImageTests {
    @Test
    void genericImageTest() throws IOException, UdfFormatException, NoSuchAlgorithmException {
        File isoFile = new File("./src/test/resources/small_only_udf_260.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            try {
                System.out.println("I-IV: " + iso.getInitializationVectorForImage());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertTrue(iso.isUdfModeInUse());

            GenericInternalIsoFile[] files = iso.getAllFiles();
            Helpers.treePrint(File.separatorChar, files);
            List<GenericInternalIsoFile> flatList = iso.convertTreeFilesToFlatList(files);
            Assertions.assertNotEquals(flatList.size(), 0);
            for (GenericInternalIsoFile cycleFiles : flatList) {
                System.out.println(cycleFiles.getFullFileName(File.separatorChar));
                System.out.println(iso.getFileInitializationVectorForFile(cycleFiles));
            }
        }
    }

    @Test
    void genericInputStreamUdfTest() {
        File isoFile = new File("./src/test/resources/small_only_udf_260.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            GenericInternalIsoFile[] files = iso.getAllFiles();
            Optional<GenericInternalIsoFile> generalFile = iso.getSpecificFileByName(
                    files, "/test/2da3ad96e6cbf41723f9c56d743390412da3ad96e6cbf41723f9c56d7433.txt");
            Assertions.assertTrue(generalFile.isPresent());
            String date = String.valueOf(generalFile.get().getDateAsDate());

            // Mac mounted EST: Nov 21, 2019, 18:08
            // Cali server:     2019-11-21 15:08:37.000000000 -0800
            // Real must be 23:08
            System.out.println(date);
            String input = "Thu Nov 21 23:08:37 UTC 2019";
            SimpleDateFormat parser = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");
            Date knownDate = parser.parse(input);
            System.out.println("Test Date: " + knownDate);
            System.out.println("From File: " + date);
            Assertions.assertEquals(
                    knownDate.toInstant(),
                    generalFile.get().getDateAsDate().get().toInstant());
            Assertions.assertEquals(knownDate, generalFile.get().getDateAsDate().get());
            System.out.println("Time Diff:"
                    + ChronoUnit.SECONDS.between(
                            knownDate.toInstant(),
                            generalFile.get().getDateAsDate().get().toInstant()));

            int timezoneType = ((UdfInternalDataFile) generalFile.get())
                    .getThisFileEntry()
                    .getModificationTime()
                    .getTypeOfTimestamp();
            Assertions.assertEquals(1, timezoneType);
            System.out.println("Timezone Type: " + timezoneType);

            InputStream stream = iso.getFileStream(generalFile.get());
            byte[] array = stream.readAllBytes();

            // This equals "LONG "
            byte[] comparingData = {0x4c, 0x4f, 0x4e, 0x47, 0x0a};
            for (int i = 0; i < generalFile.get().getSize(); i++) {
                Assertions.assertEquals(comparingData[i], array[i]);
            }
            stream.reset();
            for (int i = 0; i < generalFile.get().getSize(); i++) {
                Assertions.assertEquals(comparingData[i], stream.read());
            }
            array = new byte[(int) generalFile.get().getSize()];
            stream.reset();
            int count = stream.read(array);
            Assertions.assertEquals(count, (int) generalFile.get().getSize());
            for (int i = 0; i < generalFile.get().getSize(); i++) {
                Assertions.assertEquals(comparingData[i], array[i]);
            }

            stream.reset();
            Assertions.assertEquals(stream.available(), 5);
            long skipped = stream.skip(2);
            Assertions.assertEquals(skipped, 2);
            Assertions.assertEquals(stream.available(), 3);

            array = new byte[(int) generalFile.get().getSize()];
            stream.reset();
            stream.readNBytes(array, 1, 3);
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(comparingData[i], array[i + 1]);
            }
        } catch (IOException | UdfFormatException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void genericInputStreamIsoTest() {
        File isoFile = new File("./src/test/resources/iso_test.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            GenericInternalIsoFile[] files = iso.getAllFiles();
            Optional<GenericInternalIsoFile> generalFile = iso.getSpecificFileByName(
                    files, "/mactest-mactest-mactest-mactest-mactest-mactest-mactest-mact.txt");
            Assertions.assertTrue(generalFile.isPresent());
            InputStream stream = iso.getFileStream(generalFile.get());
            byte[] array = stream.readAllBytes();

            // This equals "LONG "
            byte[] comparingData = {
                0x20, 0x31, 0x20, 0x3D, 0x20, 0x7B,
                0x49, 0x73, 0x6F, 0x45, 0x6E, 0x68,
                0x61, 0x6E, 0x63, 0x65, 0x64, 0x56
            };
            for (int i = 0; i < comparingData.length; i++) {
                Assertions.assertEquals(comparingData[i], array[i]);
            }
            stream.reset();
            for (int i = 0; i < comparingData.length; i++) {
                Assertions.assertEquals(comparingData[i], stream.read());
            }
            array = new byte[(int) comparingData.length];
            stream.reset();
            int count = stream.read(array);
            Assertions.assertEquals(count, (int) comparingData.length);
            for (int i = 0; i < comparingData.length; i++) {
                Assertions.assertEquals(comparingData[i], array[i]);
            }
            stream.reset();
            ByteArrayOutputStream holdingBuffer = new ByteArrayOutputStream();
            long readData = stream.transferTo(holdingBuffer);
            Assertions.assertEquals(readData, 5566);
            byte[] returnedData = holdingBuffer.toByteArray();
            for (int i = 0; i < comparingData.length; i++) {
                Assertions.assertEquals(comparingData[i], returnedData[i]);
            }
        } catch (IOException | UdfFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void genericFileGetBytesIsoTest() {
        File isoFile = new File("./src/test/resources/iso_test.iso");

        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            GenericInternalIsoFile[] files = iso.getAllFiles();
            Optional<GenericInternalIsoFile> generalFile = iso.getSpecificFileByName(
                    files, "/mactest-mactest-mactest-mactest-mactest-mactest-mactest-mact.txt");
            Assertions.assertTrue(generalFile.isPresent());
            byte[] dataFromFile = iso.getFileBytes(generalFile.get());
            String getDate = String.valueOf(generalFile.get().getDateAsDate().get());
            String input = "Tue May 18 18:21:57 EDT 2021";
            SimpleDateFormat parser = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");
            Date date = parser.parse(input);
            Assertions.assertEquals(String.valueOf(date), getDate);
            // This equals "LONG "
            byte[] comparingData = {
                0x20, 0x31, 0x20, 0x3D, 0x20, 0x7B,
                0x49, 0x73, 0x6F, 0x45, 0x6E, 0x68,
                0x61, 0x6E, 0x63, 0x65, 0x64, 0x56
            };
            for (int i = 0; i < comparingData.length; i++) {
                Assertions.assertEquals(comparingData[i], dataFromFile[i]);
            }
        } catch (IOException | UdfFormatException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
