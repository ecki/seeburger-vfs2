package com.seeburger.vfs2.provider.digestarc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;

import com.seeburger.vfs2.provider.digestarc.DarcFileProvider;
import com.seeburger.vfs2.provider.digestarc.DarcTree.Directory;
import com.seeburger.vfs2.provider.digestarc.DarcTree.Entry;
import com.seeburger.vfs2.util.TreePrinter;

public class StandaloneClient
{
	private static final Charset ASCII = Charset.forName("ASCII");

    /**
	 * @param args
     * @throws IOException
     * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException
	{
	    File testDir = new File("target/testdir");
	    populateTestdir(testDir);

	    DefaultFileSystemManager manager = new DefaultFileSystemManager();
		manager.addProvider("seearc", new DarcFileProvider());
		manager.addProvider("file", new DefaultLocalFileProvider());
		manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
		manager.init();

		FileObject base = manager.resolveFile(testDir, "a3/6af8df2216a8357967276438ea608fe1a2c0e1");
		final FileObject dir1 = manager.createFileSystem("seearc", base);
		final FileObject root = dir1.getFileSystem().getRoot();

		TreePrinter.printTree(root, "| ", System.out);

		FileObject testFile = dir1.resolveFile("file2");
System.out.println("opened file: " + testFile);
System.out.println("       size: " + testFile.getContent().getSize());
        InputStream is = testFile.getContent().getInputStream();
        int c;
        while((c = is.read()) != -1)
        {
            System.out.println(" " + String.valueOf((char)c));
        }
        is.close();

	}

    private static void populateTestdir(File base) throws IOException, NoSuchAlgorithmException
    {
        base.mkdirs();
        String hash1 = createBlob(base, "Hello World!");
        String hash2 = createBlob(base, "");
        createDir(base, hash1, hash2);
    }

    private static void createDir(java.io.File base, String hash12, String hash0) throws IOException
    {
        DarcTree df = new DarcTree();
        Map<String, Entry> content = new HashMap<String, Entry>();
        DarcTree.File f = df.new File(0, hash0);
        content.put("file1", f);
        f = df.new File(12, hash12);
        content.put("file2", f);
        Directory dir = df.new Directory(content);

        java.io.File temp = File.createTempFile("newtree", ".tmp", base);
        FileOutputStream out = new FileOutputStream(temp);
        String hash = dir.writeToStream(out);
        out.close();
        BlobStorageProvider provider = new BlobStorageProvider(null, null);
        String path = provider.hashToPath(hash);
        File target = new File(base, path);
        File parent = target.getParentFile();
        parent.mkdir();
        temp.renameTo(target);
        System.out.println("Dir hash " + target);
    }

    private static String createBlob(File base, String string) throws IOException, NoSuchAlgorithmException
    {
        File temp = File.createTempFile("newblob", ".tmp", base);
        DarcTree df = new DarcTree();
        DarcTree.File f = df.new File(0, "");
        byte[] b = string.getBytes(ASCII);
        ByteArrayInputStream source = new ByteArrayInputStream(b);
        OutputStream fos = new FileOutputStream(temp);
        byte[] hash = f.writeBlob(fos, b.length, source);
        fos.close();
        String hashString = DarcFileInputStream.asHex(hash);
        BlobStorageProvider provider = new BlobStorageProvider(null, null);
        String path = provider.hashToPath(hashString);
        File target = new File(base, path);
        File parent = target.getParentFile();
        parent.mkdir();
        temp.renameTo(target);
        return hashString;
    }
}
