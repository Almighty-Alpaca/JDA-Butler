package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DocsCommand extends ReactionCommand {

	public DocsCommand(Dispatcher.ReactionListenerRegistry registry) {
		super(registry);
	}

	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		if(content.trim().isEmpty()) {
			channel.sendMessage(new MessageBuilder().append("See the docs here: ").append(JDocUtil.JDOCBASE).build()).queue();
			return;
		}
		final List<Documentation> docs = JDoc.get(content);
		if(docs.size() == 0) {
			channel.sendMessage("No Result found!").queue();
		} else if(docs.size() == 1) {
			channel.sendMessage(getDocMessage(docs.get(0))).queue();
		} else {
			EmbedBuilder embedB = getDefaultEmbed().setTitle("Refine your Search");
			for(int i = 0; i < docs.size(); i++) {
				Documentation doc = docs.get(i);
				embedB.appendDescription(ReactionCommand.NUMBERS[i] + " [" + doc.getTitle() + "](" + doc.getUrl() + ")\n");
			}
			embedB.getDescriptionBuilder().setLength(embedB.getDescriptionBuilder().length() - 1);
			List<String> options = new ArrayList<>(Arrays.asList(Arrays.copyOf(ReactionCommand.NUMBERS, docs.size())));
			options.add(ReactionCommand.CANCEL);
			channel.sendMessage(embedB.build()).queue(m -> this.addReactions(
					m,
					options,
					Collections.singleton(sender),
					30, TimeUnit.SECONDS,
					index -> {
						if(index >= docs.size()) {				//cancel button or other error
							stopReactions(m, false);
							m.delete().queue();
							return;
						}
						stopReactions(m);
						m.editMessage(getDocMessage(docs.get(index))).queue();
					}
			));
		}
	}

	@Override
	public String getHelp() {
		return "Displays documentation";
	}

	@Override
	public String getName() {
		return "docs";
	}

	private static Message getDocMessage(Documentation documentation) {
		EmbedBuilder embed = getDefaultEmbed()
				.setTitle(documentation.getTitle(), documentation.getUrl());
		if(documentation.getContent().length() > MessageEmbed.TEXT_MAX_LENGTH) {
			embed.setDescription("Description to long. Please refer to [the docs](" + documentation.getUrl() + ')');
			return new MessageBuilder().setEmbed(embed.build()).build();
		}
		if(documentation.getContent().length() == 0) {
			embed.setDescription("No Description available.");
		} else {
			embed.setDescription(documentation.getContent());
		}
		if(documentation.getFields() != null && documentation.getFields().size() > 0) {
			for(Map.Entry<String, List<String>> field : documentation.getFields().entrySet()) {
				String fieldValue = String.join("\n", field.getValue());
				if(fieldValue.length() > MessageEmbed.VALUE_MAX_LENGTH) {
					embed.addField(field.getKey(), "This section is to long. Please look at [the docs](" + documentation.getUrl() + ')', false);
				} else {
					embed.addField(field.getKey(), field.getValue().stream().collect(Collectors.joining("\n")), false);
				}
			}
		}
		return new MessageBuilder().setEmbed(embed.build()).build();
	}

	private static EmbedBuilder getDefaultEmbed() {
		return new EmbedBuilder().setAuthor("JDA Javadocs", null, EmbedUtil.JDA_ICON).setColor(EmbedUtil.COLOR_JDA_PRUPLE);
	}
}
