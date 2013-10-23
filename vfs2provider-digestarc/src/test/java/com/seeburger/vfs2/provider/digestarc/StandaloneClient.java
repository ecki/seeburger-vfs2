/*
 * StandaloneClient.java
 *
 * created at 2013-09-16 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


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
import org.apache.commons.vfs2.FileNotFolderException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;

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

        FileSystemOptions opts = new FileSystemOptions();
        DarcFileConfigBuilder config = DarcFileConfigBuilder.getInstance();
        config.setChangeSession(opts, "2");

        FileObject base = manager.resolveFile(testDir, "");
        String uri = "seearc:" + base.getName().getURI();
        FileObject dir1 = manager.resolveFile(uri, opts);
        dir1.createFolder();
        DarcFileSystem fs = (DarcFileSystem)dir1.getFileSystem();
        System.out.println("Commiting empty: " + fs.commitChanges());


        base = manager.resolveFile(testDir, "a3/6af8df2216a8357967276438ea608fe1a2c0e1");
        uri = "seearc:" + base.getName().getURI() + "!/";
        dir1 = manager.resolveFile(uri, opts);
        fs = (DarcFileSystem)dir1.getFileSystem();
        final FileObject root = fs.getRoot();

        TreePrinter.printTree(root, "| ", System.out);

        FileObject testFile = dir1.resolveFile("file2");

        System.out.println("opened file: " + testFile);
        System.out.println("       size: " + testFile.getContent().getSize());

        InputStream is = testFile.getContent().getInputStream();
        int c;
        while((c = is.read()) != -1)
        {
            System.out.print(" " + String.valueOf((char)c));
        }
        System.out.println();
        is.close();

        System.out.println("now creating new files and folders");

        dir1.resolveFile("newtestfile1").createFile();
        dir1.resolveFile("folder").createFolder();
        dir1.resolveFile("folder1/folder2/folder3/").createFolder();
        System.out.println(" children 3 levels non exist: " + dir1.resolveFile("folder1/folder2/folder3/").getChildren());
        dir1.resolveFile("folder/newtestfile2").createFile();
        dir1.resolveFile("file1").delete();

        TreePrinter.printTree(root, "+| ", System.out);

        System.out.println("Listing the fs outside session");
        opts = new FileSystemOptions();
        config.setChangeSession(opts, "3");

        final FileObject dir3 = manager.resolveFile(uri, opts);
        final FileObject root3 = dir3.getFileSystem().getRoot();

        TreePrinter.printTree(root3, "<< ", System.out);

        try
        {
            dir3.resolveFile("folder1/folder2/folder3/").getChildren();
        } catch (FileNotFolderException expected) { System.out.println("not folder (expected): " + expected); }

        System.out.println("Listing the fs inside session");
        opts = new FileSystemOptions();
        config.setChangeSession(opts, "2");

        final FileObject dir5 = manager.resolveFile(uri, opts);
        final FileObject root5 = dir5.getFileSystem().getRoot();

        TreePrinter.printTree(root5, ">> ", System.out);


        fs = (DarcFileSystem)dir1.getFileSystem();
        String newHash = fs.commitChanges();
        System.out.println("commited changes: " + newHash);

        base = manager.resolveFile(testDir, BlobStorageProvider.hashToPath(newHash));
        FileObject dir2 = manager.createFileSystem("seearc", base);
        TreePrinter.printTree(dir2, "== ", System.out);

        FileObject dir4 = dir2.getChild("folder");
        dir4.getChildren();



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
        String path = BlobStorageProvider.hashToPath(hash);
        File target = new File(base, path);
        if (target.exists())
        {
            temp.delete();
        }

        File parent = target.getParentFile();
        parent.mkdir();
        temp.renameTo(target);
    }

    private static String createBlob(File base, String string) throws IOException, NoSuchAlgorithmException
    {
        File temp = File.createTempFile("newblob", ".tmp", base);
        byte[] b = string.getBytes(ASCII);
        OutputStream fos = new FileOutputStream(temp);
        ObjectStorage store = new ObjectStorage();
        byte[] hash = store.writeBytes(fos, b, "blob");
        fos.close();
        String hashString = DarcTree.asHex(hash);

        String path = BlobStorageProvider.hashToPath(hashString);
        File target = new File(base, path);

        if (target.exists())
        {
            temp.delete();
            return hashString;
        }

        File parent = target.getParentFile();
        parent.mkdir();
        temp.renameTo(target);
        return hashString;
    }
}
