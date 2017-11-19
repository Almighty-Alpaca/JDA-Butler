package com.almightyalpaca.discord.jdabutler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JDAUtil
{

    public static List<Integer> getBuildNumbers(String input)
    {
        List<Integer> buildNums = new ArrayList<>(2);
        String[] args = input.split("[\\s+-]");
        for(String arg : args)
        {
            if(arg.isEmpty())
                continue;
            try
            {
                final int lastIndexOfUnderscore = arg.lastIndexOf('_');
                if (lastIndexOfUnderscore != -1)
                    arg = input.substring(lastIndexOfUnderscore + 1);

                buildNums.add(Integer.parseInt(arg));
            }
            catch (final Exception ex)
            {
                //min. one buildnum failed parsing, abort
                return Collections.emptyList();
            }
        }
        return buildNums;
    }
}