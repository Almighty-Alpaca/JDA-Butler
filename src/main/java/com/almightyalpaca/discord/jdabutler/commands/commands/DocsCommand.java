package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Dispatcher;
import com.almightyalpaca.discord.jdabutler.commands.ReactionCommand;
import com.kantenkugel.discordbot.jdocparser.Documentation;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.jdocparser.JDocUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DocsCommand extends ReactionCommand
{
    private static final int RESULTS_PER_PAGE = 5;
    private static final String[] ALIASES = new String[]{"documentation", "doc", "jdoc", "jdocs"};

    public DocsCommand(Dispatcher.ReactionListenerRegistry registry)
    {
        super(registry);
    }

    private static Message getDocMessage(String jDocBase, Documentation documentation)
    {
        EmbedBuilder embed = getDefaultEmbed()
                .setTitle(documentation.getTitle(), documentation.getUrl(jDocBase));
        if(documentation.getFields() != null && documentation.getFields().get("Deprecated:") != null)
        {
            //element deprecated
            embed.setColor(Color.RED);
        }
        else if(documentation.getFields() != null && documentation.getFields().get("Incubating") != null)
        {
            //incubating
            embed.setColor(Color.orange);
        }
        if (documentation.getContent().length() > MessageEmbed.TEXT_MAX_LENGTH)
        {
            embed.appendDescription("Description too long. Please refer to [the online docs](" + documentation.getUrl(jDocBase) + ')');
            return new MessageBuilder().setEmbed(embed.build()).build();
        }
        if (documentation.getContent().length() == 0)
        {
            embed.appendDescription("No Description available.");
        }
        else
        {
            embed.appendDescription(documentation.getContent());
        }
        if (documentation.getFields() != null && documentation.getFields().size() > 0)
        {
            for (Map.Entry<String, List<String>> field : documentation.getFields().entrySet())
            {
                String fieldValue = String.join("\n", field.getValue());
                if (fieldValue.length() > MessageEmbed.VALUE_MAX_LENGTH)
                {
                    embed.addField(field.getKey(), "This section is too long. Please look at [the online docs](" + documentation.getUrl(jDocBase) + ')', false);
                }
                else
                {
                    embed.addField(field.getKey(), String.join("\n", field.getValue()), false);
                }
            }
        }
        return new MessageBuilder().setEmbed(embed.build()).build();
    }

    private void showPaginatorEmbed(TextChannel channel, User sender, String jDocBase, Set<Documentation> items)
    {
        AtomicInteger page = new AtomicInteger(0);
        List<Documentation> sorted = items.stream().sorted(Comparator.comparing(Documentation::getShortTitle)).collect(Collectors.toList());
        channel.sendMessage(getMultiResult(jDocBase, sorted, page.get()))
                .queue(m -> this.addReactions(
                        m,
                        Arrays.asList(ReactionCommand.LEFT_ARROW, ReactionCommand.RIGHT_ARROW, ReactionCommand.CANCEL),
                        Collections.singleton(sender),
                        3, TimeUnit.MINUTES,
                        index -> {
                            if (index >= 2)
                            {                //cancel button or other error
                                stopReactions(m, false);
                                m.delete().queue();
                                return;
                            }
                            int nextPage = page.updateAndGet(current -> index == 1 ? Math.min(current + 1, (sorted.size() - 1) / RESULTS_PER_PAGE) : Math.max(current - 1, 0));
                            m.editMessage(getMultiResult(JDocUtil.JDOCBASE, sorted, nextPage)).queue();
                        }));
    }

    private void showRefinementEmbed(TextChannel channel, User sender, String jDocBase, List<Documentation> docs)
    {
        EmbedBuilder embedB = getDefaultEmbed().setTitle("Refine your Search");
        for (int i = 0; i < docs.size(); i++)
        {
            Documentation doc = docs.get(i);
            embedB.appendDescription(
                    ReactionCommand.NUMBERS[i] + " [" + doc.getShortTitle() + "](" + doc.getUrl(jDocBase) + ")\n"
            );
        }
        embedB.getDescriptionBuilder().setLength(embedB.getDescriptionBuilder().length() - 1);
        List<String> options = Arrays.stream(ReactionCommand.NUMBERS).limit(docs.size()).collect(Collectors.toList());
        options.add(ReactionCommand.CANCEL);
        channel
                .sendMessage(embedB.build())
                .queue(m -> this.addReactions(m, options, Collections.singleton(sender), 30, TimeUnit.SECONDS, index -> {
                    if (index >= docs.size())
                    {                //cancel button or other error
                        stopReactions(m, false);
                        m.delete().queue();
                        return;
                    }
                    stopReactions(m);
                    m.editMessage(getDocMessage(jDocBase, docs.get(index))).queue();
                }));
    }

    private static Message getMultiResult(String jDocBase, List<Documentation> search, int page)
    {
        EmbedBuilder embed = getDefaultEmbed()
                .setTitle("Found " + search.size() + " Results. Page " + (page + 1) + '/' + ((search.size() - 1) / RESULTS_PER_PAGE + 1));
        for (int index = page * RESULTS_PER_PAGE; index < search.size() && index < (page + 1) * RESULTS_PER_PAGE; index++)
        {
            Documentation documentation = search.get(index);
            embed.appendDescription('[' + documentation.getShortTitle() + "](" + documentation.getUrl(jDocBase) + ")\n");
        }
        return new MessageBuilder().setEmbed(embed.build()).build();
    }

    private static EmbedBuilder getDefaultEmbed()
    {
        return new EmbedBuilder().setAuthor("JDA Javadocs", null, EmbedUtil.JDA_ICON).setColor(EmbedUtil.COLOR_JDA_PURPLE);
    }

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if (content.trim().isEmpty())
        {
            reply(event, "See the docs here: " + JDocUtil.JDOCBASE);
            return;
        }
        if (content.trim().equalsIgnoreCase("help"))
        {
            reply(event, "Prints out JDA documentation.\n" +
                    "Syntax: `"+Bot.config.getString("prefix")+"docs [term | search:[params:]term | java:term | help]`.\n" +
                    "While not in special mode, `term` is a class name or a class-prefixed variable or method name (for example `JDA#getUserById`, `RestAction.queue()`).\n" +
                    "Both `.` and `#` can be used to specify inner classes, methods and variables.\n" +
                    "Omitting the method parentheses will print all available methods with given name. " +
                    "When specified (with parameter types), only the specific one is returned (`RestAction#queue` vs `RestAction.queue(Consumer)`).\n\n" +
                    "When in `search` mode, `.` and `#` won't work and all documentations that **contain** `term` in their name/signature are returned.\n" +
                    "Search parameters can be used to restrict the search:\n" +
                    "`f` to only search for methods\n" +
                    "`var` to only search for variables\n" +
                    "`c` to only search for classes\n" +
                    "`cs` to make matching case-sensitive\n\n" +
                    "When in `java` mode, java 8 docs are searched instead. Syntax for `term` is the same as without mode."
            );
            return;
        }
        if (content.contains(":"))
        {
            String[] split = content.split(":", 4);
            if (split.length > 3)
            {
                reply(event, "Invalid syntax!");
                return;
            }
            switch (split[0].toLowerCase())
            {
                case "search":
                {
                    String[] options = split.length == 3 ? split[1].toLowerCase().split("\\s*,\\s*") : new String[0];
                    Set<Documentation> search = JDoc.search(split[split.length - 1], options);
                    if (search.size() == 0)
                    {
                        reply(event, "Did not find anything matching query!");
                        return;
                    }
                    if (search.size() > RESULTS_PER_PAGE)
                    {
                        showPaginatorEmbed(channel, sender, JDocUtil.JDOCBASE, search);
                    }
                    else
                    {
                        //show all items, but without emoji selector (search only)
                        EmbedBuilder embedB = getDefaultEmbed().setTitle("Found following:");
                        for (Documentation documentation : search)
                        {
                            embedB.appendDescription('[' + documentation.getShortTitle() + "](" + documentation.getUrl(JDocUtil.JDOCBASE) + ")\n");
                        }
                        embedB.getDescriptionBuilder().setLength(embedB.getDescriptionBuilder().length() - 1);
                        reply(event, embedB.build());
                    }
                    break;
                }
                case "java":
                {
                    if (split.length != 2)
                    {
                        reply(event, "Invalid syntax!");
                        return;
                    }

                    List<Documentation> javadocs = JDoc.getJava(split[1]);
                    switch (javadocs.size())
                    {
                        case 0:
                            reply(event, "No Result found!");
                            break;
                        case 1:
                            reply(event, getDocMessage(JDocUtil.JAVA_JDOCS_PREFIX, javadocs.get(0)));
                            break;
                        default:
                            showRefinementEmbed(channel, sender, JDocUtil.JAVA_JDOCS_PREFIX, javadocs);
                            break;
                    }
                    break;
                }
                default:
                    reply(event, "Unsupported operation " + split[0]);
                    break;
            }
            return;
        }
        //no : found -> normal lookup
        final List<Documentation> docs = JDoc.get(content);
        switch (docs.size())
        {
            case 0:
                reply(event, "No Result found!");
                break;
            case 1:
                reply(event, getDocMessage(JDocUtil.JDOCBASE, docs.get(0)));
                break;
            default:
                showRefinementEmbed(channel, sender, JDocUtil.JDOCBASE, docs);
                break;
        }
    }

    @Override
    public String getHelp()
    {
        return "Displays documentation. Use `" + Bot.config.getString("prefix") + "docs help` for more help";
    }

    @Override
    public String getName()
    {
        return "docs";
    }

    @Override
    public String[] getAliases()
    {
        return DocsCommand.ALIASES;
    }
}
