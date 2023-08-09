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

package com.palantir.isofilereader.isofilereader.udf.types.toc;

import com.palantir.isofilereader.isofilereader.Util;
import com.palantir.isofilereader.isofilereader.udf.types.types.EntityId;
import java.util.Arrays;

/**
 * Tag Type 5; max size: 512 bytes.
 */
@SuppressWarnings("StrictUnusedVariable")
public class PartitionDescriptor extends GenericDescriptor {
    // Uint32 VolumeDescriptorSequenceNumber;
    private final byte[] volumeDescriptorSequenceNumber;
    // Uint16 PartitionFlags;
    private final byte[] partitionFlags;
    // Uint16 PartitionNumber;
    private final byte[] partitionNumber;
    // struct EntityID PartitionContents;
    private final EntityId partitionContents;
    // byte PartitionContentsUse[128];
    private final byte[] partitionContentsUse;
    // Uint32 AccessType;
    private final byte[] accessType;
    // Uint32 PartitionStartingLocation;
    private final byte[] partitionStartingLocation;
    // Uint32 PartitionLength;
    private final byte[] partitionLength;
    // struct EntityID ImplementationIdentifier;
    private final EntityId implementationIdentifier;
    // byte ImplementationUse[128];
    // Skip 128 Bytes
    // byte Reserved[156];
    // Skip 156 Bytes

    public PartitionDescriptor(byte[] record) {
        super(record);
        this.volumeDescriptorSequenceNumber = Arrays.copyOfRange(record, 16, 20);
        this.partitionFlags = Arrays.copyOfRange(record, 20, 22);
        this.partitionNumber = Arrays.copyOfRange(record, 22, 24);
        this.partitionContents = new EntityId(Arrays.copyOfRange(record, 24, 56));
        this.partitionContentsUse = Arrays.copyOfRange(record, 56, 184);
        this.accessType = Arrays.copyOfRange(record, 184, 188);
        this.partitionStartingLocation = Arrays.copyOfRange(record, 188, 192);
        this.partitionLength = Arrays.copyOfRange(record, 192, 196);
        this.implementationIdentifier = new EntityId(Arrays.copyOfRange(record, 196, 228));
        // byte ImplementationUse[128];
        // Skip 128 Bytes
        // byte Reserved[156];
        // Skip 156 Bytes
    }

    /**
     * Sequence number, used for multi-volume images.
     *
     * @return byte array
     */
    public byte[] getVolumeDescriptorSequenceNumber() {
        return volumeDescriptorSequenceNumber;
    }

    /**
     * Get UDF Flags on partition. 0 if the partition does not have space allocated, 1 if it does.
     *
     * @return byte array
     */
    public byte[] getPartitionFlags() {
        return partitionFlags;
    }

    /**
     * Get the partition number as a byte array.
     *
     * @return byte array
     */
    public byte[] getPartitionNumber() {
        return partitionNumber;
    }

    /**
     * Get partition contents as EntityId. This will be something similar to "+NSR03" or "+FDC01" depending on the
     * standard in use.
     *
     * @return EntityId
     */
    public EntityId getPartitionContents() {
        return partitionContents;
    }

    /**
     * Get Partition Contents Use descriptor as byte array.
     *
     * @return byte array
     */
    public byte[] getPartitionContentsUse() {
        return partitionContentsUse;
    }

    /**
     * Get the access type of the media. 0 is not specified, 1 is read only, 2 is write-once, 3 is rewritable, 4 is
     * overridable. Being this is a reading library, we don't care too much.
     *
     * @return byte array
     */
    public byte[] getAccessType() {
        return accessType;
    }

    /**
     * The address of the partition starting location, as a byte array.
     *
     * @return byte array of starting location
     */
    public byte[] getPartitionStartingLocation() {
        return partitionStartingLocation;
    }

    /**
     * This is the logical starting position.
     *
     * @return int of the logical starting position
     */
    public int getPartitionStartingLocationAsInt() {
        return Util.fourUnsignedByteToInt(getPartitionStartingLocation());
    }

    /**
     * Length of the partition.
     *
     * @return byte array of length
     */
    public byte[] getPartitionLength() {
        return partitionLength;
    }

    /**
     * Partition length converted to Java int.
     *
     * @return int
     */
    public int getPartitionLengthAsInt() {
        return Util.fourUnsignedByteToInt(getPartitionLength());
    }

    /**
     * Identifier of software that made this section of the table.
     *
     * @return EntityId
     */
    public EntityId getImplementationIdentifier() {
        return implementationIdentifier;
    }
}
