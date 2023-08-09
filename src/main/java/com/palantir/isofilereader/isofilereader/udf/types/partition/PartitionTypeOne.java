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

import java.util.Arrays;

/**
 * Section 10.7.2/Page 3/21 of original spec.
 */
public class PartitionTypeOne extends AbstractPartitionMap {
    // 2 bytes
    private final byte[] volumeSequenceNumber;
    // 2 bytes
    private final byte[] partitionNumber;

    public PartitionTypeOne(byte[] header) {
        super(header);
        this.volumeSequenceNumber = Arrays.copyOfRange(header, 2, 4);
        this.partitionNumber = Arrays.copyOfRange(header, 4, 6);
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
