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

import com.palantir.isofilereader.isofilereader.GenericInternalIsoFile;
import com.palantir.isofilereader.isofilereader.Util;
import com.palantir.isofilereader.isofilereader.udf.types.files.FileEntry;
import com.palantir.isofilereader.isofilereader.udf.types.files.FileIdentifierDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Main file type for UDF images, this wraps the internal types UDF has for a much easier time accessing files.
 */
@SuppressWarnings("StrictUnusedVariable")
public class UdfInternalDataFile extends GenericInternalIsoFile {
    private final List<UdfInternalDataFile> children = new ArrayList<>();
    private final FileEntry thisFileEntry;
    private final FileIdentifierDescriptor thisFileDescriptor;
    private UdfInternalDataFile parent;
    private final long offset;

    public UdfInternalDataFile(FileEntry thisFileEntry, FileIdentifierDescriptor thisFileDescriptor, long offset) {
        this.thisFileEntry = thisFileEntry;
        this.thisFileDescriptor = thisFileDescriptor;
        this.offset = offset;
    }

    /**
     * Get all children objects under this folder.
     *
     * @return array of children objects
     */
    @Override
    public UdfInternalDataFile[] getChildren() {
        return children.toArray(new UdfInternalDataFile[0]);
    }

    @Override
    public final UdfInternalDataFile addChild(GenericInternalIsoFile child) {
        UdfInternalDataFile convertedChild = (UdfInternalDataFile) child;
        convertedChild.setParent(this);
        this.children.add(convertedChild);
        return convertedChild;
    }

    /**
     * Set the parent to the current object.
     *
     * @param parent parental object
     */
    public void setParent(UdfInternalDataFile parent) {
        this.parent = parent;
    }

    /**
     * Add an array of children onto this object.
     *
     * @param passedChildren are files under this folder (if its a folder)
     */
    @Override
    public void addChildren(GenericInternalIsoFile[] passedChildren) {
        UdfInternalDataFile[] convertedType = (UdfInternalDataFile[]) passedChildren;
        Arrays.stream(convertedType).forEach(each -> each.setParent(this));
        this.children.addAll(List.of(convertedType));
    }

    /**
     * UDF files have 2 underlying records, instead of getting a single underlying type as ISO does, this provides the
     * FileEntry.
     *
     * @return FileEntry record
     */
    public FileEntry getThisFileEntry() {
        return thisFileEntry;
    }

    /**
     * UDF files have 2 underlying records, instead of getting a single underlying type as ISO does, this provides the
     * FileIdentifierDescriptor.
     *
     * @return FileIdentifierDescriptor record
     */
    public FileIdentifierDescriptor getThisFileDescriptor() {
        return thisFileDescriptor;
    }

    /**
     * Return the filename of this specific file, if there is no FileDescriptor (think root folder) then a empty
     * string is returned.
     *
     * @return string of filename
     */
    @Override
    public String getFileName() {
        if (getThisFileDescriptor() != null) {
            return Util.convertDStringBytesToString(getThisFileDescriptor().getFileIdentifier());
        } else {
            return "";
        }
    }

    /**
     * Check if this is a folder, false if it's a file.
     *
     * @return is a folder
     */
    @Override
    public boolean isDirectory() {
        return thisFileEntry.getIcbTag().getFileType() == FileEntry.FOLDER;
    }

    /**
     * This returns an underlying record of the subtype, except UDF has 2, this function returns null, and
     * getThisFileEntry along with getThisFileDescriptor should be used instead.
     *
     * @return null
     */
    @Override
    public Optional<Object> getUnderlyingRecord() {
        return Optional.empty();
    }

    /**
     * Get the date the file was recorded as a Java Date. This will get the bytes from the timestamp and convert them.
     * If the bytes are wrong, it will still try to make a calendar entry with the data, thus will not return null or
     * optional.
     *
     * @return Date object
     */
    @Override
    public Optional<Date> getDateAsDate() {
        return Optional.ofNullable(getThisFileEntry().getModificationTime().getAsDate());
    }

    /**
     * Returns the full file name including parent data by traversing the tree up.
     *
     * @param separatorChar depending on OS you may want to change separator char
     * @return full string of filename starting with a '/'
     */
    @Override
    public String getFullFileName(char separatorChar) {
        List<String> reverseOrderStrings = new ArrayList<>();
        UdfInternalDataFile pointer = this;
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
     * Get the size of the file as a long.
     *
     * @return filesize
     */
    @Override
    public long getSize() {
        return this.thisFileEntry.getLengthInAllocationDescriptorAsInt();
    }

    /**
     * In a tree structure, get a parent, if this is the root it will return null.
     *
     * @return parent or null
     */
    @Override
    public UdfInternalDataFile getParent() {
        return this.parent;
    }

    /**
     * Does the file have a parent, simply checking for null and returning a boolean.
     *
     * @return boolean has parent
     */
    public boolean hasParent() {
        return (this.parent == null);
    }

    /**
     * Get the logical sector of the data this file represents. This is relative to the partition it is in for UDF!
     *
     * @return long of location of data
     */
    @Override
    public long getLogicalSectorLocation() {
        return offset + this.getThisFileEntry().getLocationInAllocationDescriptorAsInt();
    }
}
