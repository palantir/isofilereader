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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * <a href="https://www.ecma-international.org/wp-content/uploads/ECMA-167_3rd_edition_june_1997.pdf">7.1</a>.
 * These are known as struct extent_ad.
 * Section 7.1/Page 3/3 of original document.
 * <p>
 * There is an identical descriptor called the Short Allocation Descriptor (short_ad), which has the same 4 bytes for
 * length and loc.
 * Section 4.14.1/Page 4/46 of original document.
 */
@SuppressWarnings("StrictUnusedVariable")
public class ExtendedDescriptor {
    // 0 - 3
    private final byte[] length;
    // 4 - 7
    private final byte[] loc;

    /**
     * Requires 8 bytes for the Extended Descriptor Loc/Length info.
     *
     * @param record 8 byte array
     */
    public ExtendedDescriptor(byte[] record) {
        length = Arrays.copyOfRange(record, 0, 4);
        loc = Arrays.copyOfRange(record, 4, 8);
    }

    /**
     * Get length of data segment.
     *
     * @return byte data of the length
     */
    public byte[] getLength() {
        return length;
    }

    /**
     * Get the length of the descriptors as a Java int.
     *
     * @return length of the main volume descriptor
     */
    public int getLengthAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(getLength());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Location in logical sectors of this descriptor. Usually this number * 2048 bytes for location.
     *
     * @return byte array of the location
     */
    public byte[] getLoc() {
        return loc;
    }

    /**
     * Get the location of the descriptor as an int instead a byte.
     *
     * @return int of location
     */
    public int getLocAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(getLoc());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }
}
