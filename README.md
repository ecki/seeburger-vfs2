seeburger-vfs2
==============

SEEBURGER Extensions to Apache Commons VFS2

* vfs2provider-digestarc - allows Git style content addressed storage of filesystem trees (with hashed blobs)
* vfs2provider-jdbctable - allow storing blobs in database tables
* (vfs2-util - currently included in vfs2provider-jdbctable/com.seeburger.vfs2.util) helper classes to deal with VFS2
  * `TreePrinter` for printing a VFS Directory
  * enhanced clone of the `VFSClassLoader`
  * `FileNameGlobbing` - support for file name filtering
  * `VFSUtils` - static utility functions dealing with VFS2 Objects
* FileOperations - mainly used by the above two providers
  * `DarcFileOperationProvider`
    * `CollectFilesOperation` - will travers a DarcFileTree and record all underlying blob files
  * `JdbcTableOperationProvider`
    * `ExpireFilesOperation`- used to delete file rows based on last markTime column with single SQL transaction
    * `BulkSetAttributeOperation` - used to set an attribute (markTime) on a list of files

Building
--------

Can be build with Java 6 - Java 9, requires Maven 3.2.x.

By default Java 6 compatibility is used (which should only be used when Maven is started with Java 6).
To specify a specific version, use:

    JAVA_HOME=/opt/jdk9
    mvn -Prelease-profile -Dmaven.compiler.source=9 -Dmaven.compiler.target=9 clean install

Use the following dependency declarations:

    <dependency>
        <groupId>com.seeburger.vfs2</groupId>
        <artifactId>vfs2provider-jdbctable</artifactId>
        <version>1.3.0</version>
    </dependency>
    <dependency>
        <groupId>com.seeburger.vfs2</groupId>
        <artifactId>vfs2provider-digestarc</artifactId>
        <version>1.3.0</version>
    </dependency>

Note: the artifacts are not available via Maven Central.

Latest Release
--------------

* *1.1.0* - Includes new com.seeburger.vfs2.operations package (in vfs2provider-jdbctable.jar)
* *1.2.0* - Provide VFSClassLoader#getFileObject(String) which retrieves backing file for resource
* *1.3.0* - make tests and javadoc work for Java 8/9. Add readOnly session to vfs2provider-digestarc
