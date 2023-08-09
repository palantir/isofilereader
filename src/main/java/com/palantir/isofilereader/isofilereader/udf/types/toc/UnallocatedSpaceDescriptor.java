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

import com.palantir.isofilereader.isofilereader.udf.types.types.ExtendedDescriptor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tag Type 7; max size: no max.
 */
public class UnallocatedSpaceDescriptor extends GenericDescriptor {
    // struct tag DescriptorTag;
    // Uint32 VolumeDescriptorSequenceNumber;
    private final byte[] volumeDescriptorSequenceNumber;
    // Uint32 NumberOfAllocationDescriptors;
    private final byte[] numberOfAllocationDescriptors;
    // extent_ad AllocationDescriptors[];
    private final byte[] allocationDescriptors;

    public UnallocatedSpaceDescriptor(byte[] record) {
        super(record);
        // Uint32 VolumeDescriptorSequenceNumber;
        this.volumeDescriptorSequenceNumber = Arrays.copyOfRange(record, 16, 20);
        // Uint32 NumberofAllocationDescriptors;
        this.numberOfAllocationDescriptors = Arrays.copyOfRange(record, 20, 24);
        // extent_ad AllocationDescriptors[];
        this.allocationDescriptors = Arrays.copyOfRange(record, 24, 24 + (getNumberOfAllocationDescriptorsAsInt() * 8));
    }

    /**
     * Get which image this is in a sequence of images.
     *
     * @return byte of the image number in volume
     */
    public byte[] getVolumeDescriptorSequenceNumber() {
        return volumeDescriptorSequenceNumber;
    }

    /**
     * Get the number of allocation descriptors, this helps find the length of the allocation descriptors.
     *
     * @return byte array of number of allocation descriptors
     */
    public byte[] getNumberOfAllocationDescriptors() {
        return numberOfAllocationDescriptors;
    }

    /**
     * Convert the number of allocation descriptors to a Java int for easier processing.
     *
     * @return int of number of descriptors
     */
    public int getNumberOfAllocationDescriptorsAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(getNumberOfAllocationDescriptors());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Get the Allocation Descriptors broken up into individual Extended Descriptor types.
     *
     * @return array of ExtendedDescriptors
     */
    public ExtendedDescriptor[] getAllocationDescriptors() {
        List<ExtendedDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < allocationDescriptors.length; i += 8) {
            descriptors.add(new ExtendedDescriptor(Arrays.copyOfRange(allocationDescriptors, i, i + 8)));
        }
        return descriptors.toArray(new ExtendedDescriptor[0]);
    }
}
