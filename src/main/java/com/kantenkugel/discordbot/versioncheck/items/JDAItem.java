package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.updatehandle.JDAUpdateHandler;
import com.kantenkugel.discordbot.versioncheck.updatehandle.UpdateHandler;

public class JDAItem extends VersionedItem
{
    private final JDAUpdateHandler updateHandler = new JDAUpdateHandler();

    @Override
    public String getName()
    {
        return "JDA";
    }

    @Override
    public RepoType getRepoType()
    {
        return RepoType.JCENTER;
    }

    @Override
    public String getGroupId()
    {
        return "net.dv8tion";
    }

    @Override
    public String getArtifactId()
    {
        return "JDA";
    }

    @Override
    public String getUrl()
    {
        return JenkinsApi.JDA_JENKINS.getLastSuccessfulBuildUrl();
    }

    @Override
    public UpdateHandler getUpdateHandler()
    {
        return updateHandler;
    }
}
