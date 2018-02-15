package com.kantenkugel.discordbot.versioncheck.updatehandle;

import com.kantenkugel.discordbot.versioncheck.VersionedItem;

@FunctionalInterface
public interface UpdateHandler
{
    void onUpdate(VersionedItem item);
}
