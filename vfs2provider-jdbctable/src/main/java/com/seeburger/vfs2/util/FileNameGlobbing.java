/*
 * FileNameGlobbing.java
 *
 * created at 2013-09-12 by Bernd Eckenfels <b.eckenfels@seeburger.de>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.util;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileType;


/**
 * Brings unix-style file-name globbing to the VFS word.
 */
public class FileNameGlobbing
{
    private final String pattern;

    private String[] explodedPattern;


    /**
     * Constructs a globbing Object with given expression.
     * <P>
     * See {@link FileNameGlobbing} class for syntax definition.
     *
     * @param pattern the '/' terminated file path pattern
     * @throws IllegalArgumentException if pattern is not valid
     */
    public FileNameGlobbing(String pattern)
    {
        if (pattern == null || pattern.isEmpty())
        {
            throw new IllegalArgumentException("FileNameGlobbing with empty pattern is not possible");
        }

        this.pattern = pattern;
    }

    /**
     * Returns all fixed path components.
     * <P>
     * This returns the part of the pattern which does not contain wildcards.
     */
    public String getBase()
    {
        String[] exploded = getExplodedPattern();
        int pos = 0;
        for(pos = 0;pos < exploded.length;pos++)
        {
            if (containsWildcard(exploded[pos]))
                break;
        }

        return createPath(exploded, 0, pos);
    }

    public String[] getExplodedPattern()
    {
        if (explodedPattern == null)
        {
            explodedPattern = getExploded(pattern);
        }
        return explodedPattern;
    }

    public FileSelector getSelector()
    {
        return new FileSelectorImpl();
    }

    /** True if it contains any (unescaped) globbing patterns. */
    static boolean containsWildcard(String string)
    {
        // TODO: escaping
        if (string.contains("*")) // also captures **
            return true;

        if (string.contains("?"))
            return true;

        return false;
    }

    /**
     * Split path by /, make sure each component ends with /.
     *
     * @param patternArg relative file path, potentially containing patterns
     * @return array with separated path components, first component starts with / if absolute, last component ends with / if dir.
     */
    static String[] getExploded(String patternArg)
    {
        String remain = patternArg;
        List<String> components = new ArrayList<String>(10);

        while(remain != null)
        {
            int pos = remain.indexOf('/');
            if (pos == -1)
            {
                components.add(remain); // last remainder with no trailing slash -> file
                remain = null;
            } else {
                components.add(remain.substring(0, pos + 1));
                remain = remain.substring(pos+1);
                if (remain.isEmpty()) // last component was a directory
                    remain = null;
            }
        }
        return components.toArray(new String[components.size()]);
    }

    /** Construct a path from components index start..end. */
    static String createPath(String[] components, int start, int end)
    {
        StringBuilder builder = null;
        for(int i=start; i<end && i< components.length; i++)
        {
            if (builder != null)
                builder.append(components[i]); // ends in / unless last
            else
                builder = new StringBuilder(components[i]); // ends in / unless last
        }

        if (builder != null)
            return builder.toString();
        else
            return "";
    }

    class FileSelectorImpl implements FileSelector
    {
        private String[] exploded;
        private Pattern matcher;
        private int maxCheckDir = -1;

        FileSelectorImpl()
        {
            exploded = getExplodedPattern();
            for(int i=0;i<exploded.length;i++)
            {
                if (exploded[i].equals("**") || exploded[i].equals("**/"))
                {
                    maxCheckDir = i;
                    break;
                }
            }
            matcher = getPatternMatcher();
        }

        public boolean includeFile(FileSelectInfo fileInfo)
            throws Exception
        {
            FileObject file = fileInfo.getFile();

            // there is currently a problem with WebDAV provider not excluding "self" respones
            if (file.getType() == FileType.IMAGINARY)
            {
                return false;
            }

            String relname = fileInfo.getBaseFolder().getName().getRelativeName(file.getName());
            // we never match the directory itself
            if (".".equals(relname))
            {
                return false;
            }

            if (file.getType().hasChildren())
            {
                relname = ensureSuffix(relname, "/");
            }

            return matcher.matcher(relname).matches();
        }

        public boolean traverseDescendents(FileSelectInfo fileInfo)
            throws Exception
        {
            int depth = fileInfo.getDepth();
            // we always traverse the start dir
            if (depth == 0)
            {
                    return true;
            }

            // we actually need to traverse whole structure in order to do full string match
            if (maxCheckDir != -1 && depth >= maxCheckDir)
            {
                return true;
            }

            FileObject file = fileInfo.getFile();

            if (file.getType().hasContent() && depth-1 < exploded.length)
            {
                return false;
            }

            // depth can not be 0 here
            if (depth <= exploded.length)
            {
                String basename = file.getName().getBaseName();
                return matchDir(exploded[depth-1], ensureSuffix(basename, "/"));
            }

            return false;
        }


        private String ensureSuffix(String base, String suffix)
        {
            if (base == null || base.endsWith(suffix))
                return base;
            return base + suffix;
        }


        private boolean matchDir(String pattern, String dirname)
        {
            // TODO: do we check for wildcards here anyway?
            if (pattern.equals("*/") && dirname.endsWith("/"))
            {
                return true;
            }

            return pattern.equals(dirname);
        }
    }

    /** Return the pattern as a compiled regular expression. */
    public Pattern getPatternMatcher()
    {
        // first escape all RE syntax elements
        String pattern2 = pattern.replaceAll("\\\\", "\\\\");
        pattern2 = pattern2.replaceAll("\\[", "\\[");
        pattern2 = pattern2.replaceAll("\\]", "\\]");
        pattern2 = pattern2.replaceAll("\\.", "\\.");
        pattern2 = pattern2.replaceAll("\\^", "\\^");
        pattern2 = pattern2.replaceAll("\\$", "\\$");

        // then replace globbing syntax elements with expressions
        pattern2 = pattern2.replaceAll("^\\*\\*/", ".+/"); // "**/" -> ".+/"
        pattern2 = pattern2.replaceAll("/\\*\\*/", "/.+/"); //
        pattern2 = pattern2.replaceAll("/\\*\\*", "/.*[^/]");

        // when replacing * with "[^/]+" we need to make sure not to replace ".*"
        // TODO: this does not works for file.* (aka file\\.*)
        pattern2 = pattern2.replaceAll("^\\*", "[^/]+");
        pattern2 = pattern2.replaceAll("([^.])\\*", "$1[^/]+");

        pattern2 = pattern2.replaceAll("\\?", "[^/]");

        return Pattern.compile("^" + pattern2 + "$");
    }
}



