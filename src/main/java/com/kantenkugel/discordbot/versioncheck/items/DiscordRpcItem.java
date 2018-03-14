package com.kantenkugel.discordbot.versioncheck.items;

import com.kantenkugel.discordbot.versioncheck.GithubVersionSupplier;
import com.kantenkugel.discordbot.versioncheck.RepoType;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DiscordRpcItem extends VersionedItem
{
    private static final GithubVersionSupplier versionSupplier = new GithubVersionSupplier("minndevelopment", "java-discord-rpc");
    private static final List<String> aliases = Collections.singletonList("rpc");

    @Override
    public String getName()
    {
        return "java-discord-rpc";
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
        return getVersion() == null ? null : "https://jitpack.io/#MinnDevelopment/java-discord-rpc/" + getVersion();
    }

    @Override
    public Supplier<String> getCustomVersionSupplier()
    {
        return versionSupplier;
    }
}
