package com.kantenkugel.discordbot.jenkinsutil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class JenkinsBuild
{
    public final int buildNum;
    public final Status status;
    public final OffsetDateTime buildTime;
    public final List<Artifact> artifacts;
    public final List<JenkinsChange> changes;
    public final List<JenkinsUser> culprits;

    private JenkinsBuild(int buildNum, Status status, OffsetDateTime buildTime, int artifactNum, List<JenkinsChange> changes, List<JenkinsUser> culprits)
    {
        this.buildNum = buildNum;
        this.status = status;
        this.buildTime = buildTime;
        this.artifacts = new ArrayList<>(artifactNum);
        this.changes = changes;
        this.culprits = culprits;
    }

    public String getUrl()
    {
        return JenkinsApi.JENKINS_BASE + buildNum + "/";
    }

    private void addArtifact(String fileName, String relPath)
    {
        artifacts.add(new Artifact(fileName, relPath));
    }

    static JenkinsBuild fromJson(JSONObject json)
    {
        int buildNum = json.getInt("id");
        Status status = json.getBoolean("building") ? Status.BUILDING : Status.valueOf(json.getString("result"));
        OffsetDateTime buildTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(json.getLong("timestamp")), ZoneId.systemDefault());

        JSONArray changeArr = json.getJSONObject("changeSet").getJSONArray("items");
        List<JenkinsChange> changes = new ArrayList<>(changeArr.length());
        for (int i = 0; i < changeArr.length(); i++)
        {
            changes.add(JenkinsChange.fromJson(changeArr.getJSONObject(i)));
        }

        JSONArray culpritArr = json.getJSONArray("culprits");
        List<JenkinsUser> culprits = new ArrayList<>(culpritArr.length());
        for (int i = 0; i < culpritArr.length(); i++)
        {
            JSONObject culprit = culpritArr.getJSONObject(i);
            culprits.add(new JenkinsUser(culprit.getString("fullName"), culprit.getString("id"),
                    culprit.isNull("description") ? null : culprit.getString("description")));
        }

        JSONArray artifactArr = json.getJSONArray("artifacts");

        JenkinsBuild build = new JenkinsBuild(buildNum, status, buildTime, artifactArr.length(), changes, culprits);

        for (int i = 0; i < artifactArr.length(); i++)
        {
            JSONObject artifactObj = artifactArr.getJSONObject(i);
            build.addArtifact(artifactObj.getString("fileName"), artifactObj.getString("relativePath"));
        }

        return build;
    }

    public class Artifact
    {
        public final String fileName;
        private final String relPath;

        private Artifact(String fileName, String relPath)
        {
            this.fileName = fileName;
            this.relPath = relPath;
        }

        public String getLink()
        {
            return JenkinsBuild.this.getUrl() + "artifact/" + relPath;
        }
    }

    public enum Status {
        BUILDING, SUCCESS, FAILURE
    }
}
