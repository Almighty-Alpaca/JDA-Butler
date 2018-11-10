package com.almightyalpaca.discord.jdabutler.config.exception;

public class ConfigSaveException extends RuntimeException
{

    private static final long serialVersionUID = 8626730187149822914L;

    public ConfigSaveException()
    {
        super();
    }

    public ConfigSaveException(final String string)
    {
        super(string);
    }

    public ConfigSaveException(final String string, final Throwable throwable)
    {
        super(string, throwable);
    }

    public ConfigSaveException(final Throwable throwable)
    {
        super(throwable);
    }
}
