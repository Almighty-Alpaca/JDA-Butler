package com.kantenkugel.discordbot.versioncheck.items;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.UpdateHandler;
import com.kantenkugel.discordbot.versioncheck.VersionUtils;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.*;
import java.util.function.Supplier;

public abstract class VersionedItem
{
    /**
     * The name of this item. Used to display this item in various command callbacks.
     * <br/> Should be human readable and capitalized as needed. Will be cast to lowercase for the registry
     *
     * @return  The name of the item
     */
    public abstract String getName();

    /**
     * The description of this version item, used for listing purposes.
     *
     * @return  The description
     */
    public String getDescription() { throw new UnsupportedOperationException(); }

    /**
     * If available, this is used in the registry lookup methods to get this item via alternative names.
     * Should be collision free with other items.
     *
     * @return  Null-able list of aliases of this item.
     */
    public List<String> getAliases()
    {
        return null;
    }

    /**
     * The maven repository type of this item (maven artifact).
     * <br/>Used for automated version retrieval from maven repository.
     *
     * <p><b>Note:</b> This may only be {@code null}, if {@link #getCustomVersionSupplier()} is used.
     *
     * @return  The repository type of this maven artifact
     */
    public abstract RepoType getRepoType();

    /**
     * All additional repositories needed to fully resolve this dependency, including transitive dependencies.
     * <br/>Used for the maven/gradle commands.
     *
     * <p><b>Note:</b> This may never be {@code null}.
     *
     * @return  The extra repositories needed for this maven artifact
     */
    public Set<RepoType> getAdditionalRepositories() {
        return EnumSet.noneOf(RepoType.class);
    }

    /**
     * Helper method that combines all Elements off {@link #getAdditionalRepositories()} with {@link #getRepoType()}
     *
     * @return Set of all relevant Repositories
     */
    public final Set<RepoType> getAllRepositories() {
        Set<RepoType> repoTypes = new HashSet<>(getAdditionalRepositories());
        if(getRepoType() != null) {
            repoTypes.add(getRepoType());
        }
        return repoTypes;
    }

    /**
     * The group id of this item (maven artifact).
     * <br/>Used for automated version retrieval from maven repository.
     *
     * <p><b>Note:</b> This may only be {@code null}, if {@link #getCustomVersionSupplier()} is used.
     *
     * @return  The group id of this maven artifact
     */
    public abstract String getGroupId();

    /**
     * The artifact id of this item (maven artifact).
     * <br/>Used for automated version retrieval from maven repository.
     *
     * <p><b>Note:</b> This may only be {@code null}, if {@link #getCustomVersionSupplier()} is used.
     *
     * @return  The artifact id of this maven artifact
     */
    public abstract String getArtifactId();

    /**
     * The (maven) dependency type of this artifact.
     * This is used in the maven commands to set the {@literal <type>} tag (if not DEFAULT)
     * as maven can't properly use the type given via pom.
     *
     * @return  The artifact dependency type
     */
    public DependencyType getDependencyType()
    {
        return DependencyType.DEFAULT;
    }

    /**
     * This url, if provided is used in embeds to link to this items main website.
     * <br>Best values for this are personal websites, github repo or build servers for example.
     *
     * @return  Null-able main url of this item
     */
    public String getUrl()
    {
        return null;
    }

    /**
     * If this method returns a non-zero Role ID,
     * this item integrates into the !notify command to toggle an announcement role.
     *
     * @return  Role ID of the announcement role, or {@code 0} (zero) if unused
     */
    public long getAnnouncementRoleId()
    {
        return 0;
    }

    /**
     * If this method returns a non-zero TextChannel ID,
     * this item integrates into the !announce command for custom announcements.
     *
     * <p>If {@link #getAnnouncementRoleId()} returns {@code 0}, then this is ignored,
     * as announcements require a role.
     *
     * @return  TextChannel ID of the announcement channel, or {@code 0} (zero) if unused
     */
    public long getAnnouncementChannelId()
    {
        return 0;
    }

    /**
     * The channel used for the !notify command.
     * <br>If this channel is a library channel such as #lavalink or #lavaplayer the notify command will default
     * to the notify roles of that channel instead.
     *
     * <p>If {@link #getAnnouncementRoleId()} returns {@code 0}, then this is ignored,
     * as announcements require a role.
     *
     * @return  TextChannel ID of the notify channel, or {@code 0} (zero) if unused
     */
    public long getNotifyChannelId()
    {
        return getAnnouncementChannelId();
    }

    /**
     * This method is used to determine if some specific User can use announcements for this item (!announce command).
     * Only used, when {@link #getAnnouncementRoleId()} and {@link #getAnnouncementChannelId()} are returning non-zero.
     *
     * <p>Note: JDA Staff can always use the announcement command and do therefore not require whitelisting through this method.
     *
     * @param u
     *          The User which wants to make an announcement for this item
     * @return  True, if the given User is allowed to make an announcement for this item
     */
    public boolean canAnnounce(User u)
    {
        return false;
    }

    /**
     * Hook to run custom code once a new version of this item is detected by JDA-Butler.
     *
     * @return  Null-able custom update handler
     */
    public UpdateHandler getUpdateHandler()
    {
        return null;
    }

    /**
     * Hook to run custom code to retrieve the latest version. This is periodically called by the update-checker code.
     * <br/>If this is provided, all maven related methods are ignored
     * and this item will not be available for maven/gradle commands.
     *
     * <p><b>Return type of Supplier:</b>
     * <br/>Null-able version String indicating current version or failure if {@code null}
     *
     * @return  Null-able custom version supplier
     */
    public Supplier<String> getCustomVersionSupplier()
    {
        return null;
    }

    /**
     * Hook to enable changelogs for this item. This is used in the !changelog command
     *
     * @return  Null-able ChangelogProvider to use
     */
    public ChangelogProvider getChangelogProvider()
    {
        return null;
    }

    /**
     * Parses this item's version to {@link com.kantenkugel.discordbot.versioncheck.VersionUtils.VersionSplits}.
     * <br/>The default implementation uses {@link VersionUtils#parseVersion(String)} to parse this version.
     * This does not work for versions that are not in the extended SemVer format (as described in {@link VersionUtils#parseVersion(String)})
     * <br/>This is currently not used anywhere but should still be correctly implemented if not extended SemVer is available.
     *
     * @return  VersionSplits object reflecting this items version
     */
    public VersionUtils.VersionSplits parseVersion()
    {
        String version = getVersion();
        if(version == null)
            throw new IllegalStateException("No version fetched so far");
        return VersionUtils.parseVersion(version);
    }

    /**
     * The url to this item's {@code maven-metadata.xml} document. This is used in the version-check routine.
     * <br/>The default implementation should already work for every standard maven repository and should not be changed.
     *
     * @return  The url to this items maven-metadata.xml file
     */
    public String getRepoUrl()
    {
        return String.format("%s/%s/%s/maven-metadata.xml", getRepoType().getRepoBase(),
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

    public final Role getAnnouncementRole()
    {
        long rid = getAnnouncementRoleId();
        return rid == 0 ? null : Bot.getGuildJda().getRoleById(rid);
    }

    public final TextChannel getAnnouncementChannel()
    {
        long cid = getAnnouncementChannelId();
        return cid == 0 ? null : Bot.getGuildJda().getTextChannelById(cid);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getRepoType(), getGroupId(), getArtifactId());
    }

    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof VersionedItem))
            return false;
        VersionedItem other = (VersionedItem) obj;
        return other.getName().equals(this.getName());
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s:%s:%s)", getName(), getGroupId(), getArtifactId(), version == null ? "Unversioned" : version);
    }
}
