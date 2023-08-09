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

import java.util.Arrays;

/**
 * Page 73 of UDF 2.60 doc.
 */
@SuppressWarnings("StrictUnusedVariable")
public class IcbTag {
    // Uint32 PriorRecordedNumberOfDirectEntries;
    private final byte[] priorRecordedNumberOfDirectEntries;
    // Uint16 StrategyType;
    private final byte[] strategyType;
    // byte StrategyParameter[2];
    private final byte[] strategyParameter;
    // Uint16 MaximumNumberOfEntries;
    private final byte[] maximumNumberOfEntries;
    // byte Reserved;
    private final byte reserved;
    // Uint8 FileType;
    private final byte fileType;
    // Lb_addr ParentICBLocation;
    private final LbAddr parentIcbLocation;
    // Uint16 Flags;
    private final byte[] flags;

    /**
     * An Information Control Block (ICB) tag.
     *
     * @param record requires 20 bytes
     */
    public IcbTag(byte[] record) {
        // Uint32 PriorRecordedNumberOfDirectEntries;
        this.priorRecordedNumberOfDirectEntries = Arrays.copyOfRange(record, 0, 4);
        // Uint16 StrategyType;
        this.strategyType = Arrays.copyOfRange(record, 4, 6);
        // byte StrategyParameter[2];
        this.strategyParameter = Arrays.copyOfRange(record, 6, 8);
        // Uint16 MaximumNumberOfEntries;
        this.maximumNumberOfEntries = Arrays.copyOfRange(record, 8, 10);
        // byte Reserved;
        this.reserved = record[10];
        // Uint8 FileType;
        this.fileType = record[11];
        // Lb_addr ParentICBLocation;
        this.parentIcbLocation = new LbAddr(Arrays.copyOfRange(record, 12, 18));
        // Uint16 Flags;
        this.flags = Arrays.copyOfRange(record, 18, 20);
    }

    /**
     * Number of Direct Entries recorded in the Icb Hierarchy prior to this entry.
     *
     * @return byte array
     */
    public byte[] getPriorRecordedNumberOfDirectEntries() {
        return priorRecordedNumberOfDirectEntries;
    }

    /**
     * Strategy type for building the Icb Hierarchy. Specified in 14.6.2/ Page 4/24 of original UDF document.
     *
     * @return byte array of strategy
     */
    public byte[] getStrategyType() {
        return strategyType;
    }

    /**
     * This is used depending on the Strategy Type in use.
     *
     * @return byte array
     */
    public byte[] getStrategyParameter() {
        return strategyParameter;
    }

    /**
     * Max number of entries in this Icb both direct and indirect.
     *
     * @return byte array
     */
    public byte[] getMaximumNumberOfEntries() {
        return maximumNumberOfEntries;
    }

    /**
     * This field is reserved for future usees and originally should be set to 0, getter created in case of future
     * usecase.
     *
     * @return byte
     */
    public byte getReserved() {
        return reserved;
    }

    /**
     * Get byte of file type, these types are included as static types on FileEntry.
     *
     * @return byte
     */
    public byte getFileType() {
        return fileType;
    }

    /**
     * If the strategy type is not 4, then this contains an indirect entry specifying the Icb of this descriptor. If
     * the type is 4, this should be the previous Icb.
     *
     * @return LbAddr
     */
    public LbAddr getParentIcbLocation() {
        return parentIcbLocation;
    }

    /**
     * Flags on the Tag. There are many of these, 14.6.8/Page 4/26 of the original standard goes over these.
     *
     * @return byte array
     */
    public byte[] getFlags() {
        return flags;
    }
}
