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

package com.palantir.isofilereader;

import com.palantir.isofilereader.isofilereader.IsoFileReader;
import com.palantir.isofilereader.isofilereader.iso.IsoFormatInternalDataFile;
import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatDirectoryRecord;
import java.io.File;
import java.security.MessageDigest;
import java.util.Locale;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OneOffTests {

    @Test
    @SuppressWarnings("StrictUnusedVariable")
    void getBestFileType() {
        File rootFolder = new File("./");
        System.out.println("Operating from: " + rootFolder.getAbsolutePath());
        File isoFile = new File("./test_isos/rocky.iso");

        boolean findTheLongOne = false;
        try (IsoFileReader iso = new IsoFileReader(isoFile)) {
            System.out.println("Best ToC:  " + iso.getTraditionalIsoReader().getTableOfContentsInUse());
            System.out.println("RockRidge: " + iso.getTraditionalIsoReader().isUseRockRidgeOverStandard());

            for (IsoFormatDirectoryRecord singleFile : iso.getAllFileRecordsInIsoRaw()) {
                if (!singleFile.isDirectory()) {
                    IsoFormatInternalDataFile isoInternalDataFile = new IsoFormatInternalDataFile(
                            singleFile, iso.getTraditionalIsoReader().isUseRockRidgeOverStandard());
                    // Historically the previous library and zip handling dropped the / for root files, we will do that
                    String filename = isoInternalDataFile.getFullFileName(File.separatorChar);
                    System.out.println("Filename is: " + filename);
                    if (filename.equalsIgnoreCase("/BaseOS/repodata/878c2f1defc6fd1f9553a7fe0230eb31b65d13c"
                            + "e6045bb841aec881f4035e1b9-filelists.sqlite.bz2")) {
                        // c453cb827bb90bf482db48f60ce69e77
                        MessageDigest md = MessageDigest.getInstance("MD5");

                        md.update(iso.getFileBytes(isoInternalDataFile));
                        byte[] digest = md.digest();
                        String myHash = DatatypeConverter.printHexBinary(digest).toLowerCase(Locale.ROOT);
                        findTheLongOne = true;
                        Assertions.assertEquals("1f03a06b61106da5abf0d961c85c9186", myHash);
                    }
                }
            }
        } catch (Exception e) {
            Assertions.fail("Could not get header");
        }
        Assertions.assertTrue(findTheLongOne);
    }
}
