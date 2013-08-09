package com.seeburger.vfs2.provider.digestarc;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

public class DarcFile
{
	Entry[] entries;

	public DarcFile()
	{
		long now = System.currentTimeMillis();
		entries = new Entry[6];
		entries[0] = new Directory("", 1, 3+1);
		entries[1] = new Directory("dir1", 4, 4+1);
		entries[2] = new Directory("dir2", 0, 0);
		entries[3] = new File("file", 10, now, "0123456789ABCDEF01234");
		entries[4] = new Directory("dir1a", 5, 5 +1);
		entries[5] = new File("file",  10, now, "0123456789ABCDEF01234");
	}

	public DarcFile(Entry[] entries)
	{
		this.entries = entries;
	}


	int findBasename(Directory d, String namePart)
	{
//System.out.println("dir " + d.name + " np " + namePart);
		Entry needle = new File(namePart,0,0,null);
		Comparator<Entry> comp = new Comparator<Entry>() {
			public int compare(Entry o1, Entry o2)
			{
				return o1.name.compareTo(o2.name);
			}
		};
		int p = Arrays.binarySearch(entries, d.firstPos, d.lastPos, needle, comp);
//System.out.println("p=" + p);
		return p;
	}

	void writeToStream(OutputStream out) throws IOException
	{
		DataOutputStream dos = (out instanceof DataOutputStream)?(DataOutputStream)out:new DataOutputStream(out);

		// write header
		dos.writeShort(0xbeda);     // magic
		dos.writeShort(0x0001);     // version
		dos.writeInt(entries.length); // number of entries

		for(int i=0;i<entries.length;i++)
		{
			Entry e = entries[i];
			e.writeToStream(dos);
		}
		// TODO: hash
		dos.flush();
	}

	void readFromStream(InputStream in) throws IOException
	{
		DataInputStream dis = (in instanceof DataInputStream)?(DataInputStream)in:new DataInputStream(in);

		// read header
		short magic = dis.readShort();
		if (magic != 0xbeda)
			throw new IOException("Stream has wrong magic number " + magic);
		short ver = dis.readShort();
		if (ver != 0x0001)
			throw new IOException("Stream has unsupported version " + ver);

		int count = dis.readInt();
		Entry[] entries = new Entry[count];

		// read entry array
		for(int i=0;i<count;i++)
		{
			int type = dis.read();
			switch(type)
			{
			case -1:
				throw new IOException("premature end of stream");
			case (int)'D':
				entries[i] = new Directory(dis);
				break;
			case (int)'F':
				entries[i] = new File(dis);
				break;
			default:
				throw new IOException("Unknown type " + type + " found");
			}
		}
	}

	public Entry resolveName(String name)
	{
		String[] parts = name.split("/"); // TODO
//System.out.println("split " + name + " into " + parts.length + " parts ");
		int p = 0;
		Directory me = (Directory)entries[0];
		if (parts.length == 0)
			return me;
		for(int i=1;i<parts.length - 1;i++)
		{
			p = findBasename(me, parts[i]);
			if (p < 0)
				return null;
			Entry e = entries[p];
			if (!(e instanceof Directory))
				return null;
			me = (Directory)e;
		}
		p = findBasename(me, parts[parts.length -1]);
		if (p < 0)
			return null;
		return entries[p];
	}


	abstract class Entry
	{
		String name;
		abstract void writeToStream(DataOutput out) throws IOException;
	}

	class Directory extends Entry
	{
		int firstPos;
		int lastPos;

		Directory(String name, int first, int last)
		{
			this.name = name;
			this.firstPos = first;
			this.lastPos = last;
		}

		Directory(DataInputStream dis) throws IOException
		{
			byte ver = dis.readByte();
			switch((int)ver)
			{
			case 1:
				this.name = dis.readUTF();
				this.firstPos = dis.readInt();
				this.lastPos = dis.readInt();
				break;
			default:
				throw new IOException("Unsupported Version " + (int)ver + " while reading dir.");
			}
		}

		void writeToStream(DataOutput out) throws IOException
		{
			out.writeByte('D');      // Type = Directory
			out.writeByte(1);        // Version
			out.writeUTF(name);
			out.writeInt(firstPos);
			out.writeInt(lastPos);
		}


		public String[] getChildrenNames()
		{
//System.out.println("get names " + firstPos + " " + lastPos);
			String[] childs = new String[lastPos - firstPos];
			int c = 0;
			for(int i=firstPos;i<lastPos;i++)
			{
				childs[c++] = entries[i].name;
			}

			return childs;
		}
	}

	class File extends Entry
	{
		long size;
		long created;
		// TODO String creator;
		String hash;

		File(String name, long size, long created, String hash)
		{
			this.name = name;
			this.size = size;
			this.created = created;
			this.hash = hash;
		}

		File(DataInputStream dis) throws IOException
		{
			byte ver = dis.readByte();
			switch((int)ver)
			{
			case 1:
				this.name = dis.readUTF();
				this.size = dis.readLong();
				this.created = dis.readLong();
				byte[] hash = new byte[32];
				dis.readFully(hash);
				this.hash = new String(hash);
			default:
				throw new IOException("Unsupported Version " + (int)ver + " while reading dir.");
			}
		}


		void writeToStream(DataOutput out) throws IOException
		{
			out.writeByte('F');      // Type = Directory
			out.writeByte(1);        // Version (incl. hashlength)
			out.writeUTF(name);      // TODO: pos/len
			out.writeLong(size);
			out.writeLong(created);
			out.writeBytes(hash);    // ASCII only
		}

		public long getTime()
		{
			return created;
		}

		public long getSize()
		{
			return size;
		}

		public String getHash()
		{
			return hash;
		}

	}
}