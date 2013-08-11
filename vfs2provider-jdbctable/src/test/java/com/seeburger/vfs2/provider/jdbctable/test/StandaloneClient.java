package com.seeburger.vfs2.provider.jdbctable.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.derby.jdbc.EmbeddedDataSource40;

import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.api.MigrationInfo;
import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;

public class StandaloneClient {

    /**
     * @param args
     * @throws SQLException
     * @throws IOException
     */
    public static void main(String[] args) throws SQLException, IOException
    {
        DataSource ds = createDatasource();
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        manager.addProvider("seejt", new JdbcTableProvider(ds));
        manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
        manager.init();

        final FileObject key = manager.resolveFile("seejt:/key");
        final FileObject root = key.getFileSystem().getRoot();

        System.out.println("key=" + key + " " + key.getFileSystem() + " root=" + root) ;
        listFiles("|", root);

        FileObject a1 = manager.resolveFile("seejt:///key/abc");
        listFiles("a1=", a1);
        FileObject b2 = manager.resolveFile("seejt:///key/b2");
        listFiles("b2=", b2);
        b2 = manager.resolveFile("seejt:///key/b2");
        listFiles("b2=", b2);
        System.out.println("-- a1 retry resolve");
        a1 = manager.resolveFile("seejt:///key/a1");
        listFiles("a1=", a1);
        System.out.println("-- a1 retry children");
        a1 = key.getChild("a1");
        listFiles("a1=", a1);
        System.out.println("-- a1 refresh");
        a1.refresh();
        listFiles("axxx=", a1);

        FileObject ax = null;
        long now = System.currentTimeMillis();
        int count = 1000;

        System.out.println("Reading ("+count+") ...");
        for(int i=0; i<count; i++)
        {
            ax = manager.resolveFile("seejt:///key/abcd_" + now + "_" +  i);
            //ax.createFile();
            OutputStream os = ax.getContent().getOutputStream();
            os.write(1); os.write(2); os.write(3); os.close();
            ax = manager.resolveFile(ax.toString());
            if (i % 50 == 0)
                listFiles("("+i+")  ", ax);
        }

        System.out.println("Reading ("+count+") ...");
        for(int i=0; i<count; i++)
        {
            ax = manager.resolveFile("seejt:///key/abcd_" + now + "_"+  i);
            InputStream is = ax.getContent().getInputStream();
            if (is.read() != 1 || is.read() != 2 || is.read() != 3)
                System.out.println("not 1 2 3");
            is.close();
            if (i % 100 == 0)
                listFiles("("+i+")  ", ax);
        }
        System.out.println("Have a good time.");
    }

    private static DataSource createDatasource() throws SQLException
    {
        EmbeddedDataSource40 ds = new EmbeddedDataSource40();
        ds.setUser("SEEASOWN");
        ds.setPassword("secret");
        ds.setCreateDatabase("create");
        ds.setDatabaseName("target/SimpleDerbyTestDB");

        Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.setValidateOnMigrate(true);
        flyway.setCleanOnValidationError(true);
        flyway.migrate();

        MigrationInfo[] states = flyway.info().all();
        for(MigrationInfo state : states)
            System.out.println("- " + state.getVersion() + "__" + state.getDescription() + " " + state.getInstalledOn() + " " + state.getChecksum() + " " + state.getState());

        return ds;
    }

    private static void listFiles(String prefix, FileObject file) throws FileSystemException
    {
        String type = " (";
        if (file.isHidden())
            type+="H";
        else
            type+=".";
        if (file.isReadable())
            type+="R";
        else
            type+=".";
        if (file.isWriteable())
            type+="W";
        else
            type+=".";
        type+=")";
        FileContent content = file.getContent();
        if (content != null)
        {
            try { type += " date=" + new Date(content.getLastModifiedTime()); }catch (Exception ig) { }
            try { type += " size=" + content.getSize();  }catch (Exception ig) { }
            try { type += " att=" + content.getAttributes();  }catch (Exception ig) { }
        }


        if (file.getType().hasChildren())
        {
            FileObject[] children = null;
            try
            {
                children = file.getChildren();
                System.out.println(prefix + file + " /" + type);
            }
            catch (FileSystemException ignored)
            {
                System.out.println(prefix + file + "/ (" + ignored + ")" + type);
            }
            if (children != null)
            {
                for(FileObject fo : children)
                {
                    listFiles(prefix + "  ", fo);
                }
            }
        }
        else if (file.getType() == FileType.FILE)
        {
            System.out.println(prefix + file + type);
        }
        else
        {
            System.out.println(prefix + "(" + file + ")" + type);
        }
    }

}
