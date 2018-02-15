package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.updatehandle.UpdateHandler;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public UpdateHandler getUpdateHandler()
    {
        return null;
    }

    public VersionSplits parseVersion()
    {
        String version = getVersion();
        if(version == null)
            throw new IllegalStateException("No version fetched so far");
        return VersionSplits.parse(version);
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

    public static class VersionSplits
    {
        //major is mandatory, others default to 0
        public final int major, minor, patch, build;
        //default to null if not present
        public final String preReleaseInfo, metaData;

        protected VersionSplits(int major, int minor, int patch, int build, String preReleaseInfo, String metaData)
        {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.build = build;
            this.preReleaseInfo = preReleaseInfo;
            this.metaData = metaData;
        }

        //major(mandatory), minor, patch, build, preRelease, metaData
        public static final Pattern EXTENDED_SEMVER_PATTERN =
                Pattern.compile("(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:_(\\d+))?" +
                        "(?:-([A-Za-z0-9-.]+))?(?:\\+([A-Za-z0-9-.]+))?");

        public static VersionedItem.VersionSplits parse(String versionString)
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
}
