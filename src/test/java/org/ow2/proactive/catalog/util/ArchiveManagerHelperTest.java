/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.catalog.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.ow2.proactive.catalog.repository.entity.CatalogObjectEntity;
import org.ow2.proactive.catalog.repository.entity.CatalogObjectEntity.CatalogObjectEntityKey;
import org.ow2.proactive.catalog.repository.entity.CatalogObjectRevisionEntity;
import org.ow2.proactive.catalog.util.ArchiveManagerHelper.FileNameAndContent;
import org.ow2.proactive.catalog.util.ArchiveManagerHelper.ZipArchiveContent;


public class ArchiveManagerHelperTest {

    private static URI XML_FILE_0;

    private static URI XML_FILE_1;

    private static URI ZIP_FILE;

    @InjectMocks
    private ArchiveManagerHelper archiveManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void setFilesUrl() throws Exception {
        XML_FILE_0 = ArchiveManagerHelperTest.class.getResource("/archives/workflow_0.xml").toURI();
        XML_FILE_1 = ArchiveManagerHelperTest.class.getResource("/archives/workflow_1.xml").toURI();
        ZIP_FILE = ArchiveManagerHelperTest.class.getResource("/archives/archive.zip").toURI();
    }

    private CatalogObjectRevisionEntity getCatalogObjectRevisionEntity(String name, byte[] fileContent)
            throws IOException {
        CatalogObjectEntity object = new CatalogObjectEntity();
        object.setId(new CatalogObjectEntityKey(1L, name));

        CatalogObjectRevisionEntity revision = new CatalogObjectRevisionEntity();
        revision.setCatalogObject(object);
        revision.setRawObject(fileContent);

        return revision;
    }

    @Test
    public void testCompressZip() throws IOException {

        assertNull(archiveManager.compressZIP(null));

        byte[] workflowByteArray0 = convertFromURIToByteArray(XML_FILE_0);
        byte[] workflowByteArray1 = convertFromURIToByteArray(XML_FILE_1);
        List<CatalogObjectRevisionEntity> expectedFiles = new ArrayList<>();
        expectedFiles.add(getCatalogObjectRevisionEntity("workflow_0", workflowByteArray0));
        expectedFiles.add(getCatalogObjectRevisionEntity("workflow_1", workflowByteArray1));
        //Compress
        ZipArchiveContent archive = archiveManager.compressZIP(expectedFiles);
        //Then extract
        List<FileNameAndContent> actualFiles = archiveManager.extractZIP(archive.getContent());
        assertEquals(2, actualFiles.size());

        compare(workflowByteArray0, actualFiles.get(0).getContent());
        compare(workflowByteArray1, actualFiles.get(1).getContent());
    }

    @Test
    public void testExtractZip() throws IOException {
        assertTrue(archiveManager.extractZIP(null).isEmpty());
        List<FileNameAndContent> files = archiveManager.extractZIP(convertFromURIToByteArray(ZIP_FILE));
        assertEquals(2, files.size());

        compare(convertFromURIToByteArray(XML_FILE_0), files.get(0).getContent());
        compare(convertFromURIToByteArray(XML_FILE_1), files.get(1).getContent());
    }

    /**
     * Compares 2 files as byte arrays
     * @param expectedFile first file to compare
     * @param actualFile second file to compare
     * @throws IOException 
     */
    private void compare(byte[] expectedFile, byte[] actualFile) throws IOException {
        BufferedReader actualReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(actualFile)));
        BufferedReader expectedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(expectedFile)));

        String expectedLine = expectedReader.readLine();
        String actualLine = actualReader.readLine();

        while (expectedLine != null) {
            if (actualLine == null) {
                fail();
            }

            assertEquals(expectedLine, actualLine);

            expectedLine = expectedReader.readLine();
            actualLine = actualReader.readLine();
        }

        if (expectedLine == null && actualLine != null) {
            fail();
        }
    }

    /**
     * Get a byte array of a given file using te file's URI
     * @param uri the URI of the file
     * @return the file as byte array
     * @throws IOException when IO error occurs
     */
    private byte[] convertFromURIToByteArray(URI uri) throws IOException {
        return Files.readAllBytes(Paths.get(uri));
    }
}
