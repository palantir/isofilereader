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
 * Rock ridge extensions to use the system area of a directory record.
 * <a href="https://web.archive.org/web/20170404043745/http://www.ymi.com/ymi/sites/default/files/pdf/Rockridge.pdf">Rock Ridge Standard</a>
 */
public class RockRidgeAttribute {
    private final byte[] signature;
    private final byte length;
    private final byte[] dataBlob;

    public RockRidgeAttribute(byte[] chunk) {
        signature = Arrays.copyOfRange(chunk, 0, 2);
        length = chunk[2];
        dataBlob = Arrays.copyOfRange(chunk, 3, chunk.length);
    }

    /**
     * Signature of extension.
     *
     * @return two bytes
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Get the 2 letter signature as a string.
     *
     * @return 2 letter string
     */
    public String getSignatureAsString() {
        return new String(getSignature(), StandardCharsets.UTF_8);
    }

    /**
     * Byte of length of the data.
     *
     * @return byte of length
     */
    public byte getLength() {
        return length;
    }

    /**
     * Data to end of packet.
     *
     * @return byte array of data, usage varies based on signature
     */
    public byte[] getDataBlob() {
        return dataBlob;
    }

    /**
     * Treat this data blob as a NM signature, which is a file name and get the name as a string.
     *
     * @return string of file name
     */
    public String getDataBlobAsNmAsString() {
        // byte 0-1 are NM
        // byte 2 is length
        // byte 3 is 3 for version       Datablob    0
        // byte 4 is flags               Datablob    1
        // byte 5 -> Length is filename  Datablob    2
        return new String(Arrays.copyOfRange(getDataBlob(), 2, getDataBlob().length), StandardCharsets.UTF_8);
    }
}
