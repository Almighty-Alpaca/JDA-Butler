package com.kantenkugel.discordbot.versioncheck;

import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;

/**
 * Class used by {@link VersionedItem#getUpdateHandler()} to make custom update handlers available.
 */
@FunctionalInterface
public interface UpdateHandler
{
    /**
     * This method is called, when a new update is detected,
     * <b>or</b> this item gets its version retrieved for the first time.
     *
     * @param item
     *          This is the reference to the VersionedItem for which the update was found.
     * @param previousVersion
     *          The previous version value prior to update.
     *          This might be {@code null}, if the Bot just started up and fetched the version for the first time!
     * @param shouldAnnounce
     *          Boolean indicating if this update loop should trigger any announcement side-effects.
     *          This is for example false, if the Bot is running in testing mode rather than production.
     */
    void onUpdate(VersionedItem item, String previousVersion, boolean shouldAnnounce);
}
