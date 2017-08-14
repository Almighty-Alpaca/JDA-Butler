package com.kantenkugel.discordbot.jenkinsutil;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.util.FixedSizeCache;
import net.dv8tion.jda.core.utils.SimpleLog;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class JenkinsApi
{
    public static final String JENKINS_BASE = "http://home.dv8tion.net:8080/job/JDA/";
    public static final String CHANGE_URL = JENKINS_BASE + "changes";
    public static final String LAST_BUILD_URL = JENKINS_BASE + "lastSuccessfulBuild/";

    static final SimpleLog LOG = SimpleLog.getLog("Jenkins");

    private static final String API_SUFFIX = "api/json?";

    private static final String BUILD_OPTIONS = "tree=artifacts[*],id,building,result,timestamp," +
            "changeSet[items[commitId,date,comment,author[fullName,id,description],paths[*]]]," +
            "culprits[fullName,id,description]";

    private static final FixedSizeCache<Integer, JenkinsBuild> resultCache = new FixedSizeCache<>(20);

    private static JenkinsBuild lastSuccBuild = null;

    public static JenkinsBuild getBuild(int buildNumber)
    {
        return resultCache.contains(buildNumber)
                ? resultCache.get(buildNumber)
                : getBuild(buildNumber + "/");
    }

    public static JenkinsBuild fetchLastSuccessfulBuild()
    {
        return lastSuccBuild = getBuild("lastSuccessfulBuild/");
    }

    public static JenkinsBuild getLastSuccessfulBuild()
    {
        if(lastSuccBuild == null)
            return fetchLastSuccessfulBuild();
        return lastSuccBuild;
    }

    private static JenkinsBuild getBuild(String identifier)
    {
        Request req = new Request.Builder().url(JENKINS_BASE + identifier + API_SUFFIX + BUILD_OPTIONS).get().build();
        try
        {
            Response res = Bot.httpClient.newCall(req).execute();
            JenkinsBuild build = JenkinsBuild.fromJson(new JSONObject(res.body().string()));
            if(build.status != JenkinsBuild.Status.BUILDING)
                resultCache.add(build.buildNum, build);
            return build;
        } catch (IOException e)
        {
            LOG.fatal("Error while Fetching Jenkins build " + identifier);
            //LOG.log(e);
        }
        return null;
    }
}
