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
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;

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
        if(documentation.getFields() != null && documentation.getFields().get("Deprecated") != null)
        {
            //element deprecated
            embed.setColor(Color.RED).setDescription("**DEPRECATED**\n");
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
                    embed.addField(field.getKey(), field.getValue().stream().collect(Collectors.joining("\n")), false);
                }
            }
        }
        return new MessageBuilder().setEmbed(embed.build()).build();
    }

    private static Message getMultiResult(String jDocBase, List<Pair<String, ? extends Documentation>> search, int page)
    {
        EmbedBuilder embed = getDefaultEmbed()
                .setTitle("Found " + search.size() + " Results. Page " + (page + 1) + '/' + ((search.size() - 1) / RESULTS_PER_PAGE + 1));
        for (int index = page * RESULTS_PER_PAGE; index < search.size() && index < (page + 1) * RESULTS_PER_PAGE; index++)
        {
            Pair<String, ? extends Documentation> pair = search.get(index);
            embed.appendDescription('[' + pair.getKey() + "](" + pair.getValue().getUrl(jDocBase) + ")\n");
        }
        return new MessageBuilder().setEmbed(embed.build()).build();
    }

    private static EmbedBuilder getDefaultEmbed()
    {
        return new EmbedBuilder().setAuthor("JDA Javadocs", null, EmbedUtil.JDA_ICON).setColor(EmbedUtil.COLOR_JDA_PRUPLE);
    }

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if (content.trim().isEmpty())
        {
            channel.sendMessage(new MessageBuilder().append("See the docs here: ").append(JDocUtil.JDOCBASE).build()).queue();
            return;
        }
        if (content.trim().equalsIgnoreCase("help"))
        {
            channel.sendMessage("Prints out JDA documentation.\n" +
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
            ).queue();
            return;
        }
        if (content.contains(":"))
        {
            String[] split = content.split(":", 4);
            if (split.length > 3)
            {
                channel.sendMessage("Invalid syntax!").queue();
                return;
            }
            switch (split[0].toLowerCase())
            {
                case "search":
                {
                    String[] options = split.length == 3 ? split[1].toLowerCase().split("\\s*,\\s*") : new String[0];
                    Set<Pair<String, ? extends Documentation>> search = JDoc.search(split[split.length - 1], options);
                    if (search.size() == 0)
                    {
                        channel.sendMessage("Did not find anything matching query!").queue();
                        return;
                    }
                    if (search.size() > RESULTS_PER_PAGE)
                    {
                        AtomicInteger page = new AtomicInteger(0);
                        List<Pair<String, ? extends Documentation>> sorted = search.stream().sorted(Comparator.comparing(Pair::getKey)).collect(Collectors.toList());
                        channel.sendMessage(getMultiResult(JDocUtil.JDOCBASE, sorted, page.get())).queue(m -> this.addReactions(m, Arrays.asList(ReactionCommand.LEFT_ARROW, ReactionCommand.RIGHT_ARROW, ReactionCommand.CANCEL), Collections.singleton(sender), 3, TimeUnit.MINUTES, index -> {
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
                    else
                    {
                        EmbedBuilder embedB = getDefaultEmbed().setTitle("Found following:");
                        for (Pair<String, ? extends Documentation> pair : search)
                        {
                            embedB.appendDescription('[' + pair.getKey() + "](" + pair.getValue().getUrl(JDocUtil.JDOCBASE) + ")\n");
                        }
                        embedB.getDescriptionBuilder().setLength(embedB.getDescriptionBuilder().length() - 1);
                        channel.sendMessage(embedB.build()).queue();
                    }
                    break;
                }
                case "java":
                {
                    if (split.length != 2)
                    {
                        channel.sendMessage("Invalid syntax!").queue();
                        return;
                    }

                    List<Documentation> javadocs = JDoc.getJava(split[1]);
                    switch (javadocs.size())
                    {
                        case 0:
                            channel.sendMessage("No Result found!").queue();
                            break;
                        case 1:
                            channel.sendMessage(getDocMessage(JDocUtil.JAVA_JDOCS_PREFIX, javadocs.get(0))).queue();
                            break;
                        default:
                            EmbedBuilder embedB = getDefaultEmbed().setTitle("Refine your Search");
                            for (int i = 0; i < javadocs.size(); i++)
                            {
                                Documentation doc = javadocs.get(i);
                                embedB.appendDescription(
                                        ReactionCommand.NUMBERS[i] + " [" + doc.getTitle() + "](" + doc.getUrl(JDocUtil.JAVA_JDOCS_PREFIX) +
                                        ")\n");
                            }
                            embedB.getDescriptionBuilder().setLength(embedB.getDescriptionBuilder().length() - 1);
                            List<String> options = new ArrayList<>(Arrays.asList(Arrays.copyOf(ReactionCommand.NUMBERS, javadocs.size())));
                            options.add(ReactionCommand.CANCEL);
                            channel
                                    .sendMessage(embedB.build())
                                    .queue(m -> this.addReactions(m, options, Collections.singleton(sender), 30, TimeUnit.SECONDS, index -> {
                                        if (index >= javadocs.size())
                                        {                //cancel button or other error
                                            stopReactions(m, false);
                                            m.delete().queue();
                                            return;
                                        }
                                        stopReactions(m);
                                        m.editMessage(getDocMessage(JDocUtil.JAVA_JDOCS_PREFIX, javadocs.get(index))).queue();
                                    }));
                            break;
                    }
                    break;
                }
                default:
                    channel.sendMessage("Unsupported operation " + split[0]).queue();
                    break;
            }
            return;
        }
        final List<Documentation> docs = JDoc.get(content);
        switch (docs.size())
        {
            case 0:
                channel.sendMessage("No Result found!").queue();
                break;
            case 1:
                channel.sendMessage(getDocMessage(JDocUtil.JDOCBASE, docs.get(0))).queue();
                break;
            default:
                EmbedBuilder embedB = getDefaultEmbed().setTitle("Refine your Search");
                for (int i = 0; i < docs.size(); i++)
                {
                    Documentation doc = docs.get(i);
                    embedB.appendDescription(ReactionCommand.NUMBERS[i] + " [" + doc.getTitle() + "](" + doc.getUrl(JDocUtil.JDOCBASE) + ")\n");
                }
                embedB.getDescriptionBuilder().setLength(embedB.getDescriptionBuilder().length() - 1);
                List<String> options = new ArrayList<>(Arrays.asList(Arrays.copyOf(ReactionCommand.NUMBERS, docs.size())));
                options.add(ReactionCommand.CANCEL);
                channel.sendMessage(embedB.build()).queue(m -> this.addReactions(m, options, Collections.singleton(sender), 30, TimeUnit.SECONDS, index -> {
                    if (index >= docs.size())
                    {                //cancel button or other error
                        stopReactions(m, false);
                        m.delete().queue();
                        return;
                    }
                    stopReactions(m);
                    m.editMessage(getDocMessage(JDocUtil.JDOCBASE, docs.get(index))).queue();
                }));
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
