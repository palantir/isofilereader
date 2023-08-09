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

package com.palantir.isofilereader.isofilereader.udf;

import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import com.palantir.isofilereader.isofilereader.udf.types.files.FileEntry;
import com.palantir.isofilereader.isofilereader.udf.types.files.FileIdentifierDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.files.FileSetDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.AnchorVolumePointer;
import com.palantir.isofilereader.isofilereader.udf.types.toc.GenericDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.ImplUseVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.LogicalVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.LogicalVolumeIntegrityDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.PartitionDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.PrimaryVolumeDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.TerminatingDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.toc.UnallocatedSpaceDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.types.LongAd;
import com.palantir.isofilereader.isofilereader.udf.types.types.Tag;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of <a href="http://www.osta.org/specs/pdf/udf260.pdf">Standards Doc</a>.
 * This is the <a href="https://www.ecma-international.org/wp-content/uploads/ECMA-167_3rd_edition_june_1997.pdf">upstream standard</a>
 * of that doc.
 */
@SuppressWarnings("StrictUnusedVariable")
public class UdfIsoReader {
    private final File isoFile;
    private List<GenericDescriptor> discDescriptors = null;
    private List<Long> udfAnchorLocations = null;

    // All the locations within a partition are relative, we need to be able to get where that physically is on the disc
    private final Map<Integer, Long> partitionLogicalStart = new HashMap<>();

    private char separatorChar = File.separatorChar;

    public UdfIsoReader(File isoFile) {
        this.isoFile = isoFile;
    }

