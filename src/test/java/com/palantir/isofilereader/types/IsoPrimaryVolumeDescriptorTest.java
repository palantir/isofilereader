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

package com.palantir.isofilereader.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatPrimaryVolumeDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IsoPrimaryVolumeDescriptorTest {
    @Test
    void getSystemIdentifier() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String sysIdString = isoPrimaryVolumeDescriptor.getSystemIdentifierAsString();
        Assertions.assertFalse(sysIdString.isBlank());
        System.out.println("System ID: " + sysIdString);
    }

    @Test
    void getVolumeIdentifierAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String sysIdString = isoPrimaryVolumeDescriptor.getVolumeIdentifierAsString();
        Assertions.assertFalse(sysIdString.isBlank());
        System.out.println("Volume ID: " + sysIdString);
    }

    @Test
    void getVolumeSpaceSizeAsLong() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        long sectorsAsLong = isoPrimaryVolumeDescriptor.getVolumeSpaceSizeAsLong();
        assertTrue(sectorsAsLong > 0);
        System.out.println("Volume Space Size: " + sectorsAsLong);
    }

    @Test
    void getVolumeSetSizeAsInt() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        int setSize = isoPrimaryVolumeDescriptor.getVolumeSetSizeAsInt();
        assertTrue(setSize > 0);
        System.out.println("Volume Set Size: " + setSize);
    }

    @Test
    void getVolumeSequenceNumberAsInt() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        int setSize = isoPrimaryVolumeDescriptor.getVolumeSequenceNumberAsInt();
        assertTrue(setSize > 0);
        System.out.println("Volume Seq Num: " + setSize);
    }

    @Test
    void getLogicBlockSizeAsInt() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        System.out.println("Logical Block Size: " + isoPrimaryVolumeDescriptor.getLogicBlockSizeAsInt());
        switch (isoPrimaryVolumeDescriptor.getLogicBlockSizeAsInt()) {
            case 512:
            case 1024:
            case 2048:
            case 4096:
                return;
        }
        fail("Odd block size");
    }

    @Test
    void getPathTableSizeAsLong() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        long pathTableSize = isoPrimaryVolumeDescriptor.getPathTableSizeAsLong();
        assertTrue(pathTableSize > 0);
        System.out.println("Path Table Size: " + pathTableSize);
    }

    @Test
    void getLPathTableLocAsLong() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        long pathTableLoc = isoPrimaryVolumeDescriptor.getLPathTableLocAsLong();
        assertTrue(pathTableLoc > 0);
        System.out.println("L Path Table Loc: " + pathTableLoc);
    }

    @Test
    void getLocOfOptionalLPathTableAsLong() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        long pathTableLoc = isoPrimaryVolumeDescriptor.getLocOfOptionalLPathTableAsLong();
        assertTrue(pathTableLoc > -1);
        System.out.println("Optional L Path Table Loc: " + pathTableLoc);
    }

    @Test
    void getLocOfMPathTableAsLong() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        long pathTableLoc = isoPrimaryVolumeDescriptor.getLocOfMPathTableAsLong();
        assertTrue(pathTableLoc > 0);
        System.out.println("M Path Table Loc: " + pathTableLoc);
    }

    @Test
    void getLocOfOptionalMPathTableAsLong() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        long pathTableLoc = isoPrimaryVolumeDescriptor.getLocOfOptionalMPathTableAsLong();
        assertTrue(pathTableLoc > -1);
        System.out.println("Optional M Path Table Loc: " + pathTableLoc);
    }

    @Test
    void getDirectoryRecordForRootDirectoryAsIsoDirectorRecordLength() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        IsoFormatDirectoryRecord isoDirectoryRecord =
                isoPrimaryVolumeDescriptor.getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
        assertEquals(34, isoDirectoryRecord.getLenDirRecordAsInt());
        System.out.println("Root Directory - Record Length: " + isoDirectoryRecord.getLenDirRecordAsInt());
    }

    @Test
    void getDirectoryRecordForRootDirectoryAsIsoDirectorRecordExtLength() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        IsoFormatDirectoryRecord isoDirectoryRecord =
                isoPrimaryVolumeDescriptor.getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
        System.out.println("Root Directory - Extended Record Length: " + isoDirectoryRecord.getExtAttrRecordLenAsInt());
    }

    @Test
    void getDirectoryRecordForRootDirectoryAsIsoDirectorRecordExtLoc() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        IsoFormatDirectoryRecord isoDirectoryRecord =
                isoPrimaryVolumeDescriptor.getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
        System.out.println("Root Directory - Ext Loc: " + isoDirectoryRecord.getLocOfExtAsLong());
    }

    @Test
    void getDirectoryRecordForRootDirectoryAsIsoDirectorRecordDataLength() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        IsoFormatDirectoryRecord isoDirectoryRecord =
                isoPrimaryVolumeDescriptor.getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
        System.out.println("Root Directory - Data length: " + isoDirectoryRecord.getDataLengthAsLong());
    }

    @Test
    void getDirectoryRecordForRootDirectoryAsIsoDirectorRecordDate() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        IsoFormatDirectoryRecord isoDirectoryRecord =
                isoPrimaryVolumeDescriptor.getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();

        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String stringDate =
                dateFormat.format(isoDirectoryRecord.getDataAndTimeAsDate().get());
        assertEquals("2019/Dec/09 16:53:37", stringDate);
    }

    @Test
    void getDirectoryRecordForRootDirectoryAsIsoDirectorRecordFlags() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        IsoFormatDirectoryRecord isoDirectoryRecord =
                isoPrimaryVolumeDescriptor.getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();

        assertTrue(isoDirectoryRecord.isDirectory());
        assertFalse(isoDirectoryRecord.hasExtendedAttributes());
    }

    @Test
    void getVolumeSetIdentifierAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String volSetId = isoPrimaryVolumeDescriptor.getVolumeSetIdentifierAsString();
        assertNotNull(volSetId);
        System.out.println("Volume Set: " + volSetId);
    }

    @Test
    void getPublisherIdentiferAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String volSetId = isoPrimaryVolumeDescriptor.getPublisherIdentiferAsString();
        assertNotNull(volSetId);
        System.out.println("Publisher Info: " + volSetId);
    }

    @Test
    void getDataPreparerIdentifierAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String volSetId = isoPrimaryVolumeDescriptor.getDataPreparerIdentifierAsString();
        assertNotNull(volSetId);
        System.out.println("DataPreparer: " + volSetId);
    }

    @Test
    void getApplicationIdentifierAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String volSetId = isoPrimaryVolumeDescriptor.getApplicationIdentifierAsString();
        assertNotNull(volSetId);
        System.out.println("ApplicationIdentifier: " + volSetId);
    }

    @Test
    void getCopyrightFileIdentifierAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String volSetId = isoPrimaryVolumeDescriptor.getCopyrightFileIdentifierAsString();
        assertNotNull(volSetId);
        System.out.println("CopyrightFileIdentifier: " + volSetId);
    }

    @Test
    void getAbstractFileIdentifierAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String volSetId = isoPrimaryVolumeDescriptor.getAbstractFileIdentifierAsString();
        assertNotNull(volSetId);
        System.out.println("AbstractFileIdentifier: " + volSetId);
    }

    @Test
    void getBibliographicFileIdentifierAsString() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        String volSetId = isoPrimaryVolumeDescriptor.getBibliographicFileIdentifierAsString();
        assertNotNull(volSetId);
        System.out.println("BibliographicFileIdentifier: " + volSetId);
    }

    @Test
    void getVolumeCreationDataTimeAsDate() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);

        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss:SS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String stringDate = null;
        try {
            stringDate = dateFormat.format(
                    isoPrimaryVolumeDescriptor.getVolumeCreationDataTimeAsDate().get());
        } catch (Exception e) {
            fail(e);
        }
        assertEquals("2019/Dec/09 16:54:11:00", stringDate);
        System.out.println("Volume Creation Date: " + stringDate);
    }

    @Test
    void getVolumeModificationDateTimeAsDate() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        if (Arrays.equals(
                isoPrimaryVolumeDescriptor.getVolumeModificationDateTime(),
                new byte[isoPrimaryVolumeDescriptor.getVolumeModificationDateTime().length])) {
            // Blank array, value not set
            return;
        }

        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss:SS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String stringDate = null;
        try {
            stringDate = dateFormat.format(isoPrimaryVolumeDescriptor
                    .getVolumeModificationDateTimeAsDate()
                    .get());
        } catch (Exception e) {
            fail(e);
        }
        assertEquals("2019/Dec/09 16:54:11:00", stringDate);
        System.out.println("Volume Modification Date: " + stringDate);
    }

    @Test
    void getVolumeExpirationDateTimeAsDate() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        if (Arrays.equals(
                isoPrimaryVolumeDescriptor.getVolumeExpirationDateTime(),
                new byte[isoPrimaryVolumeDescriptor.getVolumeExpirationDateTime().length])) {
            // Blank array, value not set
            return;
        }
        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss:SS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String stringDate = null;
        try {
            stringDate = dateFormat.format(isoPrimaryVolumeDescriptor.getVolumeExpirationDateTimeAsDate());
        } catch (Exception e) {
            fail(e);
        }
        assertEquals("2019/Dec/09 11:54:11:00", stringDate);
        System.out.println("Volume Expiration Date: " + stringDate);
    }

    @Test
    void getVolumeEffectiveDateTimeAsDate() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        if (Arrays.equals(
                isoPrimaryVolumeDescriptor.getVolumeEffectiveDateTime(),
                new byte[isoPrimaryVolumeDescriptor.getVolumeEffectiveDateTime().length])) {
            // Blank array, value not set
            return;
        }

        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss:SS");
        String stringDate = null;
        try {
            stringDate = dateFormat.format(isoPrimaryVolumeDescriptor.getVolumeEffectiveDateTimeAsDate());
        } catch (Exception e) {
            fail(e);
        }
        assertEquals("2019/Nov/21 18:09:51:00", stringDate);
        System.out.println("Volume Effective Date: " + stringDate);
    }

    @Test
    void getFileStructorVersionAsInt() {
        byte[] headerInfo = testFileValidationAndReturnHeader();
        IsoFormatPrimaryVolumeDescriptor isoPrimaryVolumeDescriptor = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
        int pathTableLoc = isoPrimaryVolumeDescriptor.getFileStructureVersionAsInt();
        assertEquals(1, pathTableLoc);
        System.out.println("File Structor Version: " + pathTableLoc);
    }

    /**
     * Check its a valid header and then return the header.
     * @return 2048 of header data
     */
    byte[] testFileValidationAndReturnHeader() {
        File isoFile = new File("./src/test/resources/small.iso");
        byte[] headerInfo = new byte[2048];
        try (RandomAccessFile file = new RandomAccessFile(isoFile, "r")) {
            file.seek(IsoFormatConstant.BYTES_PER_SECTOR * IsoFormatConstant.BUFFER_SECTORS);
            Assertions.assertEquals(file.read(headerInfo, 0, 2048), IsoFormatConstant.BYTES_PER_SECTOR);
        } catch (FileNotFoundException e) {
            fail("Test ISO not found");
        } catch (IOException e) {
            fail("Test ISO read error");
        }

        assertTrue(IsoFormatPrimaryVolumeDescriptor.validator(headerInfo));
        return headerInfo;
    }
}
