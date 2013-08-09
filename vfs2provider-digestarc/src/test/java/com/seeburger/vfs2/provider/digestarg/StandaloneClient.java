package com.seeburger.vfs2.provider.digestarg;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;

import com.seeburger.vfs2.provider.digestarc.DarcFileProvider;

public class StandaloneClient
{
	/**
	 * @param args
	 * @throws FileSystemException
	 */
	public static void main(String[] args) throws FileSystemException
	{
		DefaultFileSystemManager manager = new DefaultFileSystemManager();
		manager.addProvider("seeda", new DarcFileProvider());
		manager.addProvider("file", new DefaultLocalFileProvider());
		manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
		manager.init();

		final FileObject dir1 = manager.resolveFile("seeda:file:///dir1/file");
		final FileObject root = dir1.getFileSystem().getRoot();

		System.out.println("key=" + dir1 + " " + dir1.getFileSystem() + " root=" + root) ;
		listFiles("|", root);
	}

	private static void listFiles(String prefix, FileObject file) throws FileSystemException
	{
		if (file.getType().hasChildren())
		{
			FileObject[] children = null;
			try
			{
				children = file.getChildren();
				System.out.println(prefix + file + "/");
			}
			catch (FileSystemException ignored)
			{
				System.out.println(prefix + file + "/ (" + ignored + ")");
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
			System.out.println(prefix + file + " size=" + file.getContent().getSize()+ " hash " + file.getContent().getAttribute("githash"));
		}
		else
		{
			System.out.println(prefix + "(" + file + ")" + " size=" + file.getContent().getSize());
		}
	}

}
