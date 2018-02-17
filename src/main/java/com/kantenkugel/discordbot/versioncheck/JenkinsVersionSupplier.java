package com.kantenkugel.discordbot.versioncheck;

import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;

import java.util.function.Supplier;

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
        if(build.artifacts.size() > 0)
        {
            JenkinsBuild.Artifact firstArtifact = build.artifacts.values().iterator().next();
            return firstArtifact.fileNameParts.get(1);
        }
        return Integer.toString(build.buildNum);
    }
}
