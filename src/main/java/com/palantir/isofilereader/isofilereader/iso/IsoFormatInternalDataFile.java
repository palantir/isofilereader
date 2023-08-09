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

import com.palantir.isofilereader.isofilereader.GenericInternalIsoFile;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import com.palantir.isofilereader.isofilereader.iso.types.RockRidgeAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enhanced version of IsoDirectoryRecord with added on functions to work in a tree structure.
 */
public class IsoFormatInternalDataFile extends GenericInternalIsoFile {
    private final List<IsoFormatInternalDataFile> children = new ArrayList<>();
    private final IsoFormatDirectoryRecord isoDirectoryRecord;
    private final boolean useRockRidge;
    private IsoFormatInternalDataFile parent;

    public IsoFormatInternalDataFile(IsoFormatDirectoryRecord isoDirectoryRecord, boolean useRockRidge) {
        this.isoDirectoryRecord = isoDirectoryRecord;
        this.useRockRidge = useRockRidge;
    }

    /**
     * This is the parent folder of these children entries.
     *
     * @return sub-folders/sub-files of this folder
     */
    @Override
    public IsoFormatInternalDataFile[] getChildren() {
        return children.toArray(new IsoFormatInternalDataFile[0]);
    }

    /**
     * Add a single child object to this file.
     *
     * @param child child to add
     * @return that child with the parent added to its parental spot
     */
    @Override
    public IsoFormatInternalDataFile addChild(GenericInternalIsoFile child) {
        IsoFormatInternalDataFile convertedChild = (IsoFormatInternalDataFile) child;
        convertedChild.setParent(this);
        this.children.add(convertedChild);
        return convertedChild;
    }

    /**
     * Set the parent to the current object.
     *
     * @param parent parental object
     */
    public void setParent(IsoFormatInternalDataFile parent) {
        this.parent = parent;
    }

    /**
     * Add an array of children onto this object.
     *
     * @param passedChildren are files under this folder (if its a folder)
     */
    @Override
    public final void addChildren(GenericInternalIsoFile[] passedChildren) {
        IsoFormatInternalDataFile[] convertedType = (IsoFormatInternalDataFile[]) passedChildren;
        Arrays.stream(convertedType).forEach(each -> each.setParent(this));
        this.children.addAll(List.of(convertedType));
    }

    /**
     * Check if this is a folder, false if it's a file.
     *
     * @return is a folder
     */
    @Override
    public boolean isDirectory() {
        return isoDirectoryRecord.isDirectory();
    }

    /**
     * Get the raw underlying IsoDirectorRecord.
     *
     * @return raw directory entry
     */
    @Override
    public Optional<IsoFormatDirectoryRecord> getUnderlyingRecord() {
        return Optional.ofNullable(isoDirectoryRecord);
    }

    /**
     * Get the date the file was recorded as a Java Date.
     *
     * @return Date object
     */
    @Override
    public Optional<Date> getDateAsDate() {
        return getUnderlyingRecord().flatMap(IsoFormatDirectoryRecord::getDataAndTimeAsDate);
    }

    /**
     * Get the filename of the entry.
     *
     * @return string of filename, converted from UTF-8 or UTF-16
     */
    @Override
    public String getFileName() {
        String name;
        if (useRockRidge && hasRockRidgeName()) {
            // Scan for rock ridge attribute
            name = isoDirectoryRecord.getRockRidgeAttributeMap().get("NM").getDataBlobAsNmAsString();
        } else {
            name = isoDirectoryRecord.getFileIdentifierAsString();
        }
        if (name.equals(".")) {
            return "";
        }
        return name;
    }

    private boolean hasRockRidgeName() {
        for (Map.Entry<String, RockRidgeAttribute> rockRidgeAttribute :
                isoDirectoryRecord.getRockRidgeAttributeMap().entrySet()) {
            if (rockRidgeAttribute.getKey().equals("NM")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the full file name with the parent file.
     *
     * @param separatorChar depending on OS you may want to change separator char
     * @return full filename as a string starting with / or \
     */
    @Override
    public String getFullFileName(char separatorChar) {
        return this.isoDirectoryRecord.getParent() + separatorChar + getFileName();
    }

    /**
     * This can get the same name as getFullFileName, but that requires the full tree structure around the
     * IsoFormatInternalDataFile to exist. Sometimes code may convert a single Directory Record into a
     * InternalDataFile, then this will fail.
     *
     * @param separatorChar depending on OS you may want to change separator char
     * @return full filename as a string starting with / or \
     */
    public String getFullFileNameThroughTraversal(char separatorChar) {
        List<String> reverseOrderStrings = new ArrayList<>();
        IsoFormatInternalDataFile pointer = this;
        while (pointer.getParent() != null) {
            reverseOrderStrings.add(pointer.getFileName());
            reverseOrderStrings.add(String.valueOf(separatorChar));
            pointer = pointer.getParent();
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = reverseOrderStrings.size() - 1; i >= 0; i--) {
            stringBuilder.append(reverseOrderStrings.get(i));
        }
        return stringBuilder.toString();
    }

    /**
     * Get the size of the file.
     *
     * @return long of file size
     */
    @Override
    public long getSize() {
        return isoDirectoryRecord.getDataLengthAsLong();
    }

    /**
     * String of parent file name, used to tell full file name and position.
     *
     * @return string with specified file separators of full file name
     */
    @Override
    public IsoFormatInternalDataFile getParent() {
        return parent;
    }

    /**
     * Get the logical location of the data this file represents.
     *
     * @return long of the logical sector of this data
     */
    @Override
    public long getLogicalSectorLocation() {
        return isoDirectoryRecord.getLocOfExtAsLong();
    }
}
