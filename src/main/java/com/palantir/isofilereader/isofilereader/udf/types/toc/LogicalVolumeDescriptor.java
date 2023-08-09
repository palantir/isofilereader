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
import com.palantir.isofilereader.isofilereader.udf.types.partition.AbstractPartitionMap;
import com.palantir.isofilereader.isofilereader.udf.types.partition.PartitionTypeOne;
import com.palantir.isofilereader.isofilereader.udf.types.partition.PartitionTypeTwo;
import com.palantir.isofilereader.isofilereader.udf.types.types.CharSpec;
import com.palantir.isofilereader.isofilereader.udf.types.types.EntityId;
import com.palantir.isofilereader.isofilereader.udf.types.types.ExtendedDescriptor;
import java.util.Arrays;

/**
 * Tag Type 6; max size: no max.
 */
@SuppressWarnings("StrictUnusedVariable")
public class LogicalVolumeDescriptor extends GenericDescriptor {
    // Uint32 VolumeDescriptorSequenceNumber;
    private final byte[] volumeDescriptorSequenceNumber;
    // struct charspec DescriptorCharacterSet;
    private final CharSpec descriptorCharSet;
    // dstring LogicalVolumeIdentifier[128]
    private final byte[] logicalVolumeIdentifier;
    // Uint32 LogicalBlockSize
    private final byte[] logicalBlockSize;
    // struct EntityID DomainIdentifier
    private final EntityId domainIdentifier;
    // byte LogicalVolumeContentsUse[16]
    private final byte[] logicalVolumeContentsUse;
    // Uint32 MapTableLength
    private final byte[] mapTableLength;
    // Uint32 NumberofPartitionMaps
    private final byte[] numberOfPartitionMaps;
    // struct EntityID ImplementationIdentifier
    private final EntityId implementationIdentifier;
    // extent_ad IntegritySequenceExtent,
    private final ExtendedDescriptor integritySequenceExtent;
    // byte PartitionMaps[]
    private final AbstractPartitionMap partitionMaps;

    public LogicalVolumeDescriptor(byte[] record) {
        super(record);
        this.volumeDescriptorSequenceNumber = Arrays.copyOfRange(record, 16, 20);
        // struct charspec DescriptorCharacterSet;
        this.descriptorCharSet = new CharSpec(Arrays.copyOfRange(record, 20, 84));
        // dstring LogicalVolumeIdentifier[128]
        this.logicalVolumeIdentifier = Arrays.copyOfRange(record, 84, 212);
        // Uint32 LogicalBlockSize
        this.logicalBlockSize = Arrays.copyOfRange(record, 212, 216);
        // struct EntityID DomainIdentifier
        this.domainIdentifier = new EntityId(Arrays.copyOfRange(record, 216, 248));
        // byte LogicalVolumeContentsUse[16]
        this.logicalVolumeContentsUse = Arrays.copyOfRange(record, 248, 264);
        // Uint32 MapTableLength
        this.mapTableLength = Arrays.copyOfRange(record, 264, 268);
        // Uint32 NumberOfPartitionMaps
        this.numberOfPartitionMaps = Arrays.copyOfRange(record, 268, 272);
        // struct EntityID ImplementationIdentifier
        this.implementationIdentifier = new EntityId(Arrays.copyOfRange(record, 272, 304));
        // Implementation Use[128]

        // extent_ad IntegritySequenceExtent,
        this.integritySequenceExtent = new ExtendedDescriptor(Arrays.copyOfRange(record, 432, 440));
        // byte PartitionMaps[]
        AbstractPartitionMap abstractPartitionMap =
                new AbstractPartitionMap(Arrays.copyOfRange(record, 440, record.length));
        switch (abstractPartitionMap.getPartitionType()) {
            case AbstractPartitionMap.TYPE_0:
                this.partitionMaps = null;
                break;
            case AbstractPartitionMap.TYPE_1:
                this.partitionMaps = new PartitionTypeOne(Arrays.copyOfRange(record, 440, record.length));
                break;
            case AbstractPartitionMap.TYPE_2:
                this.partitionMaps = new PartitionTypeTwo(Arrays.copyOfRange(record, 440, record.length));
                break;
            default:
                this.partitionMaps = null;
                break;
        }
    }

    /**
     * The Volume Descriptor sequence number.
     *
     * @return Byte Array[4]
     */
    public byte[] getVolumeDescriptorSequenceNumber() {
        return volumeDescriptorSequenceNumber;
    }

    /**
     * Get the charset for the descriptor.
     *
     * @return charset object
     */
    public CharSpec getDescriptorCharSet() {
        return descriptorCharSet;
    }

    /**
     * Get the logical volume identifier, this is usually the actual name of the Disc.
     *
     * @return Byte Array[128] of the logical volume identifier
     */
    public byte[] getLogicalVolumeIdentifier() {
        return logicalVolumeIdentifier;
    }

    /**
     * String version fo the logical volume identifier.
     *
     * @return String object
     */
    public String getLogicalVolumeIdentifierAsString() {
        return Util.convertDStringBytesToString(getLogicalVolumeIdentifier());
    }

    /**
     * Get the logical block size, this is usually 2048 bytes.
     *
     * @return Byte Array[4] of logical block size
     */
    public byte[] getLogicalBlockSize() {
        return logicalBlockSize;
    }

    /**
     * EntityId of the domain identifier.
     *
     * @return EntityId
     */
    public EntityId getDomainIdentifier() {
        return domainIdentifier;
    }

    /**
     * The Logical Volume Contents use. "This field contains the extent location of the File Set Descriptor. This is
     * described in 4/3.1 of ECMA 167".
     *
     * @return Byte Array[16]
     */
    public byte[] getLogicalVolumeContentsUse() {
        return logicalVolumeContentsUse;
    }

    /**
     * Map table length.
     *
     * @return Byte Array[4] of length
     */
    public byte[] getMapTableLength() {
        return mapTableLength;
    }

    /**
     * Number of Partition Maps.
     *
     * @return Byte Array[4]
     */
    public byte[] getNumberOfPartitionMaps() {
        return numberOfPartitionMaps;
    }

    /**
     * EntityId of the implementer of the file creation.
     *
     * @return EntityId
     */
    public EntityId getImplementationIdentifier() {
        return implementationIdentifier;
    }

    /**
     * The Integrity Sequence Extent descriptor.
     *
     * @return ExtendedDescriptor
     */
    public ExtendedDescriptor getIntegritySequenceExtent() {
        return integritySequenceExtent;
    }

    /**
     * A byte array of the partition maps within this logical volume descriptor.
     *
     * @return Raw partition map
     */
    public AbstractPartitionMap getPartitionMaps() {
        return partitionMaps;
    }
}
