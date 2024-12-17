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

import com.palantir.isofilereader.isofilereader.iso.types.AbstractVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatEnhancedVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatPrimaryVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatVolumePartitionDescriptor;
import com.palantir.isofilereader.isofilereader.iso.types.RockRidgeAttribute;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TraditionalIsoReader {
    private final File isoFile;
    private char separatorChar = File.separatorChar;
    private int tableOfContentsInUse = -1;
    private boolean useRockRidgeOverStandard = true;

    public TraditionalIsoReader(File isoFile) {
        this.isoFile = isoFile;
    }

    /**
     * Get which separator character is in use.
     * @return char in use
     */
    public char getSeparatorChar() {
        return separatorChar;
    }

    /**
     * Allow overriding separator char to anything the user wants.
     * @param separatorChar char to use
     */
    public void setSeparatorChar(char separatorChar) {
        this.separatorChar = separatorChar;
    }

    /**
     * Array position of the table set to use, this is for getVolumeDescriptors() array.
     * @return ID number, or -1 if not set
     */
    public int getTableOfContentsInUse() {
        return tableOfContentsInUse;
    }

    /**
     * There can be many tables of contents on an image, using the correct one can read to the best filename selection.
     * @param tableOfContentsInUse int to set it to
     */
    public void setTableOfContentsInUse(int tableOfContentsInUse) {
        this.tableOfContentsInUse = tableOfContentsInUse;
    }

    /**
     * Getter for if rock ridge is enabled.
     * @return yes it is or no it is not
     */
    public boolean isUseRockRidgeOverStandard() {
        return useRockRidgeOverStandard;
    }

    /**
     * Rock Ridge can be used for filename info, should it be used ahead of standard table info?.
     *
     * @param useRockRidgeOverStandard yes it should
     */
    public void setUseRockRidgeOverStandard(boolean useRockRidgeOverStandard) {
        this.useRockRidgeOverStandard = useRockRidgeOverStandard;
    }

    /**
     * Get the internal data files for a traditional iso.
     * @param file file we are reading from
     * @param logicalSector which logical sector to start at
     * @param size size of the file
     * @param parent parent string, this makes the files much easier to work with
     * @return Array of data files
     * @throws IOException can be thrown if file can not be read
     */
    @SuppressWarnings("for-rollout:PreferSafeLoggableExceptions")
    public final IsoFormatInternalDataFile[] getInternalDataFiles(
            RandomAccessFile file, long logicalSector, long size, String parent) throws IOException {
        byte[] headerInfo = new byte[IsoFormatConstant.BYTES_PER_SECTOR];
        List<IsoFormatInternalDataFile> gatheringFiles = new ArrayList<>();
        IsoFormatInternalDataFile[] recordsRead;
        for (int i = 0; i < Math.ceil((double) size / IsoFormatConstant.BYTES_PER_SECTOR); i++) {
            file.seek(IsoFormatConstant.BYTES_PER_SECTOR * (logicalSector + i));
            // Let's add here a loop to get files in 2048 bit chunks, data will never go across the 2048 barrier
            int read = file.read(headerInfo, 0, headerInfo.length);
            if (read != headerInfo.length) {
                return null;
            }
            IsoFormatDirectoryReader reader = new IsoFormatDirectoryReader(headerInfo, parent);
            recordsRead = reader.getRecordsAsIsoInternalDataFile(isUseRockRidgeOverStandard());
            for (IsoFormatInternalDataFile singleRecord : recordsRead) {
                if (singleRecord.getUnderlyingRecord().isEmpty()) {
                    throw new IOException("Underlying ISO header record not found where one should be.");
                }
                if (singleRecord.isDirectory()
                        && !singleRecord.getUnderlyingRecord().get().isTopLevelIdentifier()) {
                    singleRecord.addChildren(getInternalDataFiles(
                            file,
                            singleRecord.getUnderlyingRecord().get().getLocOfExtAsLong(),
                            singleRecord.getSize(),
                            parent + separatorChar + singleRecord.getFileName()));
                }
            }
            gatheringFiles.addAll(Arrays.asList(recordsRead));
        }

        return gatheringFiles.toArray(new IsoFormatInternalDataFile[0]);
    }

    public final IsoFormatDirectoryRecord[] getIsoDirectoryRecords(long logSect, long length, String parent)
            throws IOException {
        List<IsoFormatDirectoryRecord> recordLibrary = new ArrayList<>();
        long scanLength = length;
        if (scanLength < 2048) {
            scanLength = 2048;
        }
        try (RandomAccessFile file = new RandomAccessFile(isoFile, "r")) {
            for (int i = 0; i < (length / IsoFormatConstant.BYTES_PER_SECTOR); i++) {
                IsoFormatDirectoryRecord[] recordsRead = getRecordsAtSector(file, logSect, parent, i);
                if (recordsRead != null) {
                    recordLibrary.addAll(Arrays.asList(recordsRead));
                }
            }
        }
        return recordLibrary.toArray(new IsoFormatDirectoryRecord[0]);
    }

    /**
     * Take already processed IsoInternalDataFile array in and find the largest filename.
     * @param oneLevelOfIsoFiles One level of IsoInternalDataFile array, then recursively go through it and its children
     * @return largest file name, no filenames will return -1, but top header will usually return at least 1
     */
    public int getLongestFileNameWithoutRockRidge(IsoFormatInternalDataFile[] oneLevelOfIsoFiles) {
        int longestName = -1;

        for (IsoFormatInternalDataFile subFile : oneLevelOfIsoFiles) {
            if (subFile.isDirectory()
                    && subFile.getUnderlyingRecord().isPresent()
                    && !subFile.getUnderlyingRecord().get().isTopLevelIdentifier()) {
                int temp = getLongestFileNameWithoutRockRidge(subFile.getChildren());
                if (temp > longestName) {
                    longestName = temp;
                }
            } else {
                if (subFile.getFileName().length() > longestName) {
                    longestName = subFile.getFileName().length();
                }
            }
        }

        return longestName;
    }

    /**
     * For the currently selected ToC, get a IsoDirectoryRecord of the Root directory.
     * @return IsoDirectoryRecord of root folder
     * @throws IOException Error, could not read the file to get headers
     */
    public IsoFormatDirectoryRecord getRootDirectoryOfCurrentToC() throws IOException {
        AbstractVolumeDescriptor[] headers = getVolumeDescriptors();
        if (tableOfContentsInUse == -1) {
            // A scan was never run and/or the user has not set which ToC to use
            tableOfContentsInUse = headers.length - 1;
            // Worst case there are no headers and this resets the value to -1
        }

        if (tableOfContentsInUse == -1 || tableOfContentsInUse >= headers.length) {
            return null;
        }

        IsoFormatDirectoryRecord currentFileDirectoryRecord;
        switch (headers[tableOfContentsInUse].getVolumeDescriptorType()) {
            case AbstractVolumeDescriptor.IsoEnhancedVolumeDescriptor:
                currentFileDirectoryRecord = ((IsoFormatEnhancedVolumeDescriptor) headers[tableOfContentsInUse])
                        .getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
                break;
            case AbstractVolumeDescriptor.IsoPrimaryVolumeDescriptor:
            default:
                currentFileDirectoryRecord = ((IsoFormatPrimaryVolumeDescriptor) headers[tableOfContentsInUse])
                        .getDirectoryRecordForRootDirectoryAsIsoDirectorRecord();
                break;
        }
        return currentFileDirectoryRecord;
    }

    private IsoFormatDirectoryRecord[] getRecordsAtSector(
            RandomAccessFile file, long logSector, String parent, int loop) throws IOException {
        long seekLocation =
                (IsoFormatConstant.BYTES_PER_SECTOR * logSector) + ((long) IsoFormatConstant.BYTES_PER_SECTOR * loop);
        file.seek(seekLocation);
        byte[] headerInfo = new byte[IsoFormatConstant.BYTES_PER_SECTOR];
        int read = file.read(headerInfo, 0, headerInfo.length);
        if (read != headerInfo.length) {
            return null;
        }
        IsoFormatDirectoryReader reader = new IsoFormatDirectoryReader(headerInfo, parent);
        IsoFormatDirectoryRecord[] recordsRead = reader.getRecords();
        List<IsoFormatDirectoryRecord> collectingRecords = new ArrayList<>(Arrays.asList(recordsRead));
        for (IsoFormatDirectoryRecord singleRecord : recordsRead) {
            if (singleRecord.isDirectory() && !singleRecord.isTopLevelIdentifier()) {
                collectingRecords.addAll(Arrays.asList(getIsoDirectoryRecords(
                        singleRecord.getLocOfExtAsLong(),
                        singleRecord.getDataLengthAsLong(),
                        parent + separatorChar + singleRecord.getFileIdentifierAsString())));
            }
        }
        return collectingRecords.toArray(new IsoFormatDirectoryRecord[0]);
    }

    /**
     * Read all the different volume descriptors and then make an array of them.
     *
     * @return Array of volume descriptors
     * @throws IOException throws error if image file can not be read
     */
    public AbstractVolumeDescriptor[] getVolumeDescriptors() throws IOException {
        List<AbstractVolumeDescriptor> sectors = new ArrayList<>();
        byte[] headerInfo = new byte[2048];
        long loc = IsoFormatConstant.BYTES_PER_SECTOR * IsoFormatConstant.BUFFER_SECTORS;
        boolean foundTerminator = false;
        long mTableLoc = isoFile.length();
        try (RandomAccessFile file = new RandomAccessFile(isoFile, "r")) {
            file.seek(loc);
            while (loc < mTableLoc && !foundTerminator) {
                loc += file.read(headerInfo, 0, 2048);
                AbstractVolumeDescriptor tempDescriptor = new AbstractVolumeDescriptor(headerInfo);
                switch (tempDescriptor.getVolumeDescriptorTypeAsInt()) {
                    case AbstractVolumeDescriptor.IsoPrimaryVolumeDescriptor:
                        IsoFormatPrimaryVolumeDescriptor temp = new IsoFormatPrimaryVolumeDescriptor(headerInfo);
                        mTableLoc = 2048 * temp.getLPathTableLocAsLong();
                        if (2048 * temp.getLocOfMPathTableAsLong() < mTableLoc) {
                            mTableLoc = 2048 * temp.getLocOfMPathTableAsLong();
                        }
                        sectors.add(temp);
                        break;
                    case AbstractVolumeDescriptor.IsoEnhancedVolumeDescriptor:
                        sectors.add(new IsoFormatEnhancedVolumeDescriptor(headerInfo));
                        break;
                    case AbstractVolumeDescriptor.IsoVolumePartitionDescriptor:
                        sectors.add(new IsoFormatVolumePartitionDescriptor(headerInfo));
                        break;
                    case AbstractVolumeDescriptor.VolumeDescriptorTerminator:
                        foundTerminator = true;
                        break;
                }
            }
        }
        return sectors.toArray(new AbstractVolumeDescriptor[0]);
    }

    /**
     * Scan for named entries in rock ridge, return an int of the largest one found.
     * @param entries list of all the file entries
     * @return -1 for no rock ridge entries, anything larger is the size of the entries
     */
    public int scanForNmEntries(IsoFormatInternalDataFile[] entries) {
        int largestrecord = -1;
        for (IsoFormatInternalDataFile singleRecord : entries) {
            if (singleRecord.isDirectory()) {
                int temp = scanForNmEntries(singleRecord.getChildren());
                if (temp > largestrecord) {
                    largestrecord = temp;
                }
            } else {
                // This is a file
                for (Map.Entry<String, RockRidgeAttribute> rockRidgeAttribute : singleRecord
                        .getUnderlyingRecord()
                        .get()
                        .getRockRidgeAttributeMap()
                        .entrySet()) {
                    if (rockRidgeAttribute.getKey().equals("NM")
                            && rockRidgeAttribute.getValue().getLength() > largestrecord) {
                        largestrecord = rockRidgeAttribute.getValue().getLength();
                    }
                }
            }
        }
        return largestrecord;
    }
}
