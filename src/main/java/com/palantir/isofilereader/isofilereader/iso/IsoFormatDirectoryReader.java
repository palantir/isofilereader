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

package com.palantir.isofilereader.isofilereader.iso;

import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class to go through the ISO/ECMA-119 format table of contents and get the IsoFormatDirectoryRecords.
 */
public class IsoFormatDirectoryReader {
    private IsoFormatDirectoryRecord[] records;
    private boolean cleanFinish;
    private int lastLoc;

    public IsoFormatDirectoryReader(byte[] header, String parent) {
        List<IsoFormatDirectoryRecord> collectingRecords = new ArrayList<>();

        int loc = 0;
        int finish = header.length;
        boolean finished = false;
        while (loc < finish && !finished) {
            int sizeOfRecord = Byte.toUnsignedInt(header[loc]);
            if (sizeOfRecord == 0 && scanRestOfRecord(header, loc)) {
                // Possibly end of records in block
                cleanFinish = true;
                finished = true;
                continue;
            }
            IsoFormatDirectoryRecord tempRecord;
            if (loc + sizeOfRecord > header.length) {
                // Reading the next record will take us out of this packet
                cleanFinish = false;
                lastLoc = loc;
                return;
            } else {
                tempRecord = new IsoFormatDirectoryRecord(Arrays.copyOfRange(header, loc, loc + sizeOfRecord), parent);
            }

            collectingRecords.add(tempRecord);
            loc += sizeOfRecord;
        }
        records = collectingRecords.toArray(new IsoFormatDirectoryRecord[0]);
    }

    /**
     * A field that can be checked if the bytes finished with clean data at the end.
     *
     * @return did the sector read finish cleanly
     */
    public boolean isCleanFinish() {
        return cleanFinish;
    }

    /**
     * Get the found IsoDirectoryRecord.
     *
     * @return array of all the records found on the iso
     */
    public IsoFormatDirectoryRecord[] getRecords() {
        return records;
    }

    /**
     * IsoInternalDataFile is a more advanced version of IsoDirectoryRecord that has supporting functions around it.
     *
     * @return Array of IsoInternalDataFile
     */
    public IsoFormatInternalDataFile[] getRecordsAsIsoInternalDataFile(boolean useRockRidge) {
        List<IsoFormatInternalDataFile> returningFiles = new ArrayList<>();
        for (IsoFormatDirectoryRecord record : getRecords()) {
            returningFiles.add(new IsoFormatInternalDataFile(record, useRockRidge));
        }
        return returningFiles.toArray(new IsoFormatInternalDataFile[0]);
    }

    /**
     * In the event the scanner doesn't finish cleanly, this will return the last location of an end of a record.
     *
     * @return int of last location of header scan
     */
    public int getLastLoc() {
        return lastLoc;
    }

    private boolean scanRestOfRecord(byte[] header, int loc) {
        for (int i = loc; i < header.length; i++) {
            if (header[i] != 0x0) {
                return false;
            }
        }
        return true;
    }
}
