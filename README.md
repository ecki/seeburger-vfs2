seeburger-vfs2
==============

SEEBURGER Extensions to Apache Commons VFS2

* vfs2provider-digestarc - allows Git style content addressed storage of filesystem trees (based on a manifest file and a directory with hashed blobs)
* vfs2provider-jdbctable - allow storing blobs in database tables (should work with digestarc)
* vfsutil - (currently included in vfs2provider-jdbctable/com.seeburger.vfs2.util) helper classes to deal with VFS2 (`TreePrinter` and a (fixed) version of the `VFSClassLoader`)
