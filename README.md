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
  * `DementFileReplicator` - extends the DefaultFileReplicator by not remebering the replicated objects.
* FileOperations - mainly used by the above two providers
  * `DarcFileOperationProvider`
    * `CollectFilesOperation` - will travers a DarcFileTree and record all underlying blob files
  * `JdbcTableOperationProvider`
    * `ExpireFilesOperation`- used to delete file rows based on last markTime column with single SQL transaction
    * `BulkSetAttributeOperation` - used to set an attribute (markTime) on a list of files

Building
--------

Can be build with Java 6 - Java 8, requires Maven 3.2.x.

By default Java 8 compatibility is used. To specify a specific version, use:

    JAVA_HOME=/opt/jdk7
    mvn -Prelease-profile -Dmaven.compiler.source=7 -Dmaven.compiler.target=7 clean install

Currently fails with JDK9.

Use the following dependency declarations:

    <dependency>
        <groupId>com.seeburger.vfs2</groupId>
        <artifactId>vfs2provider-jdbctable</artifactId>
        <version>1.6.0</version>
    </dependency>
    <dependency>
        <groupId>com.seeburger.vfs2</groupId>
        <artifactId>vfs2provider-digestarc</artifactId>
        <version>1.6.0</version>
    </dependency>

Note: the artifacts are not available via Maven Central.

Latest Release
--------------

* *1.1.0* - Includes new com.seeburger.vfs2.operations package (in vfs2provider-jdbctable.jar)
* *1.2.0* - Provide VFSClassLoader#getFileObject(String) which retrieves backing file for resource
* *1.3.0* - make tests and javadoc work for Java 8/9. Add readOnly session to vfs2provider-digestarc
* *1.3.1* - DarcFileProvider will not create multiple instances of underlying file system (removes session from option)
* *1.4.0* - Updated plugin dependencies and added DementFileReplicator.
* *1.5.0* - Uses Apache Commons VFS 2.1, some cleaned up IOException messages. JDBCTabel filesystem allows 50MB for reading blobs.
* *1.5.1* - some warning cleanup, less array copies in DarcFileObject.
* *1.5.2* - jdbctable: ignore rename race and check for outcome before throwing
* *1.5.3* - jdbctable: Better logging by differentiating missing/excessive records.<br/>
            jdbctable: Refresh before ignoring duplicate new hash<br/>
            (avoids possible corruption with bg db deletes)
* *1.6.0* - compile with Java 8, adjust travis matrix to exclude Java 6.<br/>
            Compiles against Apache Commons VFS 2.2
