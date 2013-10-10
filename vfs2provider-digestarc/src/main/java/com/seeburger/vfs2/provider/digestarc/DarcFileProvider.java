package com.seeburger.vfs2.provider.digestarc;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractLayeredFileProvider;
import org.apache.commons.vfs2.provider.FileProvider;
import org.apache.commons.vfs2.provider.LayeredFileName;


/**
 * A file system provider for Digest Archives.
 */
public class DarcFileProvider extends AbstractLayeredFileProvider implements FileProvider
{
    /** The list of capabilities this provider supports */
    protected static final Collection<Capability> capabilities =
        Collections.unmodifiableCollection(Arrays.asList(new Capability[]
        {
            Capability.GET_LAST_MODIFIED,
            Capability.GET_TYPE,
            Capability.LIST_CHILDREN,
            Capability.READ_CONTENT,
            Capability.URI,
            Capability.COMPRESS,
            Capability.VIRTUAL,

            Capability.APPEND_CONTENT,
            Capability.CREATE,
            Capability.DELETE,
            Capability.RENAME,
            Capability.WRITE_CONTENT
        }));

    /** The list of capabilities this provider supports */
    protected static final Collection<Capability> writeCapabilities =
        Collections.unmodifiableCollection(Arrays.asList(new Capability[]
        {
             Capability.APPEND_CONTENT,
             Capability.CREATE,
             Capability.DELETE,
             Capability.RENAME,
             Capability.WRITE_CONTENT,
        }));

    public DarcFileProvider()
    {
        super();
    }

    /**
     * Creates a layered file system.  This method is called if the file system
     * is not cached.
     * <P>
     * Logger, Context (and manager) are set afterwards.
     *
     * @param scheme The URI scheme.
     * @param file   The file to create the file system on top of.
     * @return The file system.
     */
    @Override
    protected FileSystem doCreateFileSystem(final String scheme,
                                            final FileObject file,
                                            final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        final LayeredFileName rootName =
            new LayeredFileName(scheme, file.getName(), FileName.ROOT_PATH, FileType.FOLDER);
        return new DarcFileSystem(rootName, file, fileSystemOptions);
    }

    @Override
    public FileSystemConfigBuilder getConfigBuilder()
    {
        return DarcFileConfigBuilder.getInstance();
    }

    public Collection<Capability> getCapabilities()
    {
        return capabilities;
    }

    @Override
    public synchronized FileObject createFileSystem(final String scheme,
                                                    final FileObject file,
                                                    final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
        // Check if cached
        final FileName rootName = file.getName();
        // super implementation does not pass options and therefore does not cache?
        FileSystem fs = findFileSystem(rootName, fileSystemOptions);
        if (fs == null)
        {
            // Create the file system
            fs = doCreateFileSystem(scheme, file, fileSystemOptions);
            addFileSystem(rootName, fs);
        }
        return fs.getRoot();
    }

}
