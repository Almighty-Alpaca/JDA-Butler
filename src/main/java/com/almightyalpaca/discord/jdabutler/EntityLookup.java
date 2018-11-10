package com.almightyalpaca.discord.jdabutler;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

public class EntityLookup
{

    /*
        JDA Guild
     */
    public static final long GUILD_JDA_ID = 125227483518861312L;
    public static Guild getGuildJda()
    {
        return Bot.jda.getGuildById(GUILD_JDA_ID);
    }

    //ROLES
    public static final long ROLE_BOTS_ID = 125616720156033024L;
    public static Role getRoleBots()
    {
        return getGuildJda().getRoleById(ROLE_BOTS_ID);
    }

    public static final long ROLE_STAFF_ID = 169481978268090369L;
    public static Role getRoleStaff()
    {
        return getGuildJda().getRoleById(ROLE_STAFF_ID);
    }

    public static final long ROLE_HELPERS_ID = 183963327033114624L;
    public static Role getRoleHelper()
    {
        return getGuildJda().getRoleById(ROLE_HELPERS_ID);
    }

    //todo
    public static final long ROLE_ANNOUNCEMUTE_ID = -1L;
    public static Role getRoleAnnounceMute()
    {
        return getGuildJda().getRoleById(ROLE_ANNOUNCEMUTE_ID);
    }


    /*
        DAPI Guild
     */
    public static final long GUILD_DAPI_ID = 81384788765712384L;

    //CHANNELS/CATEGORIES
    public static final long CHANNEL_DAPI_JDA_ID = 381889648827301889L;
    public static final long CATEGORY_DAPI_TESTING_ID = 356505966201798656L;


    /*
        USERS
     */
    public static final long MAIN_BUTLER_ID = 189074312974696448L;
}
