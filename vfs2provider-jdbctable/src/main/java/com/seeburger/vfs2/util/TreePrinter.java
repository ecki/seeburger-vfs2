/*
 * TreePrinter.java
 *
 * created at 2013-08-16 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.util;


import java.io.PrintStream;
import java.util.Date;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;


/**
 * Utility Functions for printing a VFS2 Tree.
 */
public class TreePrinter
{
    /**
     * Print the content of file and its childs to the given print stream.
     * <P>
     * Format is <code>FILENAME (dHRW)[[ date=DATE] size=SIZE att=ATTRIB]</code>
     * <P>
     * d=Directory (has child); H=Hidden; R=is Readable;W=is writeable. Unset flags are represented as {@code .}.
     * <p>
     * DATE is last modified date (skipped if 0).
     * <p>
     * This implementation is recursive, it is not intended for production uses. Use it only for
     * diagnostic and debugging utilities.
     *
     * @param file the file node to recursively print
     * @param prefix text prefix to print at beginning of lines (will be extended with blank for deeper levels).
     * @param out target to write the formatted lines to
     * @throws FileSystemException passed unmodified from VFS methods.
     */
    public static void printTree(FileObject file, String prefix, PrintStream out) throws FileSystemException
    {
        String type = "";
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
            // print only file dates which are not 0 (1970-01-01)
            try
            {
                long last = content.getLastModifiedTime();
                if (last != 0)
                {
                    type += " date=" + new Date(last);
                }
            }
            catch (Exception ig) { /* ignored */ }

            try
            {
                type += " size=" + content.getSize();
            }
            catch (Exception ig) { /* ignored */ }

            try
            {
                type += " att=" + content.getAttributes();
            }
            catch (Exception ig) { /* ignored */  }
        }
        String fileName = file.getName().getPathDecoded();
        if (prefix.length() + fileName.length() < 60)
        {
            fileName = (fileName + "                                                            ").substring(0, 60 - prefix.length());
        }

        if (file.getType().hasChildren())
        {
            FileObject[] children = null;
            try
            {
                children = file.getChildren();
                out.println(prefix + fileName + " (d" + type);
            }
            catch (FileSystemException ignored)
            {
                out.println(prefix + fileName + " (d"+type + " (" + ignored + ")");
            }
            if (children != null)
            {
                for(FileObject fo : children)
                {
                    printTree(fo, prefix + "  ", out);
                }
            }
        }
        else if (file.getType() == FileType.FILE)
        {
            out.println(prefix + fileName + " (." + type);
        }
        else
        {
            out.println(prefix + fileName + " (-" + type);
        }
    }

}



