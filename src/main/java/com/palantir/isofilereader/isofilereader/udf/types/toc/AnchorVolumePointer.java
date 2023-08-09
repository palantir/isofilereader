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
import java.util.Arrays;

/**
 * Tag Type 2; max size: 512 bytes.
 * <a href="http://www.osta.org/specs/pdf/udf260.pdf">UDF Standards Doc, section 2.2.3</a>.
 */
@SuppressWarnings("StrictUnusedVariable")
public class AnchorVolumePointer extends GenericDescriptor {
    private final ExtendedDescriptor mainVolumeDescriptor;
    private final ExtendedDescriptor reserveVolumeDescriptor;
    private final byte reserved;

    /**
     * 32 bytes that contain a tag, and pointer to volume descriptors.
     *
     * @param record byte array in
     */
    public AnchorVolumePointer(byte[] record) {
        super(record);
        mainVolumeDescriptor = new ExtendedDescriptor(Arrays.copyOfRange(record, 16, 24));
        reserveVolumeDescriptor = new ExtendedDescriptor(Arrays.copyOfRange(record, 24, 32));
        reserved = record[32];
    }

    /**
     * MainVolumeDescriptor loc and size.
     *
     * @return ExtendedDescriptor
     */
    public ExtendedDescriptor getMainVolumeDescriptor() {
        return mainVolumeDescriptor;
    }

    /**
     * ReserveVolumeDescriptor loc and size.
     *
     * @return ExtendedDescriptor
     */
    public ExtendedDescriptor getReserveVolumeDescriptor() {
        return reserveVolumeDescriptor;
    }

    /**
     * Reserved byte for implementation, basically not used.
     *
     * @return byte
     */
    public byte getReserved() {
        return reserved;
    }

    //    struct AnchorVolumeDescriptorPointer { /* ECMA 167 3/10.2 */
    //        struct tag DescriptorTag;
    //        struct extent_ad MainVolumeDescriptorSequenceExtent;
    //        struct extent_ad ReserveVolumeDescriptorSequenceExtent;
    //        byte Reserved[480]; <- is this 480 bytes?
    //    }
}
