package com.almightyalpaca.discord.jdabutler.util.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.ErrorStatus;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebhookAppender extends AppenderBase<ILoggingEvent> {
    private static Pattern WH_PATTERN = Pattern.compile("(?:https?://)?(?:\\w+\\.)?discordapp\\.com/api(?:/v\\d+)?/webhooks/(\\d+)/([\\w-]+)(?:/(?:\\w+)?)?");;

    private Encoder<ILoggingEvent> encoder;
    private String webhookUrl;
    private WebhookClient client;

    @Override
    public void start() {
        int warn = 0;
        if(encoder == null) {
            addStatus(new ErrorStatus("No encoder specified", this));
            warn++;
        }
        Matcher matcher = null;
        if(webhookUrl == null || webhookUrl.isEmpty()) {
            addStatus(new ErrorStatus("No Webhook url specified", this));
            warn++;
        } else {
            matcher = WH_PATTERN.matcher(webhookUrl);
            if(!matcher.matches()) {
                addStatus(new ErrorStatus("Webhook url was not a valid Webhook url", this));
                warn++;
            }
        }
        if(warn == 0) {
            client = new WebhookClientBuilder(Long.parseUnsignedLong(matcher.group(1)), matcher.group(2)).setDaemon(true).build();
            super.start();
        }
    }

    @Override
    public void stop() {
        if(client != null)
            client.close();
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if(!isStarted())
            return;
        if(eventObject.getLoggerName().equals("net.dv8tion.jda.webhook.WebhookClient"))
            return;
        byte[] encode = encoder.encode(eventObject);
        String log = new String(encode);
        client.send(log.length() > 2000 ? log.substring(0, 1997) + "..." : log);
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
