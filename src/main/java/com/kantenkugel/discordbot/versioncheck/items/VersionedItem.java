package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.versioncheck.VersionUtils;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.RepoType;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class VersionedItem
{
    public abstract String getName();

    public List<String> getAliases()
    {
        return null;
    }

    public abstract RepoType getRepoType();

    public abstract String getGroupId();

    public abstract String getArtifactId();

    public DependencyType getDependencyType()
    {
        return DependencyType.DEFAULT;
    }

    public String getUrl()
    {
        return null;
    }

    public Consumer<VersionedItem> getUpdateHandler()
    {
        return null;
    }

    public Supplier<String> getCustomVersionSupplier()
    {
        return null;
    }

    public ChangelogProvider getChangelogProvider()
    {
        return null;
    }

    public VersionUtils.VersionSplits parseVersion()
    {
        String version = getVersion();
        if(version == null)
            throw new IllegalStateException("No version fetched so far");
        return VersionUtils.parseVersion(version);
    }

    public String getRepoUrl()
    {
        return String.format("%s%s/%s/maven-metadata.xml", getRepoType().getRepoBase(),
                getGroupId().replace('.', '/'), getArtifactId());
    }


    private String version = null;

    public final String getVersion()
    {
        return version;
    }

    public final void setVersion(String version)
    {
        this.version = version;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getRepoType(), getGroupId(), getArtifactId());
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == null || !(obj instanceof VersionedItem))
            return false;
        VersionedItem other = (VersionedItem) obj;
        return other.getRepoType() == this.getRepoType()
                && other.getGroupId().equals(this.getGroupId())
                && other.getArtifactId().equals(this.getArtifactId());
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s:%s", getGroupId(), getArtifactId(), version == null ? "Unversioned" : version);
    }
}
