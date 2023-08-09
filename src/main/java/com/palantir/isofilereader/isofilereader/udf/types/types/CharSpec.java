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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SuppressWarnings("StrictUnusedVariable")
public class CharSpec {
    // Uint8 CharacterSetType;
    private final byte characterSetType;
    // byte CharacterSetInfo[63]
    private final byte[] characterSetInfo;

    /**
     * Character set type, the type should be 0, and the set info will be "OSTA Compressed Unicode".
     *
     * @param record 64 bytes
     */
    public CharSpec(byte[] record) {
        this.characterSetType = record[0];
        this.characterSetInfo = Arrays.copyOfRange(record, 1, 64);
    }

    /**
     * Spec: The CharacterSetType field shall have the value of 0 to indicate the CS0 coded
     * character set.
     *
     * @return byte of character set type
     */
    public byte getCharacterSetType() {
        return characterSetType;
    }

    /**
     * The CharacterSetInfo field shall contain the following byte values with the
     * remainder of the field set to a value of 0.
     * #4F, #53, #54, #41, #20, #43, #6F, #6D, #70, #72, #65, #73, #73, #65,
     * #64, #20, #55, #6E, #69, #63, #6F, #64, #65
     * The above byte values represent the following ASCII string:
     * “OSTA Compressed Unicode”
     *
     * @return byte array of charsetinfo
     */
    public byte[] getCharacterSetInfo() {
        return characterSetInfo;
    }

    /**
     * Get the same CharacterSetInfo, but as a string.
     *
     * @return String of the CharacterSetInfo byte array
     */
    public String getCharacterSetInfoAsString() {
        return new String(getCharacterSetInfo(), StandardCharsets.US_ASCII);
    }
}
