seeburger-vfs2
==============

SEEBURGER Extensions to Apache Commons VFS2

* vfs2provider-digestarc - allows Git style content addressed storage of filesystem trees (based on a manifest file and a directory with hashed blobs)
* vfs2provider-jdbctable - allow storing blobs in database tables
* (vfs2-util - currently included in vfs2provider-jdbctable/com.seeburger.vfs2.util) helper classes to deal with VFS2
  * `TreePrinter` for printing a VFS Directory
  * emhanced clone of the `VFSClassLoader`
  * `FileNameGlobbing` - support for file name filtering
  * `VFSUtils` - static utility functions dealing with VFS2 Objects
* FileOperations - mainly used by the above two providers
  * `DarcFileOperationProvider`
    * `CollectFilesOperation` - will travers a DarcFileTree and record all underlying blob files
  * `JdbcTableOperationProvider`
    * `ExpireFilesOperation`- used to delete file rows based on last markTime column with single SQL transaction
    * `BulkSetAttributeOperation` - used to set an attribute (markTime) on a list of files

Use `mvn -Prelease-profile clean install` to build. You need Apache Maven 3.2.x for this.

Latest Release
--------------
*1.1.0* - Includes new com.seeburger.vfs2.operations package (in vfs2provider-jdbctable.jar)
*1.2.0* - Provide VFSClassLoader#getFileObject(String) which retrieves backing file for resource
