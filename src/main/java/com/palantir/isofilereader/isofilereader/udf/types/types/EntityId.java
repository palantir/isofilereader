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
public class EntityId {
    // Uint8 Flags
    private final byte flags;
    // char Identifier[23]
    private final byte[] identifier;
    // char IdentifierSuffix[8]
    private final byte[] identifierSuffix;

    /**
     * Requires 32 bytes in to init.
     *
     * @param record byte array of the EntityId.
     */
    public EntityId(byte[] record) {
        this.flags = record[0];
        this.identifier = Arrays.copyOfRange(record, 1, 24);
        this.identifierSuffix = Arrays.copyOfRange(record, 24, 32);
    }

    /**
     * Standard say this should be zero.
     *
     * @return a byte of zero
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * DeveloperID starting with a *.
     *
     * @return byte array of the developerId
     */
    public byte[] getIdentifier() {
        return identifier;
    }

    /**
     * Get the developerId as a string.
     *
     * @return string
     */
    public String getIdentifierAsString() {
        return new String(getIdentifier(), StandardCharsets.US_ASCII);
    }

    /**
     * Byte array of IdSuffix, UDF 2.60 2.1.5.3. 2 bytes of UDF revision,
     * then 1 byte of Domain Flags. UDF Revision example, version 2.60
     * is written as #0260. Domain Flags, bit 0 is hardwriteprotect,
     * bit 1 is soft right protect.
     *
     * @return byte array
     */
    public byte[] getIdentifierSuffix() {
        return identifierSuffix;
    }
}
