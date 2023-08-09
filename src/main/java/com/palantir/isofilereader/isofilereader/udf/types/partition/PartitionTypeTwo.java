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

package com.palantir.isofilereader.isofilereader.udf.types.partition;

import com.palantir.isofilereader.isofilereader.udf.types.types.EntityId;
import java.util.Arrays;

/**
 * Section 10.7.3/ Page 3/22 of original document.
 */
public class PartitionTypeTwo extends AbstractPartitionMap {
    // Length 32 bytes
    private final EntityId partitionTypeIdentifier;
    // 2 bytes
    private final byte[] volumeSequenceNumber;
    // 2 bytes
    private final byte[] partitionNumber;

    public PartitionTypeTwo(byte[] header) {
        super(header);
        this.partitionTypeIdentifier = new EntityId(Arrays.copyOfRange(header, 4, 36));
        this.volumeSequenceNumber = Arrays.copyOfRange(header, 36, 38);
        this.partitionNumber = Arrays.copyOfRange(header, 38, 40);
    }

    /**
     * Get an EntityId for the Partition Type Identifier.
     *
     * @return EntityId
     */
    public EntityId getPartitionTypeIdentifier() {
        return partitionTypeIdentifier;
    }

    /**
     * Get which image this is in a sequence of images.
     *
     * @return byte of the image number in sequence
     */
    public byte[] getVolumeSequenceNumber() {
        return volumeSequenceNumber;
    }

    /**
     * Get a byte array representation of this partition number.
     *
     * @return byte array
     */
    public byte[] getPartitionNumber() {
        return partitionNumber;
    }
}
