package com.seeburger.vfs2.provider.jdbctable.test;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;

import com.seeburger.vfs2.provider.jdbctable.JdbcTableProvider;

public class StandaloneClient {

	/**
	 * @param args
	 * @throws FileSystemException
	 */
	public static void main(String[] args) throws FileSystemException
	{
		DefaultFileSystemManager manager = new DefaultFileSystemManager();
		manager.addProvider("seejt", new JdbcTableProvider());
		manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
		manager.init();

		final FileObject key = manager.resolveFile("seejt:/key");
		final FileObject root = key.getFileSystem().getRoot();

		System.out.println("key=" + key + " " + key.getFileSystem() + " root=" + root) ;
		listFiles("|", root);

		FileObject a1 = manager.resolveFile("seejt:///key/a1");
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
		a1 = manager.resolveFile("seejt:///key/a1");
		listFiles("a1=", a1);
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
			System.out.println(prefix + file);
		}
		else
		{
			System.out.println(prefix + "(" + file + ")");
		}
	}

}