    /**
     * Check if the image contains UDF data.
     *
     * @return true if found, false if not
     */
    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    public boolean checkForUdfData() {
        // UDF says the starting pointer should be at either Logical Sector 256 (524,288 bytes in), or N - 256, or N.
        // N is the last sector on the media. In practice first and last seem to be it.
        udfAnchorLocations = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(isoFile, "r")) {
            // This is not supposed to be a valid location, but some images seem to start here... ImgBurn is one of them
            boolean fakeFirstSpotForData = checkSpotForUdfData(file, 32);
            if (fakeFirstSpotForData) {
                udfAnchorLocations.add(32L);
            }

            boolean firstSpotForData = checkSpotForUdfData(file, 256);
            if (firstSpotForData) {
                udfAnchorLocations.add(256L);
            }
            long lastSector = file.length() / IsoFormatConstant.BYTES_PER_SECTOR;
            lastSector -= 1;
            boolean secondSpotForData = checkSpotForUdfData(file, (lastSector - 256));
            if (secondSpotForData) {
                udfAnchorLocations.add(lastSector - 256);
            }
            boolean lastSpotForData = checkSpotForUdfData(file, lastSector);
            if (lastSpotForData) {
                udfAnchorLocations.add(lastSector);
            }
            // The standard says 2/3 tag locations have to be valid, except a bunch of images dont follow this.
            return udfAnchorLocations.size() >= 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the set separation character between files.
     *
     * @return char of char
     */
    public char getSeparatorChar() {
        return separatorChar;
    }

    /**
     * Set which separator should be used on this system. / for most systems, \\ for windows.
     *
     * @param separatorChar char to use, defaults to File.separatorChar.
     */
    public void setSeparatorChar(char separatorChar) {
        this.separatorChar = separatorChar;
    }

    /**
     * A map of partition number to start of that partition, this can be useful when looking for relative position of
     * data. Note this is an absolute location.
     *
     * @return Map of PartitionId, location
     */
    public Map<Integer, Long> getPartitionLogicalStart() {
        return partitionLogicalStart;
    }

    /**
     * Jump to a logical sector of the provided image, and check if there is valid UDF looking data there.
     *
     * @param file raw ISO file
     * @param logicalSector logical sector to jump to
     * @return boolean of a valid UDF segment or not
     * @throws IOException Read errors at that location
     */
    private boolean checkSpotForUdfData(RandomAccessFile file, long logicalSector) throws IOException {
        file.seek(logicalSector * IsoFormatConstant.BYTES_PER_SECTOR);

        byte[] data = new byte[16];
        int read = file.read(data, 0, 16);
        if (read != 16) {
            // We should have been able to get at least 16 bytes to get a tag here.
            return false;
        }

        Tag firstTag = new Tag(data);
        return firstTag.verifyValidTag(logicalSector);
    }

    /**
     * Standard entry point to get all files off the image.
     */
    public UdfInternalDataFile[] getAllFiles() throws UdfFormatException {
        // We can assume this is a valid udf disk.
        UdfInternalDataFile[] rootFiles;
        if (discDescriptors == null) {
            getDiscDescriptors();
        }
        try (RandomAccessFile file = new RandomAccessFile(isoFile, "r")) {
            rootFiles = indexFileData(file);
        } catch (IOException | UdfFormatException e) {
            throw new RuntimeException(e);
        }
        return rootFiles;
    }

    /**
     * Read a Table of contents header, using logical positioning. Logical * Sector size = position.
     *
     * @param file Raw File to read
     * @param logicalPos logical position from start of image
     * @return byte array of item read
     * @throws IOException if a failure to read occurs we can throw a IOException
     */
    private byte[] readTocItem(RandomAccessFile file, long logicalPos) throws IOException {
        return readTocItemRaw(file, logicalPos * IsoFormatConstant.BYTES_PER_SECTOR);
    }

    private byte[] readTocItemRaw(RandomAccessFile file, long purePosition) throws IOException {
        file.seek(purePosition);
        byte[] data = new byte[16];
        int read = file.read(data, 0, 16);
        if (read != 16) {
            return new byte[0];
        }

        Tag firstTag = new Tag(data);

        file.seek(purePosition);
        data = new byte[firstTag.getDescriptorCrcLengthAsInt() + 16];
        read = file.read(data, 0, data.length);
        if (read != data.length) {
            return new byte[0];
        }
        return data;
    }

    /**
     * There are many tables to go through when traversing a disc, this will go through them.
     * @param file The random access file to use
     * @param pos the logical block number to read
     * @throws IOException if the image fails to read
     */
    @SuppressWarnings("ReadReturnValueIgnored")
    private void recursiveTableLookup(RandomAccessFile file, long pos, long stoppingPos)
            throws IOException, UdfFormatException {
        // Page 136 is the DVD example
        byte[] descriptor = readTocItem(file, pos);
        Tag tagOfDescriptor = new Tag(descriptor);

        switch (tagOfDescriptor.getTagIdentifierAsInt()) {
            case 0:
                // blank sectors, there are a lot in the header
                break;
            case Tag.PRIMARY_VOLUME:
                PrimaryVolumeDescriptor primaryVolumeDescriptor = new PrimaryVolumeDescriptor(descriptor);
                discDescriptors.add(primaryVolumeDescriptor);
                break;
            case Tag.ANCHOR_VOLUME_DESCRIPTOR_POINTER:
                AnchorVolumePointer anchorVolumePointer = new AnchorVolumePointer(descriptor);
                discDescriptors.add(anchorVolumePointer);
                int logicalSectorOfPrimaryLogicalVolumeDescriptor =
                        anchorVolumePointer.getMainVolumeDescriptor().getLocAsInt();
                long headerEndLocation =
                        (long) IsoFormatConstant.BYTES_PER_SECTOR * logicalSectorOfPrimaryLogicalVolumeDescriptor;
                headerEndLocation +=
                        anchorVolumePointer.getMainVolumeDescriptor().getLengthAsInt();
                recursiveTableLookup(file, logicalSectorOfPrimaryLogicalVolumeDescriptor, headerEndLocation);
                break;
            case Tag.IMPL_USE_VOLUME_DESCRIPTOR:
                ImplUseVolumeDescriptor implUseVolumeDescriptor = new ImplUseVolumeDescriptor(descriptor);
                discDescriptors.add(implUseVolumeDescriptor);
                break;
            case Tag.PARTITION_DESCRIPTOR:
                PartitionDescriptor partitionDescriptor = new PartitionDescriptor(descriptor);
                discDescriptors.add(partitionDescriptor);
                // These are on page 74 of the 1997 doc
                break;
            case Tag.LOGICAL_VOLUME_DESCRIPTOR:
                LogicalVolumeDescriptor logicalVolumeDescriptor = new LogicalVolumeDescriptor(descriptor);
                discDescriptors.add(logicalVolumeDescriptor);
                int logicalSectorOfNextIntegritySeqExt =
                        logicalVolumeDescriptor.getIntegritySequenceExtent().getLocAsInt();
                long logicalSectorOfNextIntegritySeqExtEnd =
                        (long) IsoFormatConstant.BYTES_PER_SECTOR * logicalSectorOfNextIntegritySeqExt;
                logicalSectorOfNextIntegritySeqExtEnd +=
                        logicalVolumeDescriptor.getIntegritySequenceExtent().getLengthAsInt();
                recursiveTableLookup(file, logicalSectorOfNextIntegritySeqExt, logicalSectorOfNextIntegritySeqExtEnd);
                break;
            case Tag.UNALLOCATED_SPACE_DESCRIPTOR:
                UnallocatedSpaceDescriptor unallocatedSpaceDescriptor = new UnallocatedSpaceDescriptor(descriptor);
                discDescriptors.add(unallocatedSpaceDescriptor);
                break;
            case Tag.TERMINATING_DESCRIPTOR:
                TerminatingDescriptor terminatingDescriptor = new TerminatingDescriptor(descriptor);
                discDescriptors.add(terminatingDescriptor);
                break;
            case Tag.LOGICAL_VOLUME_INTEGRITY_DESCRIPTOR:
                LogicalVolumeIntegrityDescriptor logicalVolumeIntegrityDescriptor =
                        new LogicalVolumeIntegrityDescriptor(descriptor);
                discDescriptors.add(logicalVolumeIntegrityDescriptor);
                if (logicalVolumeIntegrityDescriptor.getNextIntegrityExtent().getLocAsInt() != 0) {
                    int logNextVolumeIntegritySector = logicalVolumeIntegrityDescriptor
                            .getNextIntegrityExtent()
                            .getLocAsInt();
                    long logNextVolumeIntegritySectorEnd =
                            (long) IsoFormatConstant.BYTES_PER_SECTOR * logNextVolumeIntegritySector;
                    logNextVolumeIntegritySectorEnd += logicalVolumeIntegrityDescriptor
                            .getNextIntegrityExtent()
                            .getLengthAsInt();
                    recursiveTableLookup(file, logNextVolumeIntegritySector, logNextVolumeIntegritySectorEnd);
                }
                break;
            default:
                throw new UdfFormatException("Unknown Descriptor Type: " + tagOfDescriptor.getTagIdentifierAsInt());
        }
        if (((pos + 1) * IsoFormatConstant.BYTES_PER_SECTOR) < stoppingPos) {
            recursiveTableLookup(file, pos + 1, stoppingPos);
        }
    }

    @SuppressWarnings("StrictUnusedVariable")
    private UdfInternalDataFile[] indexFileData(RandomAccessFile file) throws IOException, UdfFormatException {
        // How to read a DVD helps, that starts at page 135 of UDF 2.60
        PartitionDescriptor[] descriptor = (PartitionDescriptor[]) getSpecificDiscDescriptor(Tag.PARTITION_DESCRIPTOR);
        // PartitionDescriptor[] descriptor = getPartitionDescriptors();
        List<UdfInternalDataFile> rootFiles = new ArrayList<>();
        // There can be multiple partitions, I haven't seen this, but it can happen.
        for (PartitionDescriptor partitionDescriptor : descriptor) {
            long startOfPartition = partitionDescriptor.getPartitionStartingLocationAsInt();
            long partitionLength = partitionDescriptor.getPartitionLengthAsInt();
            for (long i = startOfPartition; i < (startOfPartition + partitionLength); ) {
                byte[] rawTocInfo = readTocItem(file, i);
                Tag tagOfDescriptor = new Tag(rawTocInfo);

                switch (tagOfDescriptor.getTagIdentifierAsInt()) {
                    case Tag.FILE_SET_DESCRIPTOR:
                        FileSetDescriptor fileSetDescriptor = new FileSetDescriptor(rawTocInfo);
                        partitionLogicalStart.put(
                                fileSetDescriptor
                                        .getRootDirectoryIcb()
                                        .getExtentLocation()
                                        .getPartitionReferenceAsInt(),
                                startOfPartition);
                        LongAd rootFolderLoc =
                                fileSetDescriptor.getRootDirectoryIcb(); // This should point to a File Entry
                        UdfInternalDataFile rootFolder = getFilesAndFoldersAtLocForFileEntries(
                                file,
                                i,
                                rootFolderLoc.getExtentLengthAsInt(),
                                rootFolderLoc.getExtentLocation().getLogicalBlockNumberAsLong(),
                                null);
                        rootFiles.add(rootFolder);
                        return rootFiles.toArray(new UdfInternalDataFile[0]);
                    case Tag.EXTENDED_FILE_ENTRY:
                        // This is a Metadata bitmap if it's here
                        FileEntry fileEntry = new FileEntry(rawTocInfo);
                        i += (fileEntry.getInfoLengthAsLong() / IsoFormatConstant.BYTES_PER_SECTOR);
                        break;
                    default:
                        throw new UdfFormatException("Expected File Set Descriptor or EXTENDED_FILE_ENTRY "
                                + "and did not receive it. Disc table of contents or library error.");
                }
            }
        }

        return rootFiles.toArray(new UdfInternalDataFile[0]);
    }

    private UdfInternalDataFile getFilesAndFoldersAtLocForFileEntries(
            RandomAccessFile file,
            long rootPartitionLogicalSector,
            int lengthOfRecords,
            long localRelativeLogicalSector,
            FileIdentifierDescriptor fileIdentifierDescriptor)
            throws IOException, UdfFormatException {
        // We need to get the Allocation Descriptor to find the File Identity Descriptors of this
        byte[] rawTocInfo = readTocItem(file, localRelativeLogicalSector + rootPartitionLogicalSector);
        Tag tagOfDescriptor = new Tag(rawTocInfo);
        if (tagOfDescriptor.getTagIdentifierAsInt() != Tag.FILE_ENTRY
                && tagOfDescriptor.getTagIdentifierAsInt() != Tag.EXTENDED_FILE_ENTRY) {
            throw new UdfFormatException(
                    "Expected File Entry and did not receive it. Disc table of contents or library error. Type: "
                            + tagOfDescriptor.getTagIdentifierAsInt());
        }
        FileEntry fileEntry = new FileEntry(rawTocInfo);
        switch (fileEntry.getIcbTag().getFileType()) {
            case FileEntry.FILE_AS_RAN_ACCESS_STREAM:
                int partitionId = fileIdentifierDescriptor
                        .getInformationControlBlock()
                        .getExtentLocation()
                        .getPartitionReferenceAsInt();
                long logicalPartitionStartingOffset = getPartitionLogicalStart().get(partitionId);
                return new UdfInternalDataFile(fileEntry, fileIdentifierDescriptor, logicalPartitionStartingOffset);
            case FileEntry.FOLDER:
                return getFilesAndFoldersAtLocForFileIdentifier(
                        file, rootPartitionLogicalSector, fileEntry, fileIdentifierDescriptor);
            default:
                throw new UdfFormatException("Not Implemented File Entry type: "
                        + Byte.toUnsignedInt(fileEntry.getIcbTag().getFileType()));
        }
    }

    private UdfInternalDataFile getFilesAndFoldersAtLocForFileIdentifier(
            RandomAccessFile file,
            long rootPartitionLogicalSector,
            FileEntry fileEntry,
            FileIdentifierDescriptor parentFolderInfo)
            throws IOException, UdfFormatException {
        int trackingLogical = 0;
        byte[] rawTocInfo = readTocItemRaw(
                file,
                ((fileEntry.getLocationInAllocationDescriptorAsInt() + rootPartitionLogicalSector)
                                * IsoFormatConstant.BYTES_PER_SECTOR)
                        + trackingLogical);
        trackingLogical += rawTocInfo.length;
        Tag tagOfDescriptor = new Tag(rawTocInfo);
        // The area starts with the folder, but it doesn't have more data than the earlier entry.
        if (tagOfDescriptor.getTagIdentifierAsInt() != Tag.FILE_IDENTIFIER_DESCRIPTOR) {
            throw new UdfFormatException(
                    "Error reading UDF disc. Expected a File Identifier Description and found other tag.");
        }

        UdfInternalDataFile parentFolder =
                new UdfInternalDataFile(fileEntry, parentFolderInfo, rootPartitionLogicalSector);

        List<UdfInternalDataFile> tempItems = new ArrayList<>();
        // Internally tracking as we move through bytes of the image
        for (; trackingLogical < fileEntry.getLengthInAllocationDescriptorAsInt(); ) {
            rawTocInfo = readTocItemRaw(
                    file,
                    ((fileEntry.getLocationInAllocationDescriptorAsInt() + rootPartitionLogicalSector)
                                    * IsoFormatConstant.BYTES_PER_SECTOR)
                            + trackingLogical);
            trackingLogical += rawTocInfo.length;
            tagOfDescriptor = new Tag(rawTocInfo);
            if (tagOfDescriptor.getTagIdentifierAsInt() != Tag.FILE_IDENTIFIER_DESCRIPTOR) {
                throw new UdfFormatException(
                        "Error reading UDF disc. Expected a File Identifier Description and found other tag.");
            }
            FileIdentifierDescriptor tempFileDescriptor = new FileIdentifierDescriptor(rawTocInfo);
            UdfInternalDataFile files = getFilesAndFoldersAtLocForFileEntries(
                    file,
                    rootPartitionLogicalSector,
                    tempFileDescriptor.getInformationControlBlock().getExtentLengthAsInt(),
                    tempFileDescriptor
                            .getInformationControlBlock()
                            .getExtentLocation()
                            .getLogicalBlockNumberAsLong(),
                    tempFileDescriptor);
            tempItems.add(files);
        }
        parentFolder.addChildren(tempItems.toArray(tempItems.toArray(new UdfInternalDataFile[0])));
        return parentFolder;
    }

    /**
     * Get all the disc descriptors, this will check if they have been read before, and if not attempt to read them
     * first.
     *
     * @return List of disc descriptors
     * @throws UdfFormatException this can occur if there is an issue reading underlying media
     */
    public List<GenericDescriptor> getDiscDescriptors() throws UdfFormatException {
        if (discDescriptors == null) {
            discDescriptors = new ArrayList<>();
            if (udfAnchorLocations == null && !checkForUdfData()) {
                throw new UdfFormatException("Image does not appear to be a UDF image.");
            }
            try (RandomAccessFile file = new RandomAccessFile(isoFile, "r")) {
                recursiveTableLookup(file, udfAnchorLocations.get(0), -1);
            } catch (IOException | UdfFormatException e) {
                throw new RuntimeException(e);
            }
        }
        return discDescriptors;
    }

    /**
     * Find a specific disc descriptor type, for example you want all the Tag.FILE_SET_DESCRIPTOR.
     *
     * @param tagType tag id to search for
     * @return array of tags matching that
     */
    public GenericDescriptor[] getSpecificDiscDescriptor(int tagType) {
        List<PartitionDescriptor> partitionDescriptor = new ArrayList<>();
        for (GenericDescriptor genericDescriptor : discDescriptors) {
            if (genericDescriptor.getDescriptorTag().getTagIdentifierAsInt() == tagType) {
                partitionDescriptor.add((PartitionDescriptor) genericDescriptor);
            }
        }
        return partitionDescriptor.toArray(new PartitionDescriptor[0]);
    }
}
