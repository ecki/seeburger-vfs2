/*
 * VFSUtils.java
 *
 * created at 2013-09-13 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.util;


import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;


public class VFSUtils
{
    /**
     * Compare the last modified times of two files.
     * <P>
     * This method will use the bigger accuracy of the two file systems, but no more than 10s.
     *
     * @return 0 if both times are same (within accuracy), 1 if fileA was modified after fileB (newer), -1 if fileB is newer
     * @throws FileSystemException if getLastModified on fileA or fileB failed.
     * */
    public static int compareFileTimes(FileObject fileA, FileObject fileB) throws FileSystemException
    {
        long modifiedA = fileA.getContent().getLastModifiedTime();
        long modifiedB = fileA.getContent().getLastModifiedTime();
        double accuracyA = Math.abs(fileA.getFileSystem().getLastModTimeAccuracy());
        double accuracyB = Math.abs(fileB.getFileSystem().getLastModTimeAccuracy());
        double accuracy = Math.max(accuracyA, accuracyB);
        // make sure accuracy is capped to 10s
        accuracy = Math.min(accuracy, 10*1000);

        if (Math.abs(modifiedA - modifiedB) <= accuracy) // accuracy can be 0
            return 0;

        if (modifiedA < modifiedB)
            return -1;
        else
            return 1;
    }
}
