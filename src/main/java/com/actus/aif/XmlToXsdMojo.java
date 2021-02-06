package com.actus.aif;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.thaiopensource.relaxng.translate.Driver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maven plugin that grabs XML files from multiple webdav folders and infers a XML schema.
 * Can be used in conjunction with jaxb2-maven-plugin to generate java classes suited for JAXB.
 *
 * This maven plugin uses the org.relaxng.trang dependency as the underlying XML to XSD converter.
 */
@Mojo(name="XmlToXsd", defaultPhase = LifecyclePhase.INITIALIZE)
public class XmlToXsdMojo
    extends AbstractMojo
{
    // TODO: add default mail parameters

    /**
     * the webdav paths of the folders containing the xml files that the schema shall be generated from
     */
    @Parameter(required = true)
    private List<String> xmlFolderPaths;

    /**
     * the webdav hostname (domain)
     */
    @Parameter(required = true)
    private String webdavHostname;

    /**
     * the webdav user
     */
    @Parameter(required = true)
    private String webdavUsername;

    /**
     * the webdav password
     */
    @Parameter(required = true)
    private String webdavPassword;

    /**
     * folder where the generated xml schema will be stored
     */
    @Parameter(required = true)
    private String xsdPath;

    /**
     * This is the overridden method that converts the XML
     * document to an equivalent JSON document
     * @throws MojoExecutionException - if an unexpected problem occurs. Throwing this exception causes a "BUILD ERROR" message to be displayed.
     * @throws MojoFailureException - if an expected problem (such as a compilation failure) occurs. Throwing this exception causes a "BUILD FAILURE" message to be displayed.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> filePaths = downloadFiles();
        generateXsdFromXmlFiles(filePaths);
    }

    private void generateXsdFromXmlFiles(List<String> filePaths) {
        filePaths.add(xsdPath);
        new Driver().run((String[]) filePaths.toArray());
    }

    private List<String> downloadFiles() throws MojoExecutionException {
        final Sardine sardine = SardineFactory.begin();
        sardine.setCredentials(webdavUsername, webdavPassword);
        sardine.enablePreemptiveAuthentication(webdavHostname,80, 443);

        final List<DavResource> xmlFiles = new ArrayList<>();
        for(String xmlFolderPath : xmlFolderPaths) {
            System.out.println(xmlFolderPath);
            try {
                System.out.println(sardine.exists(xmlFolderPath));
                sardine.list(xmlFolderPath);
                System.out.println("hey!");
                xmlFiles.addAll(sardine.list(xmlFolderPath).stream().filter(resource -> !(resource.isDirectory() || resource.getName().startsWith(".")) && resource.getContentType().equalsIgnoreCase("application/xml")).collect(Collectors.toUnmodifiableList()));
            } catch (IOException e) {
                e.printStackTrace();
                throw new MojoExecutionException("Failed to execute plugin", e);
            }
        }
        return createTemporaryLocalFiles(sardine, xmlFiles);
    }

    private List<String> createTemporaryLocalFiles(Sardine sardine, List<DavResource> webdavResources) {
        List<String> filePaths = new ArrayList<>();
        webdavResources.forEach(webdavResource -> {
            try {
                InputStream inputStream = sardine.get(webdavResource.getPath()); //.toFile;
                File tempFile = Files.createTempFile(webdavResource.getName(), ".xml").toFile();
                tempFile.deleteOnExit();
                Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                filePaths.add(tempFile.getAbsolutePath());
            } catch (IOException ignored) {
                // TODO
            }
        });
        return filePaths;
    }

    //TODO add error mail
}
