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
 * Implementation of the Long Allocation Descriptor, page 66 of 2.6 document.
 */
@SuppressWarnings("StrictUnusedVariable")
public class LongAd {
    // Uint32 ExtentLength;
    private final byte[] extentLength;
    // Lb_addr ExtentLocation;
    private final LbAddr extentLocation;
    // byte ImplementationUse[6];
    private final byte[] implementationUse;

    /**
     * Long allocation descriptor, 16 byte location and length of data. The spec also allows for more info in impl use.
     *
     * @param record 16 bytes
     */
    public LongAd(byte[] record) {
        this.extentLength = Arrays.copyOfRange(record, 0, 4);
        this.extentLocation = new LbAddr(Arrays.copyOfRange(record, 4, 10));
        this.implementationUse = Arrays.copyOfRange(record, 10, 16);
    }

    /**
     * Get the length of this extent.
     *
     * @return byte array of length
     */
    public byte[] getExtentLength() {
        return extentLength;
    }

    /**
     * Get the length of the extent converted to a int for easy Java processing.
     *
     * @return int of length
     */
    public int getExtentLengthAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(getExtentLength());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Get the location of this extent, as a LbAddr address.
     *
     * @return LbAddr address
     */
    public LbAddr getExtentLocation() {
        return extentLocation;
    }

    /**
     * Get the "Implementation Use" of the LongAd. Suggested implementation use Page 58 of 2.60 reference; as
     *     defined by 2.3.10.1. Some of this data is flags, the main one being a 1 if the data has been erased in a
     *     RW volume. The class file for this has more description.
     *
     * @return byte array of the data
     */
    public byte[] getImplementationUse() {
        return implementationUse;
    }

    /*
    Suggested implementation use
    (Page 58 of 2.60 reference)
    The Implementation Use bytes of the long_ad in all File Identifier Descriptors
    shall be used to store the UDF UniqueID for the file and directory namespace.

    The Implementation Use bytes of a long_ad hold an ADImpUse structure as
    defined by 2.3.10.1. The four impUse bytes of that structure will be interpreted as
    a Uint32 holding the UDF UniqueID.

    struct ADImpUse
        {
        Uint16 flags;
        byte impUse[4];
        }

        The Implementation Use bytes of a long_ad hold an ADImpUse structure as
        defined by 2.3.10.1. The four impUse bytes of that structure will be interpreted as
        a Uint32 holding the UDF UniqueID.
        ADImpUse structure holding UDF UniqueID
        RBP Length Name Contents
        0 2 Flags (see 2.3.10.1) Uint16
        2 4 UDF UniqueID Uint32
        Section 3.2.1 Logical Volume Header Descriptor describes how UDF UniqueID
        field in Implementation Use bytes of the long_ad in the File Identifier Descriptor
        and the UniqueID field in the File Entry and Extended File Entry are set.
     */

    // Currently it looks like the only real use of the flags, are if it starts with a 0 it is not erased, 1 is erased.
}
