package com.kantenkugel.discordbot.versioncheck;

import com.kantenkugel.discordbot.versioncheck.updatehandle.UpdateHandler;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionedItem
{
    private final String name;

    private final RepoType repoType;
    private final DependencyType depType;
    private final String groupId;
    private final String artifactId;

    private String version;

    private String url;
    private UpdateHandler updateHandler;

    public VersionedItem(String name, RepoType repoType, DependencyType depType, String groupId, String artifactId)
    {
        this.name = name;
        this.repoType = repoType;
        this.depType = depType;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public VersionedItem(String name, RepoType repoType, DependencyType depType, String groupId, String artifactId, String url)
    {
        this(name, repoType, depType, groupId, artifactId);
        this.url = url;
    }

    public VersionedItem(String name, RepoType repoType, DependencyType depType, String groupId, String artifactId, UpdateHandler updateHandler)
    {
        this(name, repoType, depType, groupId, artifactId);
        this.updateHandler = updateHandler;
    }

    public VersionedItem(String name, RepoType repoType, DependencyType depType, String groupId, String artifactId, String url, UpdateHandler updateHandler)
    {
        this(name, repoType, depType, groupId, artifactId, url);
        this.updateHandler = updateHandler;
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

    public DependencyType getDependencyType()
    {
        return depType;
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

    public UpdateHandler getUpdateHandler()
    {
        return updateHandler;
    }

    public VersionSplits parseVersion()
    {
        String version = getVersion();
        if(version == null)
            throw new IllegalStateException("No version fetched so far");
        return VersionSplits.parse(version);
    }

    public static class VersionSplits
    {
        //major is mandatory, others default to 0
        public final int major, minor, patch, build;
        //default to null if not present
        public final String preReleaseInfo, metaData;

        private VersionSplits(int major, int minor, int patch, int build, String preReleaseInfo, String metaData)
        {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.build = build;
            this.preReleaseInfo = preReleaseInfo;
            this.metaData = metaData;
        }

        //major(mandatory), minor, patch, build, preRelease, metaData
        private static final Pattern EXTENDED_SEMVER_PATTERN =
                Pattern.compile("(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:_(\\d+))?" +
                        "(?:-([A-Za-z0-9-.]+))?(?:\\+([A-Za-z0-9-.]+))?");

        private static VersionSplits parse(String versionString)
        {
            Matcher matcher = EXTENDED_SEMVER_PATTERN.matcher(versionString);
            if(!matcher.matches())
                throw new IllegalArgumentException("Given version string is not extended semver");
            int major = Integer.parseInt(matcher.group(1));
            int minor = matcher.group(2).isEmpty() ? 0 : Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3).isEmpty() ? 0 : Integer.parseInt(matcher.group(3));
            int build = matcher.group(4).isEmpty() ? 0 : Integer.parseInt(matcher.group(4));
            String preReleaseInfo = matcher.group(5).isEmpty() ? null : matcher.group(5);
            String metaData = matcher.group(6).isEmpty() ? null : matcher.group(6);

            return new VersionSplits(major, minor, patch, build, preReleaseInfo, metaData);
        }
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
