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

package com.palantir.isofilereader.isofilereader.iso.types;

import com.palantir.isofilereader.isofilereader.Util;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This is a raw ISO format directory record, we read the raw data into byte fields, then translate out of those
 * fields what we need. IsoFormatInternalDataFile is the enhanced type over this class.
 */
@SuppressWarnings("StrictUnusedVariable")
public class IsoFormatDirectoryRecord {
    // 0 - Length of directory record
    private final byte lenDirRecord;
    // 1 - Ext Attrib Record Length
    private final byte extAttrRecordLen;
    // 2-9
    private final byte[] locOfExt;
    // 10-17 - Data length
    private final byte[] dataLength;
    // 18-24 - Recording Date and Time
    private final byte[] dataAndTime;
    // 25
    private final byte fileFlags;
    // 26
    private final byte fileUnitSize;
    // 27
    private final byte interLeaveGapSize;
    // 28-31
    private final byte[] volumeSeqNum;
    // 32
    private final byte lenOfFileIdentifier;
    // 33-(33+lenOfFileIdentifier)
    private final byte[] fileIdentifier;
    // 34+lenOfFileIdentifier
    // Padding
    private Map<String, RockRidgeAttribute> rockRidgeAttributeMap = new HashMap<>();

    private final String parent; // This is not part of the spec, but it's much harder to track files without it

    /**
     * Create a new raw IsoDirectoryRecord, feed in the raw bytes, and if it can be parsed, the data will be split.
     *
     * @param record  raw bytes of the record
     * @param parent  String of the parent for full filename tracking
     */
    public IsoFormatDirectoryRecord(byte[] record, String parent) {
        this.parent = parent;

        // 0 - Length of directory record
        lenDirRecord = record[0];
        // 1 - Ext Attrib Record Length
        extAttrRecordLen = record[1];
        // 2-9
        locOfExt = Arrays.copyOfRange(record, 2, 10);
        // 10-17 - Data length
        dataLength = Arrays.copyOfRange(record, 10, 18);
        // 18-24 - Recording Date and Time
        dataAndTime = Arrays.copyOfRange(record, 18, 25);
        // 25
        fileFlags = record[25];
        // 26
        fileUnitSize = record[26];
        // 27
        interLeaveGapSize = record[27];
        // 28-31
        volumeSeqNum = Arrays.copyOfRange(record, 28, 32);
        // 32
        lenOfFileIdentifier = record[32];
        // 33-(33+lenOfFileIdentifier)
        fileIdentifier = Arrays.copyOfRange(record, 33, 33 + Byte.toUnsignedInt(lenOfFileIdentifier));
        // 34+lenOfFileIdentifier
        // Padding
        if (record.length > (34 + Byte.toUnsignedInt(lenOfFileIdentifier))) {
            if (getLenOfFileIdentifierAsInt() % 2 == 0) {
                // Optional padding bit
                rockRidgeAttributeMap = readAttributes(
                        Arrays.copyOfRange(record, 34 + Byte.toUnsignedInt(lenOfFileIdentifier), record.length));
            } else {
                rockRidgeAttributeMap = readAttributes(
                        Arrays.copyOfRange(record, 33 + Byte.toUnsignedInt(lenOfFileIdentifier), record.length));
            }
        }
    }

    private Map<String, RockRidgeAttribute> readAttributes(byte[] systemUseArea) {
        Map<String, RockRidgeAttribute> returningHash = new HashMap<>();
        int loc = 0;
        while (loc < (systemUseArea.length - 1)) {
            int lengthOfSingleExtension = Byte.toUnsignedInt(systemUseArea[2 + loc]);
            byte[] arrayData = Arrays.copyOfRange(systemUseArea, loc, loc + lengthOfSingleExtension);

            RockRidgeAttribute rockRidgeAttribute = new RockRidgeAttribute(arrayData);
            returningHash.put(rockRidgeAttribute.getSignatureAsString(), rockRidgeAttribute);
            loc += lengthOfSingleExtension;
        }
        return returningHash;
    }

    /**
     * Get length of the directory record as a byte.
     *
     * @return int of size of record
     */
    public byte getLenDirRecord() {
        return lenDirRecord;
    }

    /**
     * Get length of the directory record as an int. 7.1.1 Unsigned byte
     *
     * @return int of size of record
     */
    public int getLenDirRecordAsInt() {
        return Byte.toUnsignedInt(getLenDirRecord());
    }

    /**
     * Get the size of the Ext Attr record as a byte.
     *
     * @return length as a byte
     */
    public byte getExtAttrRecordLen() {
        return extAttrRecordLen;
    }

    /**
     * Get the size of the Ext Attr record as an int. 7.1.1 Unsigned byte
     *
     * @return length as int
     */
    public int getExtAttrRecordLenAsInt() {
        return Byte.toUnsignedInt(getExtAttrRecordLen());
    }

    /**
     * Get the location in logical blocks of where the data lives.
     *
     * @return byte array in 7.3.3 of data loc
     */
    public byte[] getLocOfExt() {
        return locOfExt;
    }

