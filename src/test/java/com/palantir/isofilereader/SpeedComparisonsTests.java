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

import com.github.stephenc.javaisotools.loopfs.iso9660.Iso9660FileSystem;
import com.palantir.isofilereader.isofilereader.GenericInternalIsoFile;
import com.palantir.isofilereader.isofilereader.IsoFileReader;
import com.palantir.isofilereader.isofilereader.IsoInputStream;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import com.palantir.isofilereader.isofilereader.udf.UdfFormatException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpeedComparisonsTests {
    private static final int TEST_COUNT = 2;

    @Test
    void speedComparisons() throws Exception {
        String[] filesToGet = {
            "/isolinux/initrd.img", "/isolinux/vmlinuz", "/.treeinfo", "/images/install.img", "/images/efiboot.img"
        };
        String[] md5s = {
            "5f755a14382fd2c11a4043d5edcef81b", "059191ac9cfcc754590b581cf84fa7cd",
            "582387f8d1b7a3769340393ad39d868d", "5e4052974afb36003d35170a9fba5c22",
            "24ea2b9372aec2e9eeb8e42f73a22b46"
        };

        File isoFile = new File("./test_isos/rocky.iso");
        long[] timings;
        long timingAverage;
        long finalAverage;
        System.out.println("Starting tests:");

        // This can be used to test against files on disk
        // timings = testPureFileTimings(isoFile, filesToGet, md5s);
        // timingAverage = 0;
        // for (long timing3 : timings) {
        //     timingAverage += timing3;
        // }
        // finalAverage = timingAverage / timings.length;
        // System.out.println("Pure File: " + finalAverage);
        //
        // System.gc();

        // Old Library
        timings = oldLibTest(isoFile, filesToGet, md5s);
        timingAverage = 0;
        for (long timing2 : timings) {
            timingAverage += timing2;
        }
        finalAverage = timingAverage / timings.length;
        System.out.println("Old Lib: " + finalAverage);

        System.gc();

        timings = newLibTestMethod1(isoFile, filesToGet, md5s);
        timingAverage = 0;
        for (long timing1 : timings) {
            timingAverage += timing1;
        }
        finalAverage = timingAverage / timings.length;
        System.out.println("New Lib, Method 1: " + finalAverage);

        System.gc();

        timings = newLibTestMethod2(isoFile, filesToGet, md5s);
        timingAverage = 0;
        for (long element : timings) {
            timingAverage += element;
        }
        finalAverage = timingAverage / timings.length;
        System.out.println("New Lib, Method 2: " + finalAverage);

        System.gc();

        getMethod3IVs(isoFile, filesToGet);

        System.gc();

        timings = newLibTestMethod3(isoFile, md5s);
        timingAverage = 0;
        for (long item : timings) {
            timingAverage += item;
        }
        finalAverage = timingAverage / timings.length;
        System.out.println("New Lib, Method 3 (IVs, shared RAF): " + finalAverage);

        System.gc();

        timings = newLibTestMethod4(isoFile, md5s);
        timingAverage = 0;
        for (long value : timings) {
            timingAverage += value;
        }
        finalAverage = timingAverage / timings.length;
        System.out.println("New Lib, Method 4 (IVs, Independent Files): " + finalAverage);

        System.gc();

        timings = newLibTestMethod5(isoFile, md5s);
        timingAverage = 0;
        for (long l : timings) {
            timingAverage += l;
        }
        finalAverage = timingAverage / timings.length;
        System.out.println("New Lib, Method 5 (IVs, Streams, shared RAF): " + finalAverage);

        System.gc();

        timings = newLibTestMethod6(isoFile, md5s);
        timingAverage = 0;
        for (long timing : timings) {
            timingAverage += timing;
        }
        finalAverage = timingAverage / timings.length;
        System.out.println("New Lib, Method 6 (IVs, Streams, Independent Files): " + finalAverage);
    }

    @SuppressWarnings("UnusedMethod")
    private long[] testPureFileTimings(File isoFile, String[] filesToGet, String[] md5s) throws Exception {
        List<File> tempFiles = new ArrayList<>();
        try (IsoFileReader iso = new IsoFileReader(isoFile, "0,1,0")) {
            GenericInternalIsoFile[] files = iso.getAllFiles();

            RandomAccessFile rawIso = iso.getRawIsoWithAutoClose();
            List<GenericInternalIsoFile> flatFiles = iso.convertTreeFilesToFlatList(files);

            flatFiles.forEach(n -> {
                String filename = n.getFullFileName('/');
                if (Arrays.asList(filesToGet).contains(filename)) {
                    try {
                        InputStream isoIn = iso.getFileStream(rawIso, n);
                        File tempFile =
                                Files.createTempFile("image_test", ".bin").toFile();
                        Files.copy(isoIn, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        tempFiles.add(tempFile);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (Exception e) {
            Assertions.fail("Could not get header", e);
        }

        long[] pureFileTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < pureFileTimings.length; i++) {
            startTime = System.currentTimeMillis();
            for (File singleTempFile : tempFiles) {
                String md5 = getMD5Hash(new FileInputStream(singleTempFile));
                Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
            }
            pureFileTimings[i] = System.currentTimeMillis() - startTime;
        }

        for (File singleTempFile : tempFiles) {
            singleTempFile.delete();
        }
        return pureFileTimings;
    }

    private long[] oldLibTest(File isoFile, String[] filesToGet, String[] md5s) {
        long[] oldTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < oldTimings.length; i++) {
            startTime = System.currentTimeMillis();
            AtomicInteger filesFound = new AtomicInteger();
            try (Iso9660FileSystem discFs = new Iso9660FileSystem(isoFile, true)) {
                discFs.forEach(n -> {
                    if (Arrays.asList(filesToGet).contains("/" + n.getPath())) {
                        InputStream isoIn = discFs.getInputStream(n);
                        try {
                            String md5 = getMD5Hash(isoIn);
                            Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
                            filesFound.getAndIncrement();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Assertions.assertEquals(filesToGet.length, filesFound.get());
            oldTimings[i] = System.currentTimeMillis() - startTime;
        }
        return oldTimings;
    }

    private long[] newLibTestMethod1(File isoFile, String[] filesToGet, String[] md5s) {
        long[] newTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < newTimings.length; i++) {
            startTime = System.currentTimeMillis();
            @SuppressWarnings("for-rollout:UnnecessaryAsync")
            AtomicInteger filesFound = new AtomicInteger();
            // I am using precomputed settings for this image, because the other library doesnt even check for those,
            // so its only fair that I dont have to take the scan time into account!
            try (IsoFileReader iso = new IsoFileReader(isoFile, "0,1,0")) {
                GenericInternalIsoFile[] files = iso.getAllFiles();

                RandomAccessFile rawIso = iso.getRawIsoWithAutoClose();
                for (String stringOfFileToFind : filesToGet) {
                    Optional<GenericInternalIsoFile> foundFile = iso.getSpecificFileByName(files, stringOfFileToFind);
                    Assertions.assertTrue(foundFile.isPresent());
                    IsoInputStream isoInputStream = new IsoInputStream(
                            rawIso,
                            foundFile.get().getLogicalSectorLocation() * IsoFormatConstant.BYTES_PER_SECTOR,
                            foundFile.get().getSize());
                    String md5 = getMD5Hash(isoInputStream);
                    Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
                    filesFound.getAndIncrement();
                }
            } catch (Exception e) {
                Assertions.fail("Could not get header", e);
            }
            Assertions.assertEquals(filesToGet.length, filesFound.get());
            newTimings[i] = System.currentTimeMillis() - startTime;
        }
        return newTimings;
    }

    private long[] newLibTestMethod2(File isoFile, String[] filesToGet, String[] md5s) {
        long[] newTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < newTimings.length; i++) {
            startTime = System.currentTimeMillis();
            AtomicInteger filesFound = new AtomicInteger();
            try (IsoFileReader iso = new IsoFileReader(isoFile, "0,1,0")) {
                GenericInternalIsoFile[] files = iso.getAllFiles();

                RandomAccessFile rawIso = iso.getRawIsoWithAutoClose();
                List<GenericInternalIsoFile> flatFiles = iso.convertTreeFilesToFlatList(files);

                flatFiles.forEach(n -> {
                    String filename = n.getFullFileName('/');
                    if (Arrays.asList(filesToGet).contains(filename)) {
                        try {
                            InputStream isoIn = iso.getFileStream(rawIso, n);
                            String md5 = getMD5Hash(isoIn);
                            Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
                            filesFound.getAndIncrement();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (Exception e) {
                Assertions.fail("Could not get header", e);
            }
            Assertions.assertEquals(filesToGet.length, filesFound.get());
            newTimings[i] = System.currentTimeMillis() - startTime;
        }
        return newTimings;
    }

    private void getMethod3IVs(File isoFile, String[] filesToGet) throws IOException {
        try (IsoFileReader isoFileReader = new IsoFileReader(isoFile)) {
            System.out.println("Image IV: " + isoFileReader.getInitializationVectorForImage());

            GenericInternalIsoFile[] internalFiles = isoFileReader.getAllFiles();
            for (String file : filesToGet) {
                Optional<GenericInternalIsoFile> foundFile = isoFileReader.getSpecificFileByName(internalFiles, file);
                if (foundFile.isPresent()) {
                    System.out.println("File IV: " + isoFileReader.getFileInitializationVectorForFile(foundFile.get()));
                } else {
                    Assertions.fail("Could not find file");
                }
            }
        } catch (NoSuchAlgorithmException | UdfFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private long[] newLibTestMethod3(File isoFile, String[] md5s) throws FileNotFoundException {
        long[] newTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < newTimings.length; i++) {
            startTime = System.currentTimeMillis();
            @SuppressWarnings("for-rollout:UnnecessaryAsync")
            AtomicInteger filesFound = new AtomicInteger();

            String imageIv = "I1|2048|10|1610940416|750c30c146c68b40dc812b4f9522a23c";
            String[] filesIv = {
                "F1|2048|4|100358364|386052|/isolinux/initrd.img|446aea6f7e4e2342e338fa61e160c8d0",
                "F1|2048|4|12182792|435056|/isolinux/vmlinuz|1593e0e4d3554f5db52fedbe2f24c402",
                "F1|2048|4|1500|441260|/.treeinfo|86b2c63b1e47ed585759b9005c6885e1",
                "F1|2048|4|776253440|7022|/images/install.img|862761503bb35f58fd5e8cfcbde4d834",
                "F1|2048|4|7264256|166|/images/efiboot.img|4577102270ea904d709f913146cfb782"
            };

            for (String fileIv : filesIv) {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(isoFile, "r")) {
                    Optional<byte[]> data = IsoFileReader.getFileDataWithIVs(randomAccessFile, imageIv, fileIv);
                    if (data.isPresent()) {
                        String md5 = getMD5Hash(data.get());
                        Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
                        filesFound.getAndIncrement();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Assertions.assertEquals(filesIv.length, filesFound.get());
            newTimings[i] = System.currentTimeMillis() - startTime;
        }
        return newTimings;
    }

    private long[] newLibTestMethod4(File isoFile, String[] md5s) throws FileNotFoundException {
        long[] newTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < newTimings.length; i++) {
            startTime = System.currentTimeMillis();
            @SuppressWarnings("for-rollout:UnnecessaryAsync")
            AtomicInteger filesFound = new AtomicInteger();

            String imageIv = "I1|2048|10|1610940416|750c30c146c68b40dc812b4f9522a23c";
            String[] filesIv = {
                "F1|2048|4|100358364|386052|/isolinux/initrd.img|446aea6f7e4e2342e338fa61e160c8d0",
                "F1|2048|4|12182792|435056|/isolinux/vmlinuz|1593e0e4d3554f5db52fedbe2f24c402",
                "F1|2048|4|1500|441260|/.treeinfo|86b2c63b1e47ed585759b9005c6885e1",
                "F1|2048|4|776253440|7022|/images/install.img|862761503bb35f58fd5e8cfcbde4d834",
                "F1|2048|4|7264256|166|/images/efiboot.img|4577102270ea904d709f913146cfb782"
            };

            for (String fileIv : filesIv) {
                try {
                    Optional<byte[]> data = IsoFileReader.getFileDataWithIVsFromFile(isoFile, imageIv, fileIv);
                    if (data.isPresent()) {
                        String md5 = getMD5Hash(data.get());
                        Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
                        filesFound.getAndIncrement();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Assertions.assertEquals(filesIv.length, filesFound.get());
            newTimings[i] = System.currentTimeMillis() - startTime;
        }
        return newTimings;
    }

    private long[] newLibTestMethod5(File isoFile, String[] md5s) throws FileNotFoundException {
        long[] newTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < newTimings.length; i++) {
            startTime = System.currentTimeMillis();
            @SuppressWarnings("for-rollout:UnnecessaryAsync")
            AtomicInteger filesFound = new AtomicInteger();

            String imageIv = "I1|2048|10|1610940416|750c30c146c68b40dc812b4f9522a23c";
            String[] filesIv = {
                "F1|2048|4|100358364|386052|/isolinux/initrd.img|446aea6f7e4e2342e338fa61e160c8d0",
                "F1|2048|4|12182792|435056|/isolinux/vmlinuz|1593e0e4d3554f5db52fedbe2f24c402",
                "F1|2048|4|1500|441260|/.treeinfo|86b2c63b1e47ed585759b9005c6885e1",
                "F1|2048|4|776253440|7022|/images/install.img|862761503bb35f58fd5e8cfcbde4d834",
                "F1|2048|4|7264256|166|/images/efiboot.img|4577102270ea904d709f913146cfb782"
            };

            for (String fileIv : filesIv) {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(isoFile, "r")) {
                    Optional<InputStream> data =
                            IsoFileReader.getFileDataAsStreamWithIVs(randomAccessFile, imageIv, fileIv);
                    if (data.isPresent()) {
                        String md5 = getMD5Hash(data.get());
                        Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
                        filesFound.getAndIncrement();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Assertions.assertEquals(filesIv.length, filesFound.get());
            newTimings[i] = System.currentTimeMillis() - startTime;
        }
        return newTimings;
    }

    private long[] newLibTestMethod6(File isoFile, String[] md5s) throws FileNotFoundException {
        long[] newTimings = new long[TEST_COUNT];
        long startTime;

        for (int i = 0; i < newTimings.length; i++) {
            startTime = System.currentTimeMillis();
            @SuppressWarnings("for-rollout:UnnecessaryAsync")
            AtomicInteger filesFound = new AtomicInteger();
            // RandomAccessFile rawIso = new RandomAccessFile(isoFile, "r");

            String imageIv = "I1|2048|10|1610940416|750c30c146c68b40dc812b4f9522a23c";
            String[] filesIv = {
                "F1|2048|4|100358364|386052|/isolinux/initrd.img|446aea6f7e4e2342e338fa61e160c8d0",
                "F1|2048|4|12182792|435056|/isolinux/vmlinuz|1593e0e4d3554f5db52fedbe2f24c402",
                "F1|2048|4|1500|441260|/.treeinfo|86b2c63b1e47ed585759b9005c6885e1",
                "F1|2048|4|776253440|7022|/images/install.img|862761503bb35f58fd5e8cfcbde4d834",
                "F1|2048|4|7264256|166|/images/efiboot.img|4577102270ea904d709f913146cfb782"
            };

            for (String fileIv : filesIv) {
                try {
                    Optional<InputStream> data =
                            IsoFileReader.getFileDataAsStreamWithIVsFromFile(isoFile, imageIv, fileIv);
                    if (data.isPresent()) {
                        String md5 = getMD5Hash(data.get());
                        Assertions.assertTrue(Arrays.asList(md5s).contains(md5));
                        filesFound.getAndIncrement();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Assertions.assertEquals(filesIv.length, filesFound.get());
            newTimings[i] = System.currentTimeMillis() - startTime;
        }
        return newTimings;
    }

    private static String getMD5Hash(InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = inputStream.read(byteArray)) != -1) {
            md.update(byteArray, 0, bytesCount);
        }

        byte[] bytes = md.digest();
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private static String getMD5Hash(byte[] inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(inputStream);

        byte[] bytes = md.digest();
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}
