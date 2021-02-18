# xmlschemagenerator-maven-plugin
Maven plugin that grabs XML files from multiple webdav folders as well as local filepaths and infers a XML schema.

## Usage

To install the plugin into your local maven repository:

```
git clone https://github.com/YtGz/xmlschemagenerator-maven-plugin.git
cd xmlschemagenerator-maven-plugin
mvn install
```

To include the plugin in a maven project, add the following to your pom.xml:

```
<build>
  <plugins>
    <plugin>
        <groupId>com.actus.aif</groupId>
        <artifactId>xmlschemagenerator-maven-plugin</artifactId>
        <version>0.6.0</version>
        <executions>
            <execution>
                <id>XmlToXsd</id>
                <goals>
                    <goal>XmlToXsd</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <localXmlFilePaths>
                /path/so/some/file.xml,/path/to/another/file.xml
            </localXmlFilePaths>
            <webdavXmlFolderPaths>
                /some/relative/path/to/a/webdavfolder/,/some/relative/path/to/another/webdavfolder/
            </webdavXmlFolderPaths>
            <webdavHostname>example.org</webdavHostname>
            <webdavRoot>/remote.php/webdav/</webdavRoot>
            <webdavUsername>username</webdavUsername>
            <webdavPassword>password</webdavPassword>
            <xsdPath>src/main/xsd/schema.xsd</xsdPath>
        </configuration>
    </plugin>
  </plugins>
</build>
```
