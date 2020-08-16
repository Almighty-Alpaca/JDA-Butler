package com.almightyalpaca.discord.jdabutler.util.gradle;

import com.almightyalpaca.discord.jdabutler.Bot;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class GradleDownloader
{
    public static final File GRADLE_DIR = new File("gradle-cache/");

    public static final String GRADLE_VERSION = "6.5";

    public static final File GRADLE_ZIP = new File(GradleDownloader.GRADLE_DIR, "gradle-" + GradleDownloader.GRADLE_VERSION + "-bin.zip");

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static String getGradleDistributionURL()
    {
        return "https://services.gradle.org/distributions/gradle-" + GradleDownloader.GRADLE_VERSION + "-bin.zip";
    }

    public static File getExecutableGradleFile()
    {
        if (!GradleDownloader.initialized.getAndSet(true))
            GradleDownloader.downloadGradle();

        return new File(GradleDownloader.GRADLE_DIR, "/gradle-" + GradleDownloader.GRADLE_VERSION + "/bin/gradle" + (SystemUtils.IS_OS_WINDOWS ? ".bat" : ""));
    }

    private static void downloadGradle()
    {
        if(getExecutableGradleFile().exists())
            return; //don't re-fetch already existing gradle version

        Bot.LOG.info("Downloading gradle...");
        try
        {
            if (GRADLE_ZIP.exists())
                GRADLE_ZIP.delete();
            else
                GRADLE_ZIP.getParentFile().mkdirs();
            GRADLE_ZIP.createNewFile();

            FileUtils.copyURLToFile(new URL(getGradleDistributionURL()), GRADLE_ZIP);

            final ZipFile zip = new ZipFile(GRADLE_ZIP);

            zip.extractAll(GRADLE_DIR.getAbsolutePath());
            getExecutableGradleFile().setExecutable(true);
            GRADLE_ZIP.delete();
        }
        catch (IOException | ZipException e)
        {
            Bot.LOG.error("There was an error downloading/extracting gradle", e);
        }
    }
}
