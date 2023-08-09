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

package com.palantir.isofilereader.isofilereader.udf.types.types;

import com.palantir.isofilereader.isofilereader.Util;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * UDF Standards Doc, section 2.2.1.
 */
@SuppressWarnings("StrictUnusedVariable")
public class Tag {
    // 0-1
    private final byte[] tagIdentifier;
    public static final int PRIMARY_VOLUME = 1;
    public static final int ANCHOR_VOLUME_DESCRIPTOR_POINTER = 2;
    public static final int VOLUME_DESCRIPTOR_POINTER = 3;
    public static final int IMPL_USE_VOLUME_DESCRIPTOR = 4;
    public static final int PARTITION_DESCRIPTOR = 5;
    public static final int LOGICAL_VOLUME_DESCRIPTOR = 6;
    public static final int UNALLOCATED_SPACE_DESCRIPTOR = 7;
    public static final int TERMINATING_DESCRIPTOR = 8;
    public static final int LOGICAL_VOLUME_INTEGRITY_DESCRIPTOR = 9;
    public static final int FILE_SET_DESCRIPTOR = 256;
    public static final int FILE_IDENTIFIER_DESCRIPTOR = 257;
    public static final int ALLOCATION_EXTENT_DESCRIPTOR = 258;
    public static final int INDIRECT_ENTRY = 259;
    public static final int TERMINAL_ENTRY = 260;
    public static final int FILE_ENTRY = 261;
    public static final int EXTENDED_ATTRIBUTE_HEADER_DESCRIPTOR = 262;
    public static final int UNALLOCATED_SPACE_ENTRY = 263;
    public static final int SPACE_BITMAP_DESCRIPTOR = 264;
    public static final int PARTITION_INTEGRITY_ENTRY = 265;
    public static final int EXTENDED_FILE_ENTRY = 266;

    // 2-3
    private final byte[] descriptorVersion;
    // 4
    private final byte tagChecksum;
    // 5
    private final byte reserved;
    // 6-7
    private final byte[] tagSerialNumber;
    // 8-9
    private final byte[] descriptorCrc;
    // 10-11
    private final byte[] descriptorCrcLength;
    // 12-15
    private final byte[] tagLocation;

    /**
     * Tag to identify info in this sector of the image, 16 bytes long.
     *
     * @param record 16 bytes of tag to parse
     */
    public Tag(byte[] record) {
        tagIdentifier = Arrays.copyOfRange(record, 0, 2);
        descriptorVersion = Arrays.copyOfRange(record, 2, 4);
        tagChecksum = record[4];
        reserved = record[5];
        tagSerialNumber = Arrays.copyOfRange(record, 6, 8);
        descriptorCrc = Arrays.copyOfRange(record, 8, 10);
        descriptorCrcLength = Arrays.copyOfRange(record, 10, 12);
        tagLocation = Arrays.copyOfRange(record, 12, 16);
    }

    /**
     * Tag identifier, this explains what type of tag this data is for.
     *
     * @return tagIdentifier
     */
    public byte[] getTagIdentifier() {
        return tagIdentifier;
    }

    /**
     * Get The Tag identifier as an int to compare to our constants.
     *
     * @return int of tag identifier
     */
    public int getTagIdentifierAsInt() {
        //        byte[] temp = new byte[4];
        //        temp[1] = getTagIdentifier()[1];
        //        temp[0] = getTagIdentifier()[0];
        //        final ByteBuffer bb = ByteBuffer.wrap(temp);
        //        bb.order(ByteOrder.LITTLE_ENDIAN);
        return Util.twoUnsignedByteToInt(getTagIdentifier());
    }

    /**
     * Get the byte array describing the descriptor version.
     *
     * @return byte array
     */
    public byte[] getDescriptorVersion() {
        return descriptorVersion;
    }

    /**
     * Get the byte checksum of the tag itself, this takes all data in the 16 byte array except this one,
     * and then sum modulus 256 the data. tagChecksumIsCorrect() will check this for you.
     *
     * @return byte of the checksum
     */
    public byte getTagChecksum() {
        return tagChecksum;
    }

    /**
     * This is a reserved byte.
     *
     * @return access to reserved byte
     */
    public byte getReserved() {
        return reserved;
    }

    /**
     * Get the serial number of this tag, they will increment throughout a volume.
     *
     * @return byte array of tag serial
     */
    public byte[] getTagSerialNumber() {
        return tagSerialNumber;
    }

    /**
     * CRC of the descriptor that is to follow.
     *
     * @return byte array
     */
    public byte[] getDescriptorCrc() {
        return descriptorCrc;
    }

    /**
     * The descriptor this tag is metadata for, the length of info to scan for the CRC.
     *
     * @return Byte array of the CRC length
     */
    public byte[] getDescriptorCrcLength() {
        return descriptorCrcLength;
    }

    /**
     * Get the DescriptorCRC as an int instead of a byte array.
     *
     * @return Length of CRC in bytes
     */
    public int getDescriptorCrcLengthAsInt() {
        //        byte[] paddedFixedBothNumber = new byte[4];
        //        System.arraycopy(Arrays.copyOfRange(getDescriptorCrcLength(), 0, 2), 0, paddedFixedBothNumber, 2, 2);
        //        return ByteBuffer.wrap(paddedFixedBothNumber).getInt();
        return ((getDescriptorCrcLength()[1] & 0xff) << 8) | (getDescriptorCrcLength()[0] & 0xff);
        //        final ByteBuffer bb = ByteBuffer.wrap(getDescriptorCrcLength());
        //        bb.order(ByteOrder.LITTLE_ENDIAN);
        //        return bb.getInt();
    }

    /**
     * Logical location as a sector number, this * Logical_sector_size = byte location.
     *
     * @return Byte array of tag location
     */
    public byte[] getTagLocation() {
        return tagLocation;
    }

    /**
     * Same Tag location, but as an int instead of a byte array.
     *
     * @return Int of logical sector number
     */
    public int getTagLocationAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(getTagLocation());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Check this tags CRC for the 16 bytes of the tag.
     *
     * @return Does the CRC checkout
     */
    public boolean tagChecksumIsCorrect() {
        int rollingCount = 0;
        rollingCount += tagIdentifier[0];
        rollingCount += tagIdentifier[1];
        rollingCount += descriptorVersion[0];
        rollingCount += descriptorVersion[1];
        rollingCount += reserved;
        rollingCount += tagSerialNumber[0];
        rollingCount += tagSerialNumber[1];
        rollingCount += descriptorCrc[0];
        rollingCount += descriptorCrc[1];
        rollingCount += descriptorCrcLength[0];
        rollingCount += descriptorCrcLength[1];
        rollingCount += tagLocation[0];
        rollingCount += tagLocation[1];
        rollingCount += tagLocation[2];
        rollingCount += tagLocation[3];
        return (rollingCount % 256) == Byte.toUnsignedInt(tagChecksum);
    }

    /**
     * Check if this tag looks valid or is not a tag.
     *
     * @param logicalLocationOfSector We need to logical sector it is at to compare with what it has marked
     * @return Valid UDF tag or invalid
     */
    public boolean verifyValidTag(long logicalLocationOfSector) {
        return (getTagIdentifierAsInt() == Tag.ANCHOR_VOLUME_DESCRIPTOR_POINTER
                && tagChecksumIsCorrect()
                && (getTagLocationAsInt() == logicalLocationOfSector));
    }
}
