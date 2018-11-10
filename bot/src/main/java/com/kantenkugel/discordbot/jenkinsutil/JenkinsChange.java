package com.kantenkugel.discordbot.jenkinsutil;

import net.dv8tion.jda.core.utils.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JenkinsChange
{
    public final String commitId;
    public final JenkinsUser author;
    public final String commitMsg;
    public final OffsetDateTime commitTime;
    public final List<Pair<EditType, String>> changedFiles;

    public JenkinsChange(String commitId, JenkinsUser author, String commitMsg, OffsetDateTime commitTime, List<Pair<EditType, String>> changedFiles)
    {
        this.commitId = commitId;
        this.author = author;
        this.commitMsg = commitMsg;
        this.commitTime = commitTime;
        this.changedFiles = changedFiles;
    }

    public String getShortId()
    {
        return commitId.substring(0, 7);
    }

    public enum EditType {
        ADD, EDIT, DELETE
    }

    //2017-08-06 23:53:16 +0200
    private static final DateTimeFormatter commitDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss x");

    public static JenkinsChange fromJson(JSONObject json)
    {
        JSONObject authorObj = json.getJSONObject("author");
        JenkinsUser author = new JenkinsUser(authorObj.getString("fullName"), authorObj.getString("id"),
                authorObj.isNull("description") ? null : authorObj.getString("description"));

        JSONArray fileChangeArr = json.getJSONArray("paths");
        List<Pair<EditType, String>> fileChanges = new ArrayList<>(fileChangeArr.length());
        for (int i = 0; i < fileChangeArr.length(); i++)
        {
            JSONObject fileChange = fileChangeArr.getJSONObject(i);
            fileChanges.add(Pair.of(
                    EditType.valueOf(fileChange.getString("editType").toUpperCase()),
                    fileChange.getString("file")
            ));
        }

        OffsetDateTime commitTime = OffsetDateTime.parse(json.getString("date"), commitDateFormatter);

        return new JenkinsChange(json.getString("commitId"), author, json.getString("comment").trim(), commitTime, fileChanges);
    }
}
