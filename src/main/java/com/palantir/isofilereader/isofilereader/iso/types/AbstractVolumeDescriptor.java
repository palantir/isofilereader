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

package com.palantir.isofilereader.isofilereader.iso.types;

import java.util.Arrays;

public class AbstractVolumeDescriptor {
    public static final byte IsoPrimaryVolumeDescriptor = 0x01;
    public static final byte IsoEnhancedVolumeDescriptor = 0x02;
    public static final byte IsoVolumePartitionDescriptor = 0x03;
    public static final byte VolumeDescriptorTerminator = (byte) 0xFF;

    // 0 - Byte in descriptor
    private final byte volumeDescriptorType;
    // 1-5
    private final byte[] standardIdentifier;
    // 6
    private final byte volumeDescriptorVersion;

    public AbstractVolumeDescriptor(byte[] header) {
        volumeDescriptorType = header[0];
        standardIdentifier = Arrays.copyOfRange(header, 1, 6);
        volumeDescriptorVersion = header[6];
    }

    /**
     * This is always 1 for the primary descriptor, 2 for Enhanced.
     *
     * @return byte of volume description type
     */
    public byte getVolumeDescriptorType() {
        return volumeDescriptorType;
    }

    /**
     * This is always 1 for the primary descriptor, 2 for Enhanced.
     *
     * @return int of volume description type
     */
    public int getVolumeDescriptorTypeAsInt() {
        return Byte.toUnsignedInt(getVolumeDescriptorType());
    }

    /**
     * For a valid descriptor this should always be "CD001".
     *
     * @return byte array of descriptor
     */
    public byte[] getStandardIdentifier() {
        return standardIdentifier;
    }

    /**
     * byte of the volume descriptor version, this changes usecases depending on teh descriptor type.
     *
     * @return byte of version
     */
    public byte getVolumeDescriptorVersion() {
        return volumeDescriptorVersion;
    }
}
