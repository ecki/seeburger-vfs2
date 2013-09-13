/*
 * FileNameGlobbing.java
 *
 * created at 12.09.2013 by Eckenfel <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package com.seeburger.vfs2.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.Selectors;


public class FileNameGlobbing
{
    private final String pattern;

    private String[] explodedPattern;

    public FileNameGlobbing(String pattern)
    {
        if (pattern == null || pattern.isEmpty())
            throw new IllegalArgumentException("FileNameGlobbing with empty patter is not possible");

        this.pattern = pattern;
    }

    /** Returns all fixed path components. */
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

    /** True if it contains any (unescaped) globbing patterns. */
    private boolean containsWildcard(String string)
    {
        // TODO: escaping
        if (string.contains("*"))
            return true;

        if (string.contains("?"))
            return true;

        return false;
    }

    public String[] getExplodedPattern()
    {
        if (explodedPattern == null)
        {
            explodedPattern = getExploded(pattern);
        }
        return explodedPattern;
    }

    /**
     * Separate path by /, make sure each component ends with /.
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

    public FileSelector getSelector()
    {
        return new FileSelectorImpl();
    }

    class FileSelectorImpl implements FileSelector
    {
        private String[] exploded;
        private Pattern matcher;
        private int maxCheckDir = -1;

        FileSelectorImpl()
        {
            System.out.println("Created new selector " + pattern);
            exploded = getExplodedPattern();
            for(int i=0;i<exploded.length;i++)
            {
                if (exploded[i].equals("**") || exploded[i].equals("**/"))
                {
                    maxCheckDir = i; System.out.println("maxCheck " + maxCheckDir);
                    break;
                }
            }
            matcher = getPatternMatcher();
            System.out.println("Created new selector " + matcher.pattern());
        }

        public boolean includeFile(FileSelectInfo fileInfo)
            throws Exception
        {
            FileObject file = fileInfo.getFile();

            if (!file.getType().hasContent())
            {
                System.out.println("notmatch " + file);
                return false;
            }

            String relname = fileInfo.getBaseFolder().getName().getRelativeName(file.getName());



            boolean match = matcher.matcher(relname).matches();

            if(match)
                System.out.println("+++ match " + relname + " as in " + fileInfo.getFile());
            else
                System.out.println("  - match " + relname + " as in " + fileInfo.getFile());

            return match;
        }

        public boolean traverseDescendents(FileSelectInfo fileInfo)
            throws Exception
        {
            int depth = fileInfo.getDepth();
            String myname = fileInfo.getFile().getName().getBaseName();

            // do we need to step into any first level child?
            if (depth == 0)
            {
                if ((exploded.length > 1) || (exploded.length == 1 && exploded[0].endsWith("/")))
                    return true;
                else
                    return false;
            }

            if (maxCheckDir != -1 && depth >= maxCheckDir)
                return true;

            if (depth <= exploded.length)
                return matchDir(exploded[depth-1], myname + "/");

            return false;
        }

        private boolean matchDir(String pattern, String dirname)
        {
            System.out.println("matching " + pattern + " against " + dirname);

            if (pattern.equals("*/") && dirname.endsWith("/"))
                return true;

            return pattern.equals(dirname);
        }
    }

    public Pattern getPatternMatcher()
    {
        String pattern2 = pattern.replaceAll("\\\\", "\\\\");
        pattern2 = pattern2.replaceAll("\\[", "\\[");
        pattern2 = pattern2.replaceAll("\\]", "\\]");
        pattern2 = pattern2.replaceAll("\\.", "\\.");
        pattern2 = pattern2.replaceAll("\\^", "\\^");
        pattern2 = pattern2.replaceAll("\\$", "\\$");

        pattern2 = pattern2.replaceAll("^\\*\\*/", ".*/");
        pattern2 = pattern2.replaceAll("/\\*\\*/", "/.*/");
        pattern2 = pattern2.replaceAll("^\\*", "[^/]+");
        pattern2 = pattern2.replaceAll("([^.])\\*", "$1[^/]+");
        pattern2 = pattern2.replaceAll("\\?", "[^/]");

        return Pattern.compile("^" + pattern2 + "$");
    }
}



