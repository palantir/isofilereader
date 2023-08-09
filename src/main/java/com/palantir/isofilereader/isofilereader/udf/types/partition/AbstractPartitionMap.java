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

/**
 * Abstract of Partition Map.
 * Section 10.7/Page 3/20 of original standard.
 */
public class AbstractPartitionMap {
    public static final byte TYPE_0 = 0x00;
    public static final byte TYPE_1 = 0x01;
    public static final byte TYPE_2 = 0x02;
    private final byte partitionType;
    private final byte partitionLength;

    public AbstractPartitionMap(byte[] header) {
        this.partitionType = header[0];
        this.partitionLength = header[1];
    }

    /**
     * Get the type of partition as a byte.
     *
     * @return byte
     */
    public byte getPartitionType() {
        return partitionType;
    }

    /**
     * Get the length of the partition information, as a byte.
     *
     * @return byte
     */
    public byte getPartitionLength() {
        return partitionLength;
    }
}
