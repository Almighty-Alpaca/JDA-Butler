package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.versioncheck.GithubVersionSupplier;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.LongStream;

public class LavalinkItem extends VersionedItem
{
    private static final GithubVersionSupplier versionSupplier = new GithubVersionSupplier("Frederikam", "Lavalink");
    private static final List<String> aliases = Arrays.asList("ll", "link");

    private static final long[] announcerIds = {81011298891993088L, 166604053629894657L}; //Fred, Napster

    @Override
    public String getName()
    {
        return "Lavalink";
    }

    @Override
    public List<String> getAliases()
    {
        return aliases;
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
        return getVersion() == null ? null : "https://github.com/Frederikam/Lavalink/releases/tag/" + getVersion();
    }

    @Override
    public Supplier<String> getCustomVersionSupplier()
    {
        return versionSupplier;
    }

    @Override
    public long getAnnouncementRoleId()
    {
        return 471370097747820545L;
    }

    @Override
    public long getAnnouncementChannelId()
    {
        return 418817098278764544L;
    }

    @Override
    public boolean canAnnounce(User u)
    {
        return LongStream.of(announcerIds).anyMatch(announcer -> announcer == u.getIdLong());
    }
}
