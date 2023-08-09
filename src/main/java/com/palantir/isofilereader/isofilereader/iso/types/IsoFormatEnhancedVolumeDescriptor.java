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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The Enhanced Volume Descriptor, a lot of these fields are in the Primary Volume Descriptor. The matching types
 * have been removed.
 */
@SuppressWarnings("StrictUnusedVariable")
public class IsoFormatEnhancedVolumeDescriptor extends IsoFormatPrimaryVolumeDescriptor {
    // 7 - Volume Flags *
    private final byte volumeFlags;
    // 88-119 - Escape Sequences *
    private final byte[] escapeCharSet;
    // 882 - Reserved for future use
    // 883-1394 - Application Use
    // 1395-2047 - Reserved for future use

    public IsoFormatEnhancedVolumeDescriptor(byte[] header) {
        super(header);
        volumeFlags = header[7];
        // 88-119 - Escape Sequences *
        escapeCharSet = Arrays.copyOfRange(header, 88, 120);
    }

    /**
     * get the flags for the volume.
     * @return byte for volume flags
     */
    public byte getVolumeFlags() {
        return volumeFlags;
    }

    /**
     * Get byte array for the escape characters in use.
     * @return Byte Array
     */
    public byte[] getEscapeCharSet() {
        return escapeCharSet;
    }

    /**
     * Supplemental and enhanced volume descriptors are technically different, this will return true if the descriptor
     * is a supplemental one.
     *
     * @return returns true for supplemental descriptors
     */
    public boolean isSupplementaryVolumeDescriptor() {
        return getVolumeDescriptorVersion() == 0x01;
    }

    /**
     * Supplemental and enhanced volume descriptors are technically different, this will return true if the descriptor
     * is an enhanced one.
     *
     * @return returns true for enhanced descriptors
     */
    public boolean isEnhancedVolumeDescriptor() {
        return getVolumeDescriptorVersion() == 0x02;
    }

    public static boolean validator(byte[] header) {
        // Minimal size
        if (header.length < 882) {
            return false;
        }

        // volumeDescriptorType
        if (header[0] != 0x02) {
            return false;
        }

        // standardIdentifier
        if (!Arrays.equals("CD001".getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(header, 1, 6))) {
            return false;
        }

        // volumeDescriptorVersion
        if (header[6] != 0x01 || header[6] != 0x02) {
            return false;
        }

        // Unused space should be 0
        if (header[7] != 0x00) {
            return false;
        }

        // TODO(#1): Check other spaces that are supposed ot be blank
        return true;
    }
}
