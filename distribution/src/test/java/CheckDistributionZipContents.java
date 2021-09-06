/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CheckDistributionZipContents
{
    public static final String PREFIX = "alfresco-content-services-distribution";
    public static final String FORMAT = ".zip";

    @Test
    public void testDistributionZipContents() throws Exception
    {
        File filesList[] = getDistributionZip();
        for (File file : filesList)
        {
            List<String> zipEntries = getZipEntries(file.getAbsolutePath());
            assertThat(zipEntries).
                    contains(
                            "keystore/metadata-keystore/keystore-passwords.properties",
                            "keystore/metadata-keystore/keystore",
                            "keystore/generate_keystores.bat",
                            "keystore/generate_keystores.sh",
                            "bin/alfresco-mmt.jar",
                            "bin/apply_amps.bat",
                            "bin/apply_amps.sh",
                            "web-server/webapps/ROOT.war",
                            "web-server/webapps/alfresco.war",
                            "web-server/webapps/share.war",
                            "web-server/webapps/_vti_bin.war",
                            "web-server/conf/Catalina/localhost/share.xml",
                            "web-server/conf/Catalina/localhost/alfresco.xml",
                            "amps/alfresco-share-services.amp"
                    );
        }
    }

    private File[] getDistributionZip() throws Exception
    {
        Path targetPath = Paths.get(getClass().getResource("/").toURI()).getParent();
        File distributionZip = new File(targetPath.toString());
        FilenameFilter zipFileFilter = (dir, name) -> {
            if (name.startsWith(PREFIX) && name.endsWith(FORMAT))
            {
                return true;
            }
            else
            {
                return false;
            }
        };

        return distributionZip.listFiles(zipFileFilter);
    }

    private List<String> getZipEntries(String filePath) throws Exception
    {
        List<String> zipEntries = new ArrayList<>();
        ZipFile zipFile = new ZipFile(new File(filePath));
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements())
        {
            ZipEntry entry = entries.nextElement();
            zipEntries.add(entry.toString());
        }
        return zipEntries;
    }

}
