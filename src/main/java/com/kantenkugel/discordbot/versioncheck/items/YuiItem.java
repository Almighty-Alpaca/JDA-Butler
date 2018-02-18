package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.versioncheck.JenkinsVersionSupplier;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.changelog.JenkinsChangelogProvider;

import java.util.function.Supplier;

public class YuiItem extends VersionedItem
{
    private final JenkinsApi jenkins = JenkinsApi.forConfig("http://home.dv8tion.net:8080", "Yui");
    private final Supplier<String> versionSupplier = new JenkinsVersionSupplier(jenkins);
    private final ChangelogProvider clProvider = new JenkinsChangelogProvider(jenkins);

    @Override
    public String getName()
    {
        return "Yui";
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
    public String getUrl()
    {
        return "https://github.com/DV8FromTheWorld/Yui";
    }

    @Override
    public Supplier<String> getCustomVersionSupplier()
    {
        return versionSupplier;
    }

    @Override
    public ChangelogProvider getChangelogProvider()
    {
        return clProvider;
    }
}
