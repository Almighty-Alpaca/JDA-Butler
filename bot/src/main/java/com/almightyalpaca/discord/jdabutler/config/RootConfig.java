package com.almightyalpaca.discord.jdabutler.config;

import com.almightyalpaca.discord.jdabutler.config.exception.ConfigSaveException;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;
import com.google.gson.*;
import org.apache.commons.text.translate.UnicodeUnescaper;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RootConfig extends Config
{

    private final File configFile;

    RootConfig(final File file) throws WrongTypeException, KeyNotFoundException, JsonIOException, JsonSyntaxException, FileNotFoundException
    {
        super(null, JsonParser.parseReader(new FileReader(file)).getAsJsonObject());
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
        try(Writer writer = new OutputStreamWriter(new FileOutputStream(this.configFile), StandardCharsets.UTF_8))
        {
            new UnicodeUnescaper().translate(json, writer);
        }
        catch (final IOException e)
        {
            throw new ConfigSaveException(e);
        }
    }

}
