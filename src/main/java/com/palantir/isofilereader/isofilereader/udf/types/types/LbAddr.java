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
 * Logical block address structure as stated on page 73 of the ECMA-167 original document.
 */
@SuppressWarnings("StrictUnusedVariable")
public class LbAddr {
    // RBP Length Name Contents
    // 0 4 Logical Block Number Uint32 (1/7.1.5)
    private final byte[] logicalBlockNumber;
    // 4 2 Partition Reference Number Uint16 (1/7.1.3)
    private final byte[] partitionReference;

    /**
     * Logical block address structure, stores the partition reference and logical block location of data.
     *
     * @param record requires 6 bytes
     */
    public LbAddr(byte[] record) {
        this.logicalBlockNumber = Arrays.copyOfRange(record, 0, 4);
        this.partitionReference = Arrays.copyOfRange(record, 4, 6);
    }

    /**
     * Logical block of data within that partition, this is (start of partition + (sector size * this))
     * to get byte position on disc.
     *
     * @return 4 byte array
     */
    public byte[] getLogicalBlockNumber() {
        return logicalBlockNumber;
    }

    /**
     * Same as getLogicalBlockNumber(), but value converted to a Java Long.
     *
     * @return long of logical block
     */
    public long getLogicalBlockNumberAsLong() {
        final ByteBuffer bb = ByteBuffer.wrap(getLogicalBlockNumber());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Get partition this data is referenced in.
     *
     * @return 2 byte array of partition id
     */
    public byte[] getPartitionReference() {
        return partitionReference;
    }

    /**
     * Get partition this data is referenced in, as Java int.
     *
     * @return partition ID
     */
    public int getPartitionReferenceAsInt() {
        return Util.twoUnsignedByteToInt(getPartitionReference());
    }
}
