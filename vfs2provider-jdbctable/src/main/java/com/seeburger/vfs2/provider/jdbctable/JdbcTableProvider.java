package com.seeburger.vfs2.provider.jdbctable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

public class JdbcTableProvider
    extends AbstractOriginatingFileProvider
{
    /** Authenticator information. */
    public static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[]
        {
            UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD
        };

    static final Collection<Capability> capabilities = Collections.unmodifiableCollection(Arrays.asList(new Capability[]
    {
        Capability.GET_TYPE,
        Capability.READ_CONTENT,
        Capability.WRITE_CONTENT,
        Capability.LAST_MODIFIED,
        Capability.FS_ATTRIBUTES,
        Capability.URI,
        Capability.GET_LAST_MODIFIED,
        Capability.ATTRIBUTES,
        Capability.RANDOM_ACCESS_READ,
        Capability.LIST_CHILDREN,
        Capability.APPEND_CONTENT, // conditional
        Capability.RENAME          // conditional
    }));

	DataSource dataSource;

    /**
     * Constructs a new provider.
     */
    public JdbcTableProvider()
    {
    	this(null);
    }

    /**
     * Constructs a new provider.
     */
    public JdbcTableProvider(DataSource dataSource)
    {
        super();
        setFileNameParser(JdbcTableNameParser.getInstance());
        this.dataSource = dataSource;
    }


    /**
     * Creates a {@link FileSystem}.
     */
    @Override
    protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
        throws FileSystemException
    {
    	// name can be instance of GenericFileName or URLFileName
        //final URLFileName rootName = (UrlFileNameParser) name;
        UserAuthenticationData authData = null;
        try
        {
            authData = UserAuthenticatorUtils.authenticate(fileSystemOptions, AUTHENTICATOR_TYPES);
            System.out.println("auth=" + ((authData == null)?"(null)":(authData.toString() + authData.getData(UserAuthenticationData.USERNAME))));
        }
        finally
        {
            UserAuthenticatorUtils.cleanup(authData);
        }

        return new JdbcTableFileSystem(name, this, fileSystemOptions);
    }

    @Override
    public FileSystemConfigBuilder getConfigBuilder()
    {
        return JdbcTableFileSystemConfigBuilder.getInstance();
    }

    public Collection<Capability> getCapabilities()
    {
        return capabilities;
    }
}
