package com.kantenkugel.discordbot.versioncheck.updatehandle;

import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;

@FunctionalInterface
public interface UpdateHandler
{
    void onUpdate(VersionedItem item);
}
