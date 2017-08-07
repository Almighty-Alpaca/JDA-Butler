package com.kantenkugel.discordbot.jenkinsutil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JenkinsBuild
{
    public final int buildNum;
    public final Status status;
    public final OffsetDateTime buildTime;
    public final Map<String, Artifact> artifacts;
    public final List<JenkinsChange> changes;
    public final List<JenkinsUser> culprits;

    private JenkinsBuild(int buildNum, Status status, OffsetDateTime buildTime, int artifactNum, List<JenkinsChange> changes, List<JenkinsUser> culprits)
    {
        this.buildNum = buildNum;
        this.status = status;
        this.buildTime = buildTime;
        this.artifacts = new HashMap<>();
        this.changes = changes;
        this.culprits = culprits;
    }

    public String getUrl()
    {
        return JenkinsApi.JENKINS_BASE + buildNum + "/";
    }

    private void addArtifact(String fileName, String relPath)
    {
        Artifact artifact = new Artifact(fileName, relPath);
        if (artifacts.containsKey(artifact.descriptor))
        {
            JenkinsApi.LOG.warn(String.format("Warning: overwriting artifact with same descriptor: %s -> %s", artifacts.get(artifact.descriptor).fileName, artifact.fileName));
        }
        artifacts.put(artifact.getArtifactDescriptor(), artifact);
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
        public final String descriptor;
        private final String relPath;

        private Artifact(String fileName, String relPath)
        {
            this.fileName = fileName;
            this.descriptor = getArtifactDescriptor();
            this.relPath = relPath;
        }

        public String getLink()
        {
            return JenkinsBuild.this.getUrl() + "artifact/" + relPath;
        }

        private String getArtifactDescriptor()
        {
            String fileEnding = fileName.substring(fileName.lastIndexOf('.') + 1);
            String[] split = fileName
                    .substring(0, fileName.length() - (fileEnding.length() + 1))
                    .split("-");
            if (fileName.startsWith("JDA"))
            {
                return split.length < 3 ? fileEnding : split[split.length - 1];
            }
            else
            {
                return split.length == 1 ? split[0] : split[0] + "-" + split[split.length - 1];
            }
        }
    }

    public enum Status {
        BUILDING, SUCCESS, FAILURE
    }
}
