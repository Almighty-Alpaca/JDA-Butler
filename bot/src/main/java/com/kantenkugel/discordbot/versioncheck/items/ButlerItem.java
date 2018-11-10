package com.kantenkugel.discordbot.versioncheck.items;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.versioncheck.JenkinsVersionSupplier;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.UpdateHandler;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.almightyalpaca.discord.jdabutler.Bot.LOG;

public class ButlerItem extends VersionedItem implements UpdateHandler
{
    private static final Path UPDATE_FILE = Paths.get("Bot_Update.jar");
    private static final Path LAUNCHER_FILE = Paths.get("Launcher.jar");

    private static final JenkinsApi JENKINS = JenkinsApi.forConfig("https://ci.dv8tion.net", "JDA-Butler");
    private static final JenkinsVersionSupplier VERSION_SUPPLIER = new JenkinsVersionSupplier(JENKINS, true);
    private static final List<String> ALIASES = Collections.singletonList("butler");

    @Override
    public String getName()
    {
        return "JDA-Butler";
    }

    @Override
    public RepoType getRepoType()
    {
        return null;
    }

    @Override
    public String getGroupId()
    {
        return null;
    }

    @Override
    public String getArtifactId()
    {
        return null;
    }

    @Override
    public List<String> getAliases()
    {
        return ALIASES;
    }

    @Override
    public String getUrl()
    {
        return "https://github.com/Almighty-Alpaca/JDA-Butler";
    }

    @Override
    public UpdateHandler getUpdateHandler()
    {
        return this;
    }

    @Override
    public Supplier<String> getCustomVersionSupplier()
    {
        return VERSION_SUPPLIER;
    }

    @Override
    public void onUpdate(VersionedItem item, String previousVersion, boolean shouldAnnounce)
    {
        LOG.warn("Updating Butler to new build #{}", item.getVersion());
        try
        {
            JenkinsBuild lastSuccessfulBuild = JENKINS.getLastSuccessfulBuild();
            if(lastSuccessfulBuild == null)
            {
                LOG.error("For some reason, the latest build was null");
                return;
            }
            JenkinsBuild.Artifact bot = lastSuccessfulBuild.artifacts.get("Bot");
            JenkinsBuild.Artifact launcher = lastSuccessfulBuild.artifacts.get("Launcher");
            if(bot == null || launcher == null)
            {
                LOG.error("Could not find all required artifacts (Bot, Launcher) in {}", lastSuccessfulBuild.artifacts);
                return;
            }
            LOG.info("Downloading new Butler version (+ launcher)...");
            try
            {
                Response res = Bot.httpClient.newCall(new Request.Builder().url(bot.getLink()).get().build()).execute();
                if(!res.isSuccessful())
                {
                    LOG.warn("OkHttp returned failure for {}", bot.getLink());
                    return;
                }
                try(ResponseBody body = res.body())
                {
                    final InputStream is = body.byteStream();
                    Files.copy(is, UPDATE_FILE, StandardCopyOption.REPLACE_EXISTING);
                    is.close();
                }
                res = Bot.httpClient.newCall(new Request.Builder().url(launcher.getLink()).get().build()).execute();
                if(!res.isSuccessful())
                {
                    LOG.warn("OkHttp returned failure for {}", launcher.getLink());
                    return;
                }
                try(ResponseBody body = res.body())
                {
                    final InputStream is = body.byteStream();
                    Files.copy(is, LAUNCHER_FILE, StandardCopyOption.REPLACE_EXISTING);
                    is.close();
                }
            }
            catch(Exception e)
            {
                LOG.error("Error downloading new Butler version", e);
                return;
            }
            LOG.info("Starting updater and exiting");
            new ProcessBuilder("java", "-jar", LAUNCHER_FILE.toString())
                    .redirectInput(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            Bot.shutdown();
        }
        catch(IOException e)
        {
            LOG.error("Could not get latest Butler build (from cache!)", e);
        }
    }
}
