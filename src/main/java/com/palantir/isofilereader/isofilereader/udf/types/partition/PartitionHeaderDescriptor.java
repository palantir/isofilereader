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

import com.palantir.isofilereader.isofilereader.udf.types.types.ExtendedDescriptor;
import java.util.Arrays;

/**
 * The Partition Header Descriptor is recorded in the Partition Contents Use field of
 * the Partition Descriptor.
 * Section 14.3/Page 4/20 of original standard.
 * The extended descriptor and short_ad are functionally identical.
 */
public class PartitionHeaderDescriptor {
    // struct short_ad UnallocatedSpaceTable;
    private final ExtendedDescriptor unallocatedSpaceTable;
    // struct short_ad UnallocatedSpaceBitmap;
    private final ExtendedDescriptor unallocatedSpaceBitmap;
    // struct short_ad PartitionIntegrityTable;
    private final ExtendedDescriptor partitionIntegrityTable;
    // struct short_ad FreedSpaceTable;
    private final ExtendedDescriptor freedSpaceTable;
    // struct short_ad FreedSpaceBitmap;
    private final ExtendedDescriptor freedSpaceBitmap;
    // byte Reserved[88];
    private final byte[] reserved;

    public PartitionHeaderDescriptor(byte[] header) {
        this.unallocatedSpaceTable = new ExtendedDescriptor(Arrays.copyOfRange(header, 0, 8));
        this.unallocatedSpaceBitmap = new ExtendedDescriptor(Arrays.copyOfRange(header, 8, 16));
        this.partitionIntegrityTable = new ExtendedDescriptor(Arrays.copyOfRange(header, 16, 24));
        this.freedSpaceTable = new ExtendedDescriptor(Arrays.copyOfRange(header, 24, 32));
        this.freedSpaceBitmap = new ExtendedDescriptor(Arrays.copyOfRange(header, 32, 40));
        this.reserved = Arrays.copyOfRange(header, 40, 128);
    }

    /**
     * Get the location/size of the unallocated space table.
     *
     * @return ExtendedDescriptor/Short_ad
     */
    public ExtendedDescriptor getUnallocatedSpaceTable() {
        return unallocatedSpaceTable;
    }

    /**
     * Get the location/size of the unallocated space bitmap.
     *
     * @return ExtendedDescriptor/Short_ad
     */
    public ExtendedDescriptor getUnallocatedSpaceBitmap() {
        return unallocatedSpaceBitmap;
    }

    /**
     * Get the location/size of the partition integrity table.
     *
     * @return ExtendedDescriptor/Short_ad
     */
    public ExtendedDescriptor getPartitionIntegrityTable() {
        return partitionIntegrityTable;
    }

    /**
     * Get the location/size of the freed space table.
     *
     * @return ExtendedDescriptor/Short_ad
     */
    public ExtendedDescriptor getFreedSpaceTable() {
        return freedSpaceTable;
    }

    /**
     * Get the location/size of the freed space bitmap.
     *
     * @return ExtendedDescriptor/Short_ad
     */
    public ExtendedDescriptor getFreedSpaceBitmap() {
        return freedSpaceBitmap;
    }

    /**
     * Reserved bytes that should not be used but are included for future use cases.
     *
     * @return byte array
     */
    public byte[] getReserved() {
        return reserved;
    }
}
