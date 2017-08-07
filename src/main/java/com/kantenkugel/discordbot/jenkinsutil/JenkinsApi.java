package com.kantenkugel.discordbot.jenkinsutil;

import com.almightyalpaca.discord.jdabutler.Bot;
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

    private static final String API_SUFFIX = "api/json?";

    private static final String BUILD_OPTIONS = "tree=artifacts[*],id,building,result,timestamp," +
            "changeSet[items[commitId,date,msg,author[fullName,id,description],paths[*]]]," +
            "culprits[fullName,id,description]";

    private static JenkinsBuild lastSuccBuild = null;

    public static JenkinsBuild fetchBuild(int buildNumber)
    {
        return getBuild(buildNumber + "/");
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
            return JenkinsBuild.fromJson(new JSONObject(res.body().string()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    //TODO: Remove before merging to master
    public static void main(String[] args)
    {
        Bot.httpClient = new OkHttpClient.Builder().build();

        JenkinsBuild build = getLastSuccessfulBuild();
        System.out.println("Build:  " + build.buildNum);
        System.out.println("Time:   " + build.buildTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        System.out.println("Status: " + build.status);
        System.out.println("URL:    " + build.getUrl());
        System.out.println("Artifacts:");
        for (JenkinsBuild.Artifact artifact : build.artifacts)
        {
            System.out.println(" -Name: " + artifact.fileName);
            System.out.println("  URL:  " + artifact.getLink());
        }
        System.out.println("Commits:");
        for (JenkinsChange change : build.changes)
        {
            System.out.println(" -ID:     " + change.getShortId());
            System.out.println("  Time:   " + change.commitTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
            System.out.println("  Msg:    " + change.commitMsg);
            System.out.println("  Author: " + change.author);
            System.out.println("  Files Changed: " + change.changedFiles.size());
        }
        System.out.println("Culprits:");
        for (JenkinsUser culprit : build.culprits)
        {
            System.out.println("  " + culprit);
        }
    }
}
