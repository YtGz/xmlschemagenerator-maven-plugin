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

import com.thaiopensource.relaxng.translate.Driver;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.httpclient.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maven plugin that grabs XML files from multiple webdav folders and infers a XML schema.
 * Can be used in conjunction with jaxb2-maven-plugin to generate java classes suited for JAXB.
 * <p>
 * This maven plugin uses the org.relaxng.trang dependency as the underlying XML to XSD converter.
 */
@Mojo(name = "XmlToXsd", defaultPhase = LifecyclePhase.INITIALIZE)
public class XmlToXsdMojo extends AbstractMojo {
    // TODO: add default mail parameters

    /**
     * the webdav paths of the folders containing the xml files that the schema shall be generated from
     */
    @Parameter(required = true)
    private List<String> webdavXmlFolderPaths;

    /**
     * the device-local paths of the folders containing the xml files that the schema shall be generated from
     */
    @Parameter(required = true)
    private List<String> localXmlFilePaths;

    /**
     * the webdav hostname (domain)
     */
    @Parameter(required = true)
    private String webdavHostname;

    /**
     * the root path relative to the hostname (the string must include a leading slash)
     */
    @Parameter(required = true)
    private String webdavRoot;

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
     *
     * @throws MojoExecutionException - if an unexpected problem occurs. Throwing this exception causes a "BUILD
     *                                ERROR" message to be displayed.
     * @throws MojoFailureException   - if an expected problem (such as a compilation failure) occurs. Throwing this
     *                                exception causes a "BUILD FAILURE" message to be displayed.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> filePaths = downloadFiles();
        filePaths.addAll(localXmlFilePaths);
        generateXsdFromXmlFiles(filePaths);
    }

    private void generateXsdFromXmlFiles(List<String> filePaths) {
        filePaths.add(xsdPath);
        new Driver().run(filePaths.toArray(String[]::new));
    }

    private List<String> downloadFiles() throws MojoExecutionException {
        final Host host = new Host(webdavHostname, webdavRoot, 443, webdavUsername, webdavPassword, null, null);
        host.setSecure(true);
        host.setUsePreemptiveAuth(true);
        final List<io.milton.httpclient.Resource> xmlFiles = new ArrayList<>();
        for (String xmlFolderPath : webdavXmlFolderPaths) {
            try {
                Folder xmlFolder = host.getFolder(xmlFolderPath);
                xmlFiles.addAll(xmlFolder
                                        .children()
                                        .stream()
                                        .filter(resource ->
                                                        !(resource.getClass() == io.milton.httpclient.Folder.class ||
                                                          resource.name.startsWith(".")) &&
                                                        ((((io.milton.httpclient.File) resource).contentType != null &&
                                                          ((io.milton.httpclient.File) resource).contentType.equalsIgnoreCase(
                                                                  "application/xml")) ||
                                                         resource.name.endsWith(".xml")))
                                        .collect(Collectors.toUnmodifiableList()));
            }
            catch (IOException | HttpException | NotAuthorizedException | BadRequestException e) {
                e.printStackTrace();
                throw new MojoExecutionException("Failed to execute plugin", e);
            }
        }
        return createTemporaryLocalFiles(host, xmlFiles);
    }

    private List<String> createTemporaryLocalFiles(Host host, List<io.milton.httpclient.Resource> webdavResources) {
        List<String> filePaths = new ArrayList<>();
        webdavResources.forEach(webdavResource -> {
            try {
                File tempFile = Files.createTempFile(webdavResource.name, ".xml").toFile();
                tempFile.deleteOnExit();
                ((io.milton.httpclient.File) webdavResource).downloadToFile(tempFile, new ProgressListener() {
                    @Override
                    public void onRead(final int i) {
                    }

                    @Override
                    public void onProgress(final long l, final Long aLong, final String s) {
                        System.out.println("downloading a temporary copy of file : " + s);
                    }

                    @Override
                    public void onComplete(final String s) {
                        System.out.println("downloaded " + s);
                    }

                    @Override
                    public boolean isCancelled() {
                        return false;
                    }
                });
                filePaths.add(tempFile.getAbsolutePath());
            }
            catch (IOException | HttpException ignored) {
                // TODO
            }
        });
        return filePaths;
    }

    //TODO add error mail
}
