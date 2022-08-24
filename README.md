seeburger-vfs2
==============

SEEBURGER Extensions to Apache Commons VFS2

* vfs2provider-digestarc - allows Git style content addressed storage of filesystem trees (with hashed blobs) [![Known Vulnerabilities](https://snyk.io/test/github/ecki/seeburger-vfs2/badge.svg?targetFile=vfs2provider-digestarc/pom.xml)](https://snyk.io/test/github/ecki/seeburger-vfs2?targetFile=vfs2provider-digestarc/pom.xml)
* vfs2provider-jdbctable - allow storing blobs in database tables [![Known Vulnerabilities](https://snyk.io/test/github/ecki/seeburger-vfs2/badge.svg?targetFile=vfs2provider-jdbctable/pom.xml)](https://snyk.io/test/github/ecki/seeburger-vfs2?targetFile=vfs2provider-jdbctable/pom.xml)
* (vfs2-util - currently included in vfs2provider-jdbctable/com.seeburger.vfs2.util) helper classes to deal with VFS2
  * `TreePrinter` for printing a VFS Directory
  * enhanced clone of the `VFSClassLoader`
  * `FileNameGlobbing` - support for file name filtering
  * `VFSUtils` - static utility functions dealing with VFS2 Objects
  * `DementFileReplicator` - extends the DefaultFileReplicator by not remebering the replicated objects.
* FileOperations - mainly used by the above two providers
  * `DarcFileOperationProvider`
    * `CollectFilesOperation` - will travers a DarcFileTree and record all underlying blob files
  * `JdbcTableOperationProvider`
    * `ExpireFilesOperation`- used to delete file rows based on last markTime column with single SQL transaction
    * `BulkSetAttributeOperation` - used to set an attribute (markTime) on a list of files

Building
--------

Can be build with Java 7 - Java 11, requires Maven 3.2.x.

By default Java 8 compatibility is used. To specify a specific version, use:

    JAVA_HOME=/opt/jdk11
    mvn -B -e -C -V -Prelease-profile -Dmaven.compiler.source=11 -Dmaven.compiler.target=11 clean install

Use the following dependency declarations (compile scope should only needed for com.seeburger.vfs2.util):

    <dependency>
        <groupId>com.seeburger.vfs2</groupId>
        <artifactId>vfs2provider-jdbctable</artifactId>
        <version>1.7.6</version>
    </dependency>
    <dependency>
        <groupId>com.seeburger.vfs2</groupId>
        <artifactId>vfs2provider-digestarc</artifactId>
        <version>1.7.6</version>
    </dependency>

Note: the artifacts are not available via Maven Central.

License
--------
This project is released by SEEBURGER AG, Germany under the Apache Software License 2.0 (ASL).

