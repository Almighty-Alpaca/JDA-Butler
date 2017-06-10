package com.almightyalpaca.discord.jdabutler.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils
{
    public static boolean isEmpty(final File file) throws IOException
    {
        final BufferedReader br = new BufferedReader(new FileReader(file));
        final boolean isEmpty = br.readLine() == null;
        br.close();
        return isEmpty;
    }

    public static List<File> listfiles(final File directory)
    {
        final List<File> files = new ArrayList<>();
        for (final File file : directory.listFiles())
            if (file.isFile())
                files.add(file);
            else if (file.isDirectory())
                files.addAll(FileUtils.listfiles(file));
        return files;
    }

}
