package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.ButtonListener;
import com.almightyalpaca.discord.jdabutler.commands.ReactionCommand;
import com.almightyalpaca.discord.jdabutler.commands.ReactionListenerRegistry;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.util.Paginator;
import com.kantenkugel.discordbot.jdocparser.Documentation;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.jdocparser.JDocUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DocsCommand extends ReactionCommand
{
    private static final int RESULTS_PER_PAGE = 5;
    private static final String[] ALIASES = new String[]{"documentation", "doc", "jdoc", "jdocs"};
    private final ButtonListener buttons;

    public DocsCommand(ReactionListenerRegistry registry, ButtonListener buttons)
    {
        super(registry);
        this.buttons = buttons;
    }

    private static Message getDocMessage(String jDocBase, Documentation documentation)
    {
        EmbedBuilder embed = getDefaultEmbed()
                .setTitle(documentation.getTitle(), documentation.getUrl(jDocBase));
        Map<String, List<String>> fields = documentation.getFields();
        if(fields != null && fields.get("Deprecated:") != null)
        {
            //element deprecated
            embed.setColor(Color.RED);
        }
        else if(fields != null && fields.get("Incubating") != null)
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
        if (fields != null && fields.size() > 0)
        {
            for (Map.Entry<String, List<String>> field : fields.entrySet())
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

    private void showPaginatorEmbed(GuildMessageReceivedEvent event, User sender, String jDocBase, Set<Documentation> items)
    {
        Paginator paginator = new Paginator(event.getMessage());
        buttons.addListener(paginator.getId(), paginator::onButtonClick);
        List<Documentation> sorted = items.stream().sorted(Comparator.comparing(Documentation::getShortTitle)).collect(Collectors.toList());
        for (int i = 0; i <= (sorted.size() - 1) / RESULTS_PER_PAGE; i++)
            paginator.addPage(getMultiResult(jDocBase, sorted, i));

        event.getMessage()
            .reply(paginator.getCurrent())
            .setActionRows(paginator.getButtons())
            .mentionRepliedUser(false)
            .queue(linkReply(event, null));
    }

    private void showRefinementEmbed(GuildMessageReceivedEvent event, User sender, String jDocBase, List<Documentation> docs)
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
        reply(event, embedB.build(),
                m -> this.addReactions(m, options, Collections.singleton(sender), 30, TimeUnit.SECONDS, index -> {
                    if (index >= docs.size())
                    {   //cancel button or other error
                        stopReactions(m, false);
                        m.delete().queue();
                        return;
                    }
                    stopReactions(m);
                    m.editMessage(getDocMessage(jDocBase, docs.get(index))).queue();
                })
        );
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
        return new EmbedBuilder().setAuthor("JDA Javadocs", null, EmbedUtil.getJDAIconUrl()).setColor(EmbedUtil.COLOR_JDA_PURPLE);
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
            String cmd = Bot.config.getString("prefix") + "docs";
            reply(event, "Searches and prints out documentation.\n"
                + "Syntax: `" + cmd + " [term | search:[params:]term | java:term | help]`.\n"
                + "\n"
                + "**Standard Mode**\n"
                + "When in `standard` mode, `term` is a class name or a class-prefixed variable or method name. "
                + "Both `.` and `#` can be used to specify inner classes, methods and variables. "
                + "Omitting the method parentheses will print all available methods with given name. "
                + "When specified (with parameter types), only the specific one is returned.\n"
                + "\n"
                + "__Examples__:\n"
                + "`" + cmd + " JDA#getUserById`\n"
                + "`" + cmd + " TextChannel.sendMessage`\n"
                + "`" + cmd + " TextChannel.sendMessage(Message)`\n"
                + "\n"
                + "**Search Mode**\n"
                + "When in `search` mode, `.` and `#` won't work and all documentations that **contain** `term` in their name/signature are returned.\n"
                + "Search parameters can be used to restrict the search:\n"
                + "   `f  ` - to only search for methods\n"
                + "   `var` - to only search for variables\n"
                + "   `c  ` - to only search for classes\n"
                + "   `cs ` - to make matching case-sensitive\n"
                + "\n"
                + "__Examples__:\n"
                + "`" + cmd + " search:onGuildMember`\n"
                + "`" + cmd + " search:c:join`\n"
                + "`" + cmd + " search:f:getTextChannel`\n"
                + "\n"
                + "**Java JDK Mode**\n"
                + "When in `java` mode, java 8 docs are searched instead. Syntax for `term` is the same as `standard` mode.\n"
                + "\n"
                + "__Examples__:\n"
                +  "`" + cmd + " java:BufferedInputStream`\n"
                +  "`" + cmd + " java:List.get`\n"
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
                        showPaginatorEmbed(event, sender, JDocUtil.JDOCBASE, search);
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
                            showRefinementEmbed(event, sender, JDocUtil.JAVA_JDOCS_PREFIX, javadocs);
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
                showRefinementEmbed(event, sender, JDocUtil.JDOCBASE, docs);
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
