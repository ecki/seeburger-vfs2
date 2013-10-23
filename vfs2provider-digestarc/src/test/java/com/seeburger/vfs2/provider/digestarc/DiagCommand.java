/*
 * DiagCommand.java
 *
 * created at 2013-09-25 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.provider.digestarc;


import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs2.provider.webdav.WebdavFileProvider;


public class DiagCommand
{
    /**
	 * @param args
     * @throws IOException
     * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException
	{
	    int len = args.length;
	    int pos = 0;

	    DefaultFileSystemManager manager = new DefaultFileSystemManager();
	    manager.addProvider("seearc", new DarcFileProvider());
	    manager.addProvider("file", new DefaultLocalFileProvider());
	    manager.addProvider("webdav", new WebdavFileProvider());
	    manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
	    manager.init();

	    while(len >= 2)
	    {
	        FileObject file = manager.resolveFile(new File("."), args[pos+1]);
	        String type = args[pos];

	        System.out.println("== Analysing type=" + type + " file=" + file);

	        if("seetree".equals(type))
	        {
	            DarcFileInputStream in = new DarcFileInputStream(file, null, "seetree");
	            in.readAll();
	            in.close();

	            DarcTree tree = new DarcTree(file.getContent().getInputStream(), null);
	            System.out.println("seetree " + tree);
	        } else {
	            DarcFileInputStream in = new DarcFileInputStream(file, null, type);
	            in.readAll();
	            in.close();
	        }
	        pos+=2; len-=2;
	    }
    }
}