    /**
     * Get loc of ext record. Standard 7.3.3, both byte order (yz wx uv st st uv wx yz)
     *
     * @return long of the loc of the ext, this is in logical blocks
     */
    public long getLocOfExtAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(Arrays.copyOfRange(getLocOfExt(), 4, 8), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * 7.3.3 of data length in byte array.
     *
     * @return data length in byte array
     */
    public byte[] getDataLength() {
        return dataLength;
    }

    /**
     * Get data length. Standard 7.3.3, both byte order (yz wx uv st st uv wx yz)
     *
     * @return long of the data length
     */
    public long getDataLengthAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(Arrays.copyOfRange(getDataLength(), 4, 8), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Get byte array that represents the date and time.
     *
     * @return byte array
     */
    public byte[] getDataAndTime() {
        return dataAndTime;
    }

    /**
     * Java Date of the date and time. 9.1.5 format
     *
     * @return java date
     */
    public Optional<Date> getDataAndTimeAsDate() {
        return Util.convert9_1_5DateTime(getDataAndTime());
    }

    /**
     * Byte of all the file flags.
     *
     * @return byte of file flag
     */
    public byte getFileFlags() {
        return fileFlags;
    }

    /**
     * Byte of file flags converted to an int.
     *
     * @return int of flags
     */
    public int getFileFlagsAsInt() {
        return Byte.toUnsignedInt(getFileFlags());
    }

    /**
     * The file unit size itself can be changed (but usually isn't), this field tracks that.
     *
     * @return byte of the unit size
     */
    public byte getFileUnitSize() {
        return fileUnitSize;
    }

    /**
     * Size of the interleave gap, test images don't seem to be using this much anymore.
     *
     * @return byte of interleave size
     */
    public byte getInterLeaveGapSize() {
        return interLeaveGapSize;
    }

    /**
     * If this image is a volume in a sequence, which number is it.
     *
     * @return byte array
     */
    public byte[] getVolumeSeqNum() {
        return volumeSeqNum;
    }

    /**
     * Length of file identifier, this is needed because the directory record is variable length.
     *
     * @return byte of size of file identifier
     */
    public byte getLenOfFileIdentifier() {
        return lenOfFileIdentifier;
    }

    /**
     * File identifer length changed to an int.
     *
     * @return int of file identifier length
     */
    public int getLenOfFileIdentifierAsInt() {
        return Byte.toUnsignedInt(getLenOfFileIdentifier());
    }

    /**
     * Get file identifier as a byte array.
     *
     * @return byte array of file identifier
     */
    public byte[] getFileIdentifier() {
        return fileIdentifier;
    }

    /**
     * Get the file identifier as a String.
     *
     * @return string of identifier
     */
    public String getFileIdentifierAsString() {
        // TODO(#): There are specific flags to look out for

        if (getFileIdentifier().length == 1) {
            switch (getFileIdentifier()[0]) {
                case 0x00:
                    return ".";
                case 0x01:
                    return "..";
            }
        }
        // Instead of trying to choose
        Charset charSetToUse = StandardCharsets.UTF_8;
        if (Arrays.binarySearch(getFileIdentifier(), (byte) 0) >= 0) {
            // Sometimes this is UTF-16 sometimes its UTF-8, if its UTF-16 there should be at least 1 zero, otherwise
            // this name should not have a 0 according to spec.
            charSetToUse = StandardCharsets.UTF_16BE;
        }
        return new String(getFileIdentifier(), charSetToUse);
    }

    /**
     * Get all the Rock Ridge attributes of the record.
     *
     * @return Map of attributes
     */
    public Map<String, RockRidgeAttribute> getRockRidgeAttributeMap() {
        return rockRidgeAttributeMap;
    }

    /**
     * At the start of each folder there are directory records for . and .., you may not want to process those.
     *
     * @return if its a . or .. folder identifier
     */
    public boolean isTopLevelIdentifier() {
        if (getFileIdentifier().length == 1) {
            switch (getFileIdentifier()[0]) {
                case 0x00:
                case 0x01:
                    return true;
            }
        }
        return false;
    }

    /**
     * Do bitwise operations according to spec to check if this is a directory.
     *
     * @return the record is a directory
     */
    public boolean isDirectory() {
        return java.math.BigInteger.valueOf(getFileFlags()).testBit(1)
                && !java.math.BigInteger.valueOf(getFileFlags()).testBit(2)
                && !java.math.BigInteger.valueOf(getFileFlags()).testBit(3)
                && !java.math.BigInteger.valueOf(getFileFlags()).testBit(7);
    }

    /**
     * Check if the directory entry has extended attributes.
     *
     * @return has extended attributes
     */
    public boolean hasExtendedAttributes() {
        return java.math.BigInteger.valueOf(getFileFlags()).testBit(3)
                || java.math.BigInteger.valueOf(getFileFlags()).testBit(4);
    }

    /**
     * Get the string of the parent name.
     *
     * @return string of name
     */
    public String getParent() {
        return parent;
    }
}
