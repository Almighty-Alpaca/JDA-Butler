package com.kantenkugel.discordbot.jenkinsutil;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.util.FixedSizeCache;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;

public class JenkinsApi
{
    public static final JenkinsApi JDA_JENKINS = JenkinsApi.forConfig("http://home.dv8tion.net:8080", "JDA");

    /**
     * Returns a new JenkinsApi instance for given configuration
     *
     * @param baseUrl
     *          The base url of a Jenkins server (jenkins' base page)
     * @param jobName
     *          The name of the Jenkins job to bind to
     *
     * @return  A new JenkinsApi instance bound to given Jenkins server and job
     */
    public static JenkinsApi forConfig(String baseUrl, String jobName)
    {
        if(!baseUrl.endsWith("/"))
            baseUrl += '/';
        return forConfig(baseUrl + "job/" + jobName);
    }

    /**
     * Returns a new JenkinsApi instance for given configuration
     *
     * @param fullJobPath
     *          The full url of a Jenkins job
     *
     * @return  A new JenkinsApi instance Jenkins job (via full url)
     */
    public static JenkinsApi forConfig(String fullJobPath)
    {
        if(!fullJobPath.endsWith("/"))
            fullJobPath += '/';
        return new JenkinsApi(fullJobPath);
    }

    static final Logger LOG = LoggerFactory.getLogger(JenkinsApi.class);

    private static final String API_SUFFIX = "api/json?";

    private static final String BUILD_OPTIONS = "tree=artifacts[*],id,building,result,timestamp," +
            "changeSet[items[commitId,date,comment,author[fullName,id,description],paths[*]]]," +
            "culprits[fullName,id,description]";

    private static final String LATEST_SUCC_SUFFIX = "lastSuccessfulBuild/";
    private static final String CHANGE_SUFFIX = "changes";

    public final String jenkinsBase;

    private final FixedSizeCache<Integer, JenkinsBuild> resultCache = new FixedSizeCache<>(20);

    private JenkinsBuild lastSuccBuild = null;

    public JenkinsBuild getBuild(int buildNumber)
    {
        return resultCache.contains(buildNumber)
                ? resultCache.get(buildNumber)
                : getBuild(buildNumber + "/");
    }

    public JenkinsBuild fetchLastSuccessfulBuild()
    {
        return lastSuccBuild = getBuild(LATEST_SUCC_SUFFIX);
    }

    public JenkinsBuild getLastSuccessfulBuild()
    {
        if(lastSuccBuild == null)
            return fetchLastSuccessfulBuild();
        return lastSuccBuild;
    }

    public String getChangesetUrl()
    {
        return jenkinsBase + CHANGE_SUFFIX;
    }

    public String getLastSuccessfulBuildUrl()
    {
        return jenkinsBase + LATEST_SUCC_SUFFIX;
    }

    private JenkinsBuild getBuild(String identifier)
    {
        Request req = new Request.Builder().url(jenkinsBase + identifier + API_SUFFIX + BUILD_OPTIONS).get().build();
        try
        {
            Response res = Bot.httpClient.newCall(req).execute();
            if(!res.isSuccessful())
                return null;
            JenkinsBuild build = JenkinsBuild.fromJson(new JSONObject(new JSONTokener(res.body().charStream())), this);
            if(build.status != JenkinsBuild.Status.BUILDING)
                resultCache.add(build.buildNum, build);
            return build;
        }
        catch(SocketTimeoutException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (IOException e)
        {
            LOG.error("Error while Fetching Jenkins build {} for {}", identifier, jenkinsBase);
        }
        return null;
    }

    private JenkinsApi(String jenkinsurl)
    {
        this.jenkinsBase = jenkinsurl;
    }
}
