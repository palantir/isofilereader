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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SuppressWarnings("StrictUnusedVariable")
public class IsoFormatVolumePartitionDescriptor extends AbstractVolumeDescriptor {
    // 0 - Byte in descriptor
    private final byte volumeDescriptorType;
    // 1-5
    private final byte[] standardIdentifier;
    // 6
    private final byte volumeDescriptorVersion;
    // 7 - Unused 1 byte
    // 8-39 - System Identifier, a-characters
    private final byte[] systemIdentifier;
    // 40-71 - Volume Partition Identifier, d-characters
    private final byte[] volumePartitionIdentifier;
    // 72-79 - Volume Partition Location
    private final byte[] volumePartitionLocation;
    // 80-87 - Volume Partition Size
    private final byte[] volumePartitionSize;
    // Rest is system use

    public IsoFormatVolumePartitionDescriptor(byte[] header) {
        super(header);
        volumeDescriptorType = header[0];
        standardIdentifier = Arrays.copyOfRange(header, 1, 6);
        volumeDescriptorVersion = header[6];

        // 8-39 - System Identifier, a-characters
        systemIdentifier = Arrays.copyOfRange(header, 8, 40);

        // 40-71 - Volume Partition Identifier, d-characters
        volumePartitionIdentifier = Arrays.copyOfRange(header, 40, 72);
        // 72-79 - Volume Partition Location
        volumePartitionLocation = Arrays.copyOfRange(header, 72, 80);
        // 80-87 - Volume Partition Size
        volumePartitionSize = Arrays.copyOfRange(header, 80, 88);
    }

    /**
     * This is always 3 for this type of record.
     *
     * @return byte with 3 for this type
     */
    @Override
    public byte getVolumeDescriptorType() {
        return volumeDescriptorType;
    }

    /**
     * This is always CD001 according to the standard.
     *
     * @return byte array of standard Identifier
     */
    @Override
    public byte[] getStandardIdentifier() {
        return standardIdentifier;
    }

    /**
     * Get bytes of the volumeDescriptorVersion.
     *
     * @return byte
     */
    @Override
    public byte getVolumeDescriptorVersion() {
        return volumeDescriptorVersion;
    }

    /**
     * Get the volume descriptor type as a int.
     *
     * @return byte of descriptor type
     */
    @Override
    public int getVolumeDescriptorTypeAsInt() {
        return getVolumeDescriptorType();
    }

    // standardIdentifier = Arrays.copyOfRange(header, 1,6); This is always CD001, so not making a getter

    // volumeDescriptorVersion = header[7]; always 1, not creating a getter

    /**
     * Get the identifier for the system that created the image.
     *
     * @return bytes 32 of the data
     */
    public byte[] getSystemIdentifier() {
        return systemIdentifier;
    }

    /**
     * The identifier is a String, allow the user to get the String if wanted.
     *
     * @return String of Identifier
     */
    public String getSystemIdentifierAsString() {
        return new String(getSystemIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Get raw bytes of the partition identifier.
     *
     * @return byte array
     */
    public byte[] getVolumePartitionIdentifier() {
        return volumePartitionIdentifier;
    }

    /**
     * Partition identifier converted to a string from UTF-8.
     *
     * @return string object
     */
    public String getVolumePartitionIdentifierAsString() {
        return new String(getVolumePartitionIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Bytes of the location of the partition denoted in logical sectors.
     *
     * @return byte array of location of partition
     */
    public byte[] getVolumePartitionLocation() {
        return volumePartitionLocation;
    }

    /**
     * Volume partition location as a logical sector count converted to a long from big big format.
     *
     * @return long of logical sector number
     */
    public long getVolumePartitionLocationAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(Arrays.copyOfRange(getVolumePartitionLocation(), 4, 8), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Raw bytes of teh size of the partition.
     *
     * @return byte array
     */
    public byte[] getVolumePartitionSize() {
        return volumePartitionSize;
    }

    /**
     * Converted long of the volume partition size as a long.
     *
     * @return this long * logical block size = byte offset
     */
    public long getVolumePartitionSizeAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(Arrays.copyOfRange(getVolumePartitionSize(), 4, 8), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }
}
