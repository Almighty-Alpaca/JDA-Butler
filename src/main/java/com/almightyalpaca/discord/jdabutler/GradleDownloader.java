package com.almightyalpaca.discord.jdabutler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;

public class GradleDownloader
{

    public static final File GRADLE_DIR = new File("gradle-cache/");

    public static final String GRADLE_VERSION = "3.2.1";
    public static final File GRADLE_ZIP = new File(GradleDownloader.GRADLE_DIR, "gradle-" + GradleDownloader.GRADLE_VERSION + "-bin.zip");

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static String getGradleDitributionURL()
    {
        return "https://services.gradle.org/distributions/gradle-" + GradleDownloader.GRADLE_VERSION + "-bin.zip";
    }

    public static File getGradlePath()
    {
        if (!GradleDownloader.initialized.getAndSet(true))
            GradleDownloader.downloadGradle();

        return new File(GradleDownloader.GRADLE_DIR, "/gradle-" + GradleDownloader.GRADLE_VERSION + "/bin/gradle" + (System.getProperty("os.name").toLowerCase().contains("windows") ? ".bat" : ""));
    }

    private static void downloadGradle()
    {
        if (!GradleDownloader.getGradlePath().exists())
            try
            {

                if (GradleDownloader.GRADLE_ZIP.exists())
                    GradleDownloader.GRADLE_ZIP.delete();
                else
                    GradleDownloader.GRADLE_ZIP.getParentFile().mkdirs();
                GradleDownloader.GRADLE_ZIP.createNewFile();

                FileUtils.copyURLToFile(new URL(GradleDownloader.getGradleDitributionURL()), GradleDownloader.GRADLE_ZIP);

                final ZipFile zip = new ZipFile(GradleDownloader.GRADLE_ZIP);

                zip.extractAll(GradleDownloader.GRADLE_DIR.getAbsolutePath());

            }
            catch (IOException | ZipException e)
            {
                throw new RuntimeException(e);
            }
    }

}
