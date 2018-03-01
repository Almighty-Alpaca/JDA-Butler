package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.User;

import java.util.Arrays;
import java.util.List;

/**
 * Simple wrapper class for {@link VersionedItem}.
 * <br/>This only supports maven artifacts and doesn't support advanced features like update handler, changelogs or custom versions.
 * For these, please directly extend {@link VersionedItem}.
 */
public class SimpleVersionedItem extends VersionedItem
{
    private final String name;
    private List<String> aliases;

    private final RepoType repoType;
    private final DependencyType depType;
    private final String groupId;
    private final String artifactId;

    private String url;
    private long roleId = 0;
    private long channelId = 0;
    private final TLongSet allowedAnnouncers = new TLongHashSet();

    public SimpleVersionedItem(String name, RepoType repoType, DependencyType depType, String groupId, String artifactId)
    {
        this.name = name;
        this.repoType = repoType;
        this.depType = depType;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public SimpleVersionedItem setAliases(String... aliases)
    {
        this.aliases = Arrays.asList(aliases);
        return this;
    }

    public SimpleVersionedItem setUrl(String url)
    {
        this.url = url;
        return this;
    }

    public SimpleVersionedItem setAnnouncementRoleId(long roleId)
    {
        this.roleId = roleId;
        return this;
    }

    public SimpleVersionedItem setAnnouncementChannelId(long channelId)
    {
        this.channelId = channelId;
        return this;
    }

    public SimpleVersionedItem addAnnouncementWhitelist(long... userIds)
    {
        allowedAnnouncers.addAll(userIds);
        return this;
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

    @Override
    public long getAnnouncementRoleId()
    {
        return roleId;
    }

    @Override
    public long getAnnouncementChannelId()
    {
        return channelId;
    }

    @Override
    public boolean canAnnounce(User u)
    {
        return allowedAnnouncers.contains(u.getIdLong());
    }
}
