package com.almightyalpaca.discord.jdabutler.commands.commands.moderation;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;

import java.util.List;

public class SoftbanCommand implements Command {
    @Override
    public void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent
            event) throws Exception {
		Member sendMem = event.getMember();
		if (!sendMem.hasPermission(Permission.KICK_MEMBERS)) { // only kick cause its not perma ban
			channel.sendMessage("WhO Do U thINk u Are?").queue();
			return;
		}

		List<User> mentions = message.getMentionedUsers();
		if (mentions.isEmpty()) {
			channel.sendMessage("PLeAse mENTion SOmEoNe!").queue();
			return;
		}

		Guild guild = event.getGuild();
		GuildController controller = guild.getController();
		Member self = guild.getSelfMember();

		for (User user : mentions) {
			if (user == null)
				continue;
			Member member = guild.getMember(user);
			if (member != null && !self.canInteract(member))
				continue;
			String reason = "Softban by " + sender.getName();
			controller.ban(user, 7).reason(reason).queue(
				(v) -> controller.unban(user).reason(reason).queue()
			);
		}
    }

    @Override
    public String getHelp() {
        return "Kicks user and clears past messages";
    }

    @Override
    public String getName() {
        return "softban";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"sb"};
    }
}
