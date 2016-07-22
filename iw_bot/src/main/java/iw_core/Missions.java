package iw_core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;

import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.impl.JDAImpl;
import net.dv8tion.jda.entities.impl.MessageImpl;
import net.dv8tion.jda.managers.ChannelManager;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.managers.RoleManager;
import provider.DiscordInfo;

public class Missions {
	private static List<MissionChannel> missionChannels = new ArrayList<MissionChannel>();

	private static MissionChannel getChannel(String textChanID) {
		for (MissionChannel chan : missionChannels) {
			if (chan.getId().equals(textChanID))
				return chan;
		}
		
		return null;
	}
	
	public static void newList(TextChannel channel, String list) {
		MissionChannel missionChannel = getChannel(channel.getId());
		if (missionChannel == null) {
			missionChannel = new MissionChannel(channel.getId(), channel.getGuild());
			missionChannels.add(missionChannel);
		}
		
		missionChannel.add(list);
	}
	
	public static void nextListEntry(String textChanID) {
		getChannel(textChanID).next();
	}
	
	public static void getList(String textChanID) {
		getChannel(textChanID).print(true);
	}
	
	public static void create(String name, GuildManager guildManager) {
		ChannelManager missionChannelManager = null;
		RoleManager missionRoleManager = null;
		Role everyoneRole = guildManager.getGuild().getPublicRole();
		Role moderatorRole = guildManager.getGuild().getRoleById(DiscordInfo.getAdminRoleIDs().get(0));
		Role missionRole = null;
		
		String channelName = "mission_" + name;
		String roleName = "Mission_" + name;
		
		missionRoleManager = guildManager.getGuild().createCopyOfRole(guildManager.getGuild().getRoleById("205515484223766528"));
		missionRole = missionRoleManager.getRole();
		missionRoleManager.setName(roleName).update();
		
		missionChannelManager = guildManager.getGuild().createTextChannel(channelName);
		PermissionOverrideManager permissionManager = missionChannelManager.getChannel().createPermissionOverride(moderatorRole);
		permissionManager.grant(Permission.MESSAGE_READ);
		permissionManager.grant(Permission.MESSAGE_WRITE);
		permissionManager.grant(Permission.MESSAGE_MENTION_EVERYONE);
		permissionManager.grant(Permission.MESSAGE_HISTORY);
		permissionManager.grant(Permission.MANAGE_PERMISSIONS);
		permissionManager.grant(Permission.MANAGE_CHANNEL);
		permissionManager.update();
		
		permissionManager = missionChannelManager.getChannel().createPermissionOverride(missionRole);
		permissionManager.grant(Permission.MESSAGE_READ);
		permissionManager.grant(Permission.MESSAGE_WRITE);
		permissionManager.grant(Permission.MESSAGE_MENTION_EVERYONE);
		permissionManager.grant(Permission.MESSAGE_HISTORY);
		permissionManager.update();
		
		permissionManager = missionChannelManager.getChannel().createPermissionOverride(everyoneRole);
		permissionManager.deny (Permission.MESSAGE_READ);
		permissionManager.deny (Permission.MESSAGE_WRITE);
		permissionManager.update();
	}

	public static void archive(TextChannel channel) {
		Role assoRole = null;
		for (Role role : channel.getGuild().getRoles()) {
			if (role.getName().equalsIgnoreCase(channel.getName()))
				assoRole = role;
		}
		
		List<String> lines = new ArrayList<String>();
		lines.add("*****************START OF CHANNEL '" + channel.getName() + "' LOG*****************");
		for (Message message : new MessageHistory(channel).retrieveAll()) {
			String timestamp = message.getTime() 	== null ? "[?]" : "[" + message.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]";
			String author	 = message.getAuthor() 	== null ? "?" 	: message.getAuthor().getUsername();
			String content 	 = message.getContent() == null ? "?" 	: message.getContent();
			lines.add(timestamp + " " + author + ": " + content);
		}
		lines.add("*******************END OF CHANNEL '" + channel.getName() + "' LOG*****************");
		
		Path pFile = Paths.get("./ChannelLogs/" + channel.getName() + ".txt");
		try {
			Files.write(pFile, lines, Charset.forName("UTF-8"), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}

		File file = new File ("./ChannelLogs/" + channel.getName() + ".txt");
		channel.getJDA().getTextChannelById(DiscordInfo.getAdminChanID()).sendMessage(channel.getName() + " archived.");
		channel.getJDA().getTextChannelById(DiscordInfo.getAdminChanID()).sendFile(file, null);
		channel.getManager().delete();
		assoRole.getManager().delete();
	}
	
}
