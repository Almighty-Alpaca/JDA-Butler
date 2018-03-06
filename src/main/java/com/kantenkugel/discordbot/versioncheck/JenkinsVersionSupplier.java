package com.kantenkugel.discordbot.versioncheck;

import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;

import java.util.function.Supplier;

/**
 * Simple implementation aimed to be served to {@link VersionedItem#getCustomVersionSupplier()}
 * This uses a given JenkinsApi to get the version.
 * <br/>It first uses artifact names if available and falls back to build numbers if no artifact is available.
 */
public class JenkinsVersionSupplier implements Supplier<String>
{
    private final JenkinsApi jenkins;

    public JenkinsVersionSupplier(JenkinsApi jenkins)
    {
        this.jenkins = jenkins;
    }

    @Override
    public String get()
    {
        JenkinsBuild build = jenkins.fetchLastSuccessfulBuild();
        if(build == null)   //jenkins failed fetching (internet or jenkins server?)
            return null;
        if(build.artifacts.size() > 0)
        {
            JenkinsBuild.Artifact firstArtifact = build.artifacts.values().iterator().next();
            return firstArtifact.fileNameParts.get(1);
        }
        return Integer.toString(build.buildNum);
    }
}
