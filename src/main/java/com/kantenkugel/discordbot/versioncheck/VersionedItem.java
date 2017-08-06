package com.kantenkugel.discordbot.versioncheck;

import java.util.Objects;

public class VersionedItem
{
    private final String name;

    private final RepoType repoType;
    private final String groupId;
    private final String artifactId;

    private String version;

    private String url;

    public VersionedItem(String name, RepoType repoType, String groupId, String artifactId)
    {
        this.name = name;
        this.repoType = repoType;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public VersionedItem(String name, RepoType repoType, String groupId, String artifactId, String url)
    {
        this(name, repoType, groupId, artifactId);
        this.url = url;
    }

    public VersionedItem(String name, RepoType repoType, String groupId, String artifactId, String url, String version)
    {
        this(name, repoType, groupId, artifactId, url);
        this.version = version;
    }

    public String getRepoUrl()
    {
        return String.format("%s%s/%s/maven-metadata.xml", repoType.getRepoBase(), groupId.replace('.', '/'), artifactId);
    }

    public String getName()
    {
        return name;
    }

    public RepoType getRepoType()
    {
        return repoType;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    void setVersion(String version)
    {
        this.version = version;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(repoType, groupId, artifactId);
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null || !(obj instanceof VersionedItem))
            return false;
        VersionedItem other = (VersionedItem) obj;
        return other.repoType == this.repoType && other.groupId.equals(this.groupId) && other.artifactId.equals(this.artifactId);
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s:%s", groupId, artifactId, version == null ? "Unversioned" : version);
    }
}
