package org.sparrow.common.util;

import com.google.common.base.Joiner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mauricio on 04/10/2015.
 */
public class FileUtils
{
    private static final double KB = 1024d;
    private static final double MB = 1024*1024d;
    private static final double GB = 1024*1024*1024d;
    private static final double TB = 1024*1024*1024*1024d;

    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public static File createTempFile(String prefix, String suffix, File directory) throws Exception
    {
        return File.createTempFile(prefix, suffix, directory);
    }

    public static File createTempFile(String prefix, String suffix) throws Exception
    {
        return createTempFile(prefix, suffix, new File(System.getProperty("java.io.tmpdir")));
    }

    public static String getCanonicalPath(String filename) throws Exception
    {
        return new File(filename).getCanonicalPath();
    }

    public static String getCanonicalPath(File file) throws Exception
    {
        return file.getCanonicalPath();
    }

    public static void createDirectory(String directory) throws Exception
    {
        createDirectory(new File(directory));
    }

    public static void createDirectory(File directory) throws Exception
    {
        if (!directory.exists())
        {
            if (!directory.mkdirs())
                throw new Exception("Failed to mkdirs " + directory);
        }
    }

    public static void createFile(String file)
    {
        createFile(new File(file));
    }

    public static void createFile(File file)
    {
        try
        {
            file.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static long getFileSize(String filename)
    {
        return Paths.get(filename).toFile().length();
    }

    public static void delete(String path)
    {
        File file = new File(path);
        if(file.isDirectory())
        {
            if(file.list().length==0)
            {
                file.delete();
            }
            else
            {
                String files[] = file.list();

                for (String temp : files)
                {
                    File fileDelete = new File(file, temp);
                    delete(fileDelete.getPath());
                }
                if(file.list().length==0)
                {
                    file.delete();
                }
            }

        }
        else
        {
            file.delete();
        }
    }

    public static void delete(File[] files)
    {
        for (File file : files)
        {
            file.delete();
        }
    }

    public static String stringifyFileSize(double value)
    {
        double d;
        if ( value >= TB )
        {
            d = value / TB;
            String val = decimalFormat.format(d);
            return val + " TB";
        }
        else if ( value >= GB )
        {
            d = value / GB;
            String val = decimalFormat.format(d);
            return val + " GB";
        }
        else if ( value >= MB )
        {
            d = value / MB;
            String val = decimalFormat.format(d);
            return val + " MB";
        }
        else if ( value >= KB )
        {
            d = value / KB;
            String val = decimalFormat.format(d);
            return val + " KB";
        }
        else
        {
            String val = decimalFormat.format(value);
            return val + " bytes";
        }
    }

    public static File[] listSubdirectories(File directory)
    {
        File[] directories = directory.listFiles();
        List<File> resultDirectories = new ArrayList<>();

        for (File file : directories)
        {
            if (file.isDirectory())
                resultDirectories.add(file);
        }

        return resultDirectories.toArray(new File[resultDirectories.size()]);
    }

    public static List<File> listFiles(File directory)
    {
        File[] directories = directory.listFiles();
        List<File> result = new ArrayList<>();

        if (directory.exists()) {
            result.addAll(Arrays.stream(directories)
                    .filter(File::isFile)
                    .collect(Collectors.toList()));
        }

        return result;
    }

    public static long folderSize(File directory)
    {
        long length = 0;
        for (File file : directory.listFiles())
        {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }

    public static boolean fileExists(String filename)
    {
        File f = new File(filename);
        return f.exists() && f.isFile();
    }

    public static String joinPath(String... args)
    {
        return Joiner.on(System.getProperty("file.separator")).join(args);
    }
}
