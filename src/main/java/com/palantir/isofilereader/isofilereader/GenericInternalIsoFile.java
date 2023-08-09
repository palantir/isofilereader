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

package com.palantir.isofilereader.isofilereader;

import java.util.Date;
import java.util.Optional;

/**
 * The implemented but Raw type that IsoInternalDataFile and UdfInternalDataFile are based off of. This class should not
 * be used itself.
 */
@SuppressWarnings("StrictUnusedVariable")
public abstract class GenericInternalIsoFile extends AbstractInternalFile<GenericInternalIsoFile> {
    /**
     * getChildren should return the child files to this file, assuming this file is a folder.
     * @return Array of child files
     */
    @Override
    public GenericInternalIsoFile[] getChildren() {
        return new GenericInternalIsoFile[0];
    }

    /**
     * Add a single child file to this, now, presumed directory. Also update the child object, marking this file as
     * its parent.
     *
     * @param genericInternalIsoFile single child to add
     * @return return the child object with THIS object noted as its parent
     */
    @Override
    public abstract GenericInternalIsoFile addChild(GenericInternalIsoFile genericInternalIsoFile);

    /**
     * An array variant of addChild, we are adding multiple children as an array. Each will have this file set as its
     * parent.
     *
     * @param abstractInternalFiles are files under this folder (if its a folder)
     */
    @Override
    public void addChildren(GenericInternalIsoFile[] abstractInternalFiles) {}

    /**
     * Is this file being represented a directory.
     *
     * @return boolean true/false
     */
    @Override
    public boolean isDirectory() {
        return false;
    }

    /**
     * Return the raw type underneath, this is useful if someone is doing advanced processing of directory records.
     *
     * @return underlying object, this can be different types depending on the implemented abstraction
     */
    @Override
    public Object getUnderlyingRecord() {
        return null;
    }

    /**
     * Get the recorded time of the file as a Java Date.
     *
     * @return Date Object optional, there is a chance the date can not be parsed from the underlying record
     */
    @Override
    public Optional<Date> getDateAsDate() {
        return Optional.empty();
    }

    /**
     * Get the filename, without parent information.
     *
     * @return string of filename
     */
    @Override
    public String getFileName() {
        return null;
    }

    /**
     * Get the full filename, with parent names, using the char provided as separator.
     *
     * @param separatorChar depending on OS you may want to change separator char
     * @return string of name
     */
    @Override
    public String getFullFileName(char separatorChar) {
        return null;
    }

    /**
     * Size of the file, implementors of this class should make sure whatever method their table of contents uses
     * returns a file size here.
     *
     * @return size as a long
     */
    @Override
    public long getSize() {
        return 0;
    }

    /**
     * We support storing files in a tree format, this gets the parent object.
     *
     * @return parent object
     */
    @Override
    public GenericInternalIsoFile getParent() {
        return null;
    }

    /**
     * Get the absolute logical sector of the data being represented.
     *
     * @return long of the logical sector, this is usually this x 2048 to get physical location
     */
    @Override
    public long getLogicalSectorLocation() {
        return 0;
    }
}
