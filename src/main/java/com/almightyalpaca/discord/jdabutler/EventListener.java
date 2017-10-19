package com.almightyalpaca.discord.jdabutler;

import com.almightyalpaca.discord.jdabutler.eval.Engine;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.versioncheck.VersionChecker;
import com.kantenkugel.discordbot.versioncheck.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EventListener extends ListenerAdapter
{

    private static boolean started;

    static ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, (r) ->
    {
        final Thread t = new Thread(r);
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((final Thread thread, final Throwable throwable) ->
        {
            throwable.printStackTrace();
        });
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });

    public static void start()
    {

        if (!EventListener.started)
        {
            EventListener.started = true;

            EventListener.executor.scheduleAtFixedRate(() ->
            {
                try
                {
                    Bot.LOG.debug("Checking for updates...");

                    Set<VersionedItem> changedItems = VersionChecker.checkVersions();

                    for (VersionedItem changedItem : changedItems)
                    {
                        switch (changedItem.getName())
                        {
                            case "JDA":
                                final int build = Integer.parseInt(changedItem.getVersion().split("_")[1]);
                                if (build != Bot.config.getInt("jda.version.build", -1))
                                {
                                    Bot.LOG.debug("Update found!");

                                    Bot.config.put("jda.version.build", build);
                                    Bot.config.put("jda.version.name", changedItem.getVersion());

                                    Bot.config.save();

                                    JenkinsBuild jenkinsBuild = JenkinsApi.fetchLastSuccessfulBuild();

                                    if(jenkinsBuild == null)
                                    {
                                        Bot.LOG.warn("Could not fetch Jenkins-build for new version (triggered by maven update)");
                                        return;
                                    }

                                    EventListener.executor.submit(() ->
                                    {
                                        JDoc.reFetch();
                                        GradleProjectDropboxUploader.uploadProject();
                                    });

                                    if (Bot.isStealth)
                                    {
                                        Bot.LOG.debug("Skipping announcement!");
                                        return;
                                    }

                                    final EmbedBuilder eb = new EmbedBuilder();

                                    final MessageBuilder mb = new MessageBuilder();

                                    FormattingUtil.setFooter(eb, jenkinsBuild.culprits, jenkinsBuild.buildTime);

                                    mb.append(Bot.getRoleJdaUpdates());

                                    eb.setAuthor("JDA version " + changedItem.getVersion() + " has been released\n", JenkinsApi.JENKINS_BASE + build, EmbedUtil.JDA_ICON);

                                    EmbedUtil.setColor(eb);

                                    if (jenkinsBuild.changes.size() > 0)
                                    {

                                        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

                                        final List<String> changelog = FormattingUtil.getChangelog(jenkinsBuild.changes);

                                        int fields;

                                        if (changelog.size() > 25)
                                            fields = 24;
                                        else
                                            fields = Math.min(changelog.size(), 25);

                                        for (int j = 0; j < fields; j++)
                                        {
                                            final String field = changelog.get(j);
                                            eb.addField(j == 0 ? "Commits:" : "", field, false);
                                        }

                                        if (changelog.size() > 25)
                                            eb.addField("", "max embed length reached", false);

                                    }

                                    final MessageEmbed embed = eb.build();

                                    mb.setEmbed(embed);

                                    mb.build();

                                    final Role role = Bot.getRoleJdaUpdates();
                                    final TextChannel channel = Bot.getChannelAnnouncements();

                                    role.getManager().setMentionable(true).queue(s -> channel.sendMessage(mb.build()).queue(m -> role.getManager().setMentionable(false).queue()));
                                }
                                break;
                            //Custom handling for other updates (eg announce)
                        }
                    }

                }
                catch (final Exception e)
                {
                    Bot.LOG.fatal("Checking updates errored");
                    Bot.LOG.fatal(e);
                }
            }, 0, 30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event)
    {
        final Guild guild = event.getGuild();
        if (guild.getId().equals("125227483518861312"))
        {
            final Member member = event.getMember();
            final User user = member.getUser();
            Role role;
            if (user.isBot())
                role = Bot.getRoleBots();
            else
                role = Bot.getRoleJdaFanclub();

            final AuditableRestAction<Void> action = guild.getController().addSingleRoleToMember(member, role).reason("Auto Role");
            final String message = String.format("Added %#s (%d) to %s", user, user.getIdLong(), role.getName());
            action.queue(v -> Bot.LOG.warn(message), Bot.LOG::fatal);
        }
    }

    @Override
    public void onShutdown(final ShutdownEvent event)
    {
        EventListener.executor.shutdown();
        Engine.shutdown();
    }
}
