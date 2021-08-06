package com.almightyalpaca.discord.jdabutler.commands.commands;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.ButtonListener;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.CodeFormatter;
import com.palantir.javaformat.java.FormatterException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonInteraction;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class FormatCommand extends Command {
	public FormatCommand(ButtonListener buttonListener) {
		buttonListener.addListener("format", event -> {
			if (event.getComponentId().startsWith("format")) {
				final String[] args = event.getComponentId().split("\\|");
				if (args[1].equals("accept")) {
					String codeAuthorId = args[2];
					String msgId = args[3];
					
					if (!event.getUser().getId().equals(codeAuthorId)) { //Only accept code author clicks
						event.deferEdit().queue();
						return;
					}
					
					formatById(event.getTextChannel(), event, msgId);
				}
				
				//Message is not ephemeral.
				Objects.requireNonNull(event.getMessage(), "Message should not be ephemeral")
						.delete()
						.queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
			}
		});
	}

	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message eventMessage, final String content, final GuildMessageReceivedEvent event) {
		try {
			final long msgId = Long.parseLong(content);

			channel.retrieveMessageById(msgId).queue(m -> {
				if (m.getAuthor().isBot()) {
					event.getChannel().sendMessage("Can't format bot messages :angry:").queue();
					
					return;
				}
				
				final MessageBuilder messageBuilder = new MessageBuilder();
				final EmbedBuilder embedBuilder = new EmbedBuilder()
						.setAuthor("Format request", null, event.getAuthor().getEffectiveAvatarUrl())
						.addField("From", event.getAuthor().getAsMention(), true)
						.addField("Target message", "[Message Link](" + m.getJumpUrl() + ")", true);
				
				messageBuilder
						.setContent(m.getAuthor().getAsMention())
						.setEmbeds(embedBuilder.build())
						.setActionRows(ActionRow.of(
								Button.danger("format|accept|" + m.getAuthor().getId() + "|" + msgId, "Accept"),
								Button.secondary("format|reject" + msgId, "Reject")
						));
				
				m.getChannel().sendMessage(messageBuilder.build())
						.mentionUsers(m.getAuthor().getIdLong()) //Mention the target message author
						.queue(ignored -> eventMessage.delete().queue());
			});
		} catch (NumberFormatException ignored) {
			tryFormatMessage(eventMessage, content, event);
		}
	}

	private void formatById(TextChannel channel, ButtonInteraction event, String msgId) {
		channel.retrieveMessageById(msgId).queue(m -> inferContent(m, m.getContentRaw(), inferredContent -> {
					try {
						format(m.getTextChannel(), inferredContent.code, m.getAuthor().getIdLong(), m.getIdLong(), inferredContent.isFromFile);
					} catch (FormatterException e) {
						Bot.LOG.error("Error formatting java code", e);

						event.reply(":x: Could not format your code :/").setEphemeral(true).queue();
					}
				}), new ErrorHandler()
						.handle(ErrorResponse.UNKNOWN_MESSAGE, e -> event.reply("Invalid message ID").setEphemeral(true).queue())
						.handle(t -> true,
								t -> Bot.LOG.error("Error formatting java code (while retrieving message)", t))
		);
	}

	private void tryFormatMessage(Message message, String content, GuildMessageReceivedEvent event) {
		inferContent(message, content, inferredContent -> {
			try {
				format(event.getChannel(), inferredContent.code, event.getAuthor().getIdLong(), message.getIdLong(), inferredContent.isFromFile);
			} catch (FormatterException e) {
				Bot.LOG.error("Error formatting java code", e);
				
				event.getChannel().sendMessage(":x: Could not format your code :/").queue();
			}
		});
	}

	private void inferContent(Message message, String altContent, Consumer<InferredContent> callback) {
		if (!message.getAttachments().isEmpty()) {
			final Message.Attachment attachment = message.getAttachments().get(0);
			if ("java".equals(attachment.getFileExtension())) {
			    readAttachment(attachment, s -> callback.accept(new InferredContent(s, true)));

				return;
			}

			if (!attachment.isVideo() && !attachment.isImage()) {
                readAttachment(attachment, s -> callback.accept(new InferredContent(s, true)));
                
                return;
			}
		}

        callback.accept(new InferredContent(altContent, false));
	}

	private void readAttachment(Message.Attachment attachment, Consumer<String> consumer) {
		attachment.retrieveInputStream().thenAcceptAsync(input -> {
			try (BufferedInputStream in = new BufferedInputStream(input)) {
				consumer.accept(new String(in.readAllBytes()));
			} catch (IOException e) {
				Bot.LOG.error("Error reading attachment from {}", attachment.getProxyUrl(), e);
			}
		});
	}

	private void format(TextChannel channel, String content, long authorId, long codeMessageId, boolean fromFile) throws FormatterException {
		final String code = CodeFormatter.format(content, fromFile); //Can potentially throw

		final Guild guild = channel.getGuild();
		final JDA jda = channel.getJDA();
		guild.retrieveMemberById(authorId).queue(member -> {
			final WebhookMessage webhookMessage = getWebhookMessage(code, member, fromFile);

			channel.retrieveWebhooks().queue(webhooks -> {
				final RestAction<Webhook> webhookGetter = retrieveFormatWebhook(channel, jda, webhooks);

				webhookGetter.queue(webhook -> {
					JDAWebhookClient.fromJDA(webhook).send(webhookMessage);
					channel.deleteMessageById(codeMessageId).reason("Space freeing by format command").queue();
				});
			});
		});
	}

	@NotNull
	private RestAction<Webhook> retrieveFormatWebhook(TextChannel channel, JDA jda, List<Webhook> webhooks) {
		final String expectedWebhookName = jda.getSelfUser().getAsTag() + "'s Format webhook";
		final Optional<Webhook> optWebhook = webhooks.stream().filter(w -> w.getName().equals(expectedWebhookName)).findFirst();

		if (optWebhook.isPresent()) {
			return new CompletedRestAction<>(jda, optWebhook.get());
		} else {
			return channel.createWebhook(expectedWebhookName);
		}
	}

	@NotNull
	private WebhookMessage getWebhookMessage(String code, Member member, boolean fromFile) {
		final boolean correctLength = code.length() <= Message.MAX_CONTENT_LENGTH;

		final WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
				.setUsername(member.getEffectiveName())
				.setAvatarUrl(member.getUser().getEffectiveAvatarUrl());

		if (correctLength && !fromFile) {
			messageBuilder.setContent(code);
		} else {
			messageBuilder.addFile("code.java", code.getBytes(StandardCharsets.UTF_8));
		}

		return messageBuilder.build();
	}

	@Override
	public String getHelp() {
		return "Formats your java code";
	}

	@Override
	public String getName() {
		return "format";
	}

	private static class InferredContent {
		private final String code;
		private final boolean isFromFile;

		public InferredContent(String code, boolean isFromFile) {
			this.code = code;
			this.isFromFile = isFromFile;
		}
	}
}
