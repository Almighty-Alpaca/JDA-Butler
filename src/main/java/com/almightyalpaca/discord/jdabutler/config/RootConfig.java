package com.almightyalpaca.discord.jdabutler.config;

import com.almightyalpaca.discord.jdabutler.config.exception.ConfigSaveException;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;
import com.google.gson.*;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

import java.io.*;

public class RootConfig extends Config
{

    private final File configFile;

    RootConfig(final File file) throws WrongTypeException, KeyNotFoundException, JsonIOException, JsonSyntaxException, FileNotFoundException
    {
        super(null, new JsonParser().parse(new FileReader(file)).getAsJsonObject());
        this.configFile = file;
    }

    @Override
    public File getConfigFile()
    {
        return this.configFile;
    }

    @Override
    public void save() throws ConfigSaveException
    {
        final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
        final String json = gson.toJson(this.config);
        try
        {
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.configFile), "UTF-8"));
            new UnicodeUnescaper().translate(json, writer);
            writer.close();
        }
        catch (final IOException e)
        {
            throw new ConfigSaveException(e);
        }
    }

}
