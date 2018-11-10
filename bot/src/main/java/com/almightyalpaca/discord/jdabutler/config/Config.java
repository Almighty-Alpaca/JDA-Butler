package com.almightyalpaca.discord.jdabutler.config;

import com.almightyalpaca.discord.jdabutler.config.exception.ConfigSaveException;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;
import com.almightyalpaca.discord.jdabutler.util.StringUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Config
{

    private final Config parent;
    protected boolean autoSave;
    protected final JsonObject config;

    protected Config(final Config parent, final JsonObject config) throws WrongTypeException, KeyNotFoundException
    {
        this.parent = parent;
        this.config = config;
    }

    public void clear()
    {
        this.config.entrySet().clear();
    }

    public BigDecimal getBigDecimal(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonObject(key).getAsBigDecimal();
        }
        catch (final Exception e)
        {
            throw new WrongTypeException(e);
        }
    }

    public final BigDecimal getBigDecimal(final String key, final BigDecimal defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getBigDecimal(key);
    }

    public BigInteger getBigInteger(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonObject(key).getAsBigInteger();
        }
        catch (final Exception e)
        {
            throw new WrongTypeException(e);
        }
    }

    public final BigInteger getBigInteger(final String key, final BigInteger defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getBigInteger(key);
    }

    public boolean getBoolean(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsBoolean();
        }
        catch (final Exception e)
        {
            throw new WrongTypeException(e);
        }
    }

    public final boolean getBoolean(final String key, final boolean defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getBoolean(key);
    }

    public byte getByte(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsByte();
        }
        catch (final Exception e)
        {
            throw new WrongTypeException(e);
        }
    }

    public final byte getByte(final String key, final byte defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getByte(key);
    }

    public char getCharacter(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsCharacter();
        }
        catch (final Exception e)
        {
            throw new WrongTypeException(e);
        }
    }

    public final char getCharacter(final String key, final char defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getCharacter(key);
    }

    public Config getConfig(final String key) throws WrongTypeException, KeyNotFoundException
    {
        return this.getConfig(key, new Config(this, new JsonObject()));
    }

    public final Config getConfig(final String key, final Config defaultValue)
    {
        return this.getConfig(key, defaultValue.config);
    }

    public final Config getConfig(final String key, final JsonObject defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return new Config(this, this.getJsonObject(key));
    }

    public File getConfigFile()
    {
        return this.parent.getConfigFile();
    }

    public double getDouble(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsDouble();
        }
        catch (final NumberFormatException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final double getDouble(final String key, final double defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getDouble(key);
    }

    public Map<String, Object> getEntries()
    {
        final Map<String, Object> map = new HashMap<>();
        for (final Entry<String, JsonElement> entry : this.config.getAsJsonObject().entrySet())
            map.put(entry.getKey(), entry.getValue());
        return map;
    }

    public float getFloat(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsFloat();
        }
        catch (final NumberFormatException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final float getFloat(final String key, final float defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getFloat(key);
    }

    public int getInt(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsInt();
        }
        catch (final NumberFormatException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final int getInt(final String key, final int defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getInt(key);
    }

    public JsonArray getJsonArray(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonElement(key).getAsJsonArray();
        }
        catch (final IllegalStateException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final JsonArray getJsonArray(final String key, final JsonArray defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getJsonArray(key);
    }

    public JsonElement getJsonElement(final String key) throws KeyNotFoundException, WrongTypeException
    {
        final String[] path = key.split("\\.");
        JsonElement value = this.config;
        try
        {
            for (String element : path)
            {
                if (element.trim().isEmpty())
                    continue;
                if (element.endsWith("]") && element.contains("["))
                {
                    final int i = element.lastIndexOf("[");
                    int index;
                    try
                    {
                        index = Integer.parseInt(element.substring(i).replace("[", "").replace("]", ""));
                    }
                    catch (final Exception e)
                    {
                        index = 0;
                    }
                    element = element.substring(0, i);

                    value = value.getAsJsonObject().get(element);
                    value = value.getAsJsonArray().get(index);

                }
                else
                    value = value.getAsJsonObject().get(element);
            }
            if (value == null)
                throw new NullPointerException();
            return value;
        }
        catch (final IllegalStateException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final JsonElement getJsonElement(final String key, final JsonElement defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getJsonElement(key);
    }

    public JsonObject getJsonObject(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonElement(key).getAsJsonObject();
        }
        catch (final IllegalStateException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final JsonObject getJsonObject(final String key, final JsonObject defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getJsonObject(key);
    }

    public JsonPrimitive getJsonPrimitive(final String key) throws KeyNotFoundException, WrongTypeException
    {
        try
        {
            return this.getJsonElement(key).getAsJsonPrimitive();
        }
        catch (final IllegalStateException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final JsonPrimitive getJsonPrimitive(final String key, final JsonPrimitive defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getJsonPrimitive(key);
    }

    public long getLong(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsLong();
        }
        catch (final NumberFormatException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final long getLong(final String key, final long defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getLong(key);
    }

    public Number getNumber(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsNumber();
        }
        catch (final NumberFormatException e)
        {
            throw new WrongTypeException(e);
        }
        catch (final NullPointerException e)
        {
            throw new KeyNotFoundException(e);
        }
    }

    public final Number getNumber(final String key, final Number defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getNumber(key);
    }

    public short getShort(final String key) throws WrongTypeException, KeyNotFoundException
    {
        try
        {
            return this.getJsonPrimitive(key).getAsShort();
        }
        catch (final Exception e)
        {
            throw new WrongTypeException(e);
        }
    }

    public final short getShort(final String key, final short defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getShort(key);
    }

    public String getString(final String key) throws WrongTypeException, KeyNotFoundException
    {
        return this.getJsonPrimitive(key).getAsString();
    }

    public final String getString(final String key, final String defaultValue)
    {
        if (!this.hasKey(key))
            this.put(key, defaultValue);
        return this.getString(key);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasKey(final String key)
    {
        try
        {
            this.getJsonElement(key);
        }
        catch (final Exception e)
        {
            return false;
        }
        return true;
    }

    public boolean isEmpty()
    {
        return this.config.entrySet().isEmpty();
    }

    public void put(final String key, final boolean value)
    {
        this.put(key, new JsonPrimitive(value));
    }

    public void put(final String key, final Character value)
    {
        this.put(key, new JsonPrimitive(value));
    }

    public Config put(final String key, final Config value)
    {
        this.put(key, value.config);
        return this.getConfig(key);
    }

    public void put(String key, final JsonElement value) throws WrongTypeException
    {
        final String finalKey = key.substring(key.lastIndexOf(".") + 1);
        key = StringUtils.replaceLast(key, finalKey, "");
        if (key.endsWith("."))
            key = StringUtils.replaceLast(key, ".", "");
        final String[] path = key.split("\\.");
        JsonObject current = this.config;

        try
        {
            for (String element : path)
            {
                if (element.trim().isEmpty())
                    continue;
                if (element.endsWith("]") && element.contains("["))
                {
                    final int i = element.lastIndexOf("[");
                    int index;
                    try
                    {
                        index = Integer.parseInt(element.substring(i).replace("[", "").replace("]", ""));
                    }
                    catch (final Exception e)
                    {
                        index = -1;
                    }
                    element = element.substring(0, i);

                    if (!current.has(element))
                        current.add(element, new JsonArray());
                    final JsonArray array = current.get(element).getAsJsonArray();
                    if (index == -1)
                    {
                        final JsonObject object = new JsonObject();
                        array.add(object);
                        current = object;
                    }
                    else
                    {
                        if (index == array.size())
                            array.add(new JsonObject());
                        current = array.get(index).getAsJsonObject();
                    }

                }
                else
                {
                    if (!current.has(element))
                        current.add(element, new JsonObject());
                    current = current.get(element).getAsJsonObject();
                }
            }
        }
        catch (final IllegalStateException e)
        {
            throw new WrongTypeException(e);
        }

        current.add(finalKey, value);
    }

    public void put(final String key, final Number value)
    {
        this.put(key, new JsonPrimitive(value));
    }

    public void put(final String key, final String value)
    {
        this.put(key, new JsonPrimitive(value));
    }

    public void remove(final String key)
    {
        this.config.remove(key);
    }

    public void rename(final String from, final String to) throws KeyNotFoundException, WrongTypeException
    {
        this.put(to, this.getJsonElement(from));
        this.remove(from);
    }

    public void save() throws ConfigSaveException
    {
        this.parent.save();
    }

    public Config setAutoSave(final boolean autoSave)
    {
        this.autoSave = autoSave;
        return this;
    }

    @Override
    public String toString()
    {
        return this.config.toString();
    }

}
