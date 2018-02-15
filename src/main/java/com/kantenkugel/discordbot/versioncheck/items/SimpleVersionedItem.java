package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.RepoType;

import java.util.List;

public class SimpleVersionedItem extends VersionedItem
{
    private final String name;
    private final List<String> aliases;

    private final RepoType repoType;
    private final DependencyType depType;
    private final String groupId;
    private final String artifactId;

    private String url;

    public SimpleVersionedItem(String name, RepoType repoType, DependencyType depType, String groupId, String artifactId, List<String> aliases)
    {
        this.name = name;
        this.aliases = aliases;
        this.repoType = repoType;
        this.depType = depType;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public SimpleVersionedItem(String name, RepoType repoType, DependencyType depType, String groupId, String artifactId, String url, List<String> aliases)
    {
        this(name, repoType, depType, groupId, artifactId, aliases);
        this.url = url;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public List<String> getAliases()
    {
        return aliases;
    }

    @Override
    public RepoType getRepoType()
    {
        return repoType;
    }

    @Override
    public DependencyType getDependencyType()
    {
        return depType;
    }

    @Override
    public String getGroupId()
    {
        return groupId;
    }

    @Override
    public String getArtifactId()
    {
        return artifactId;
    }

    @Override
    public String getUrl()
    {
        return url;
    }
}
