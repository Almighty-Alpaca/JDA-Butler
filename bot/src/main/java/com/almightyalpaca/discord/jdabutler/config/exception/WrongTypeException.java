package com.almightyalpaca.discord.jdabutler.config.exception;

public class WrongTypeException extends RuntimeException
{

    private static final long serialVersionUID = -9061684748274947934L;

    public WrongTypeException()
    {
        super();
    }

    public WrongTypeException(final String string)
    {
        super(string);
    }

    public WrongTypeException(final String string, final Throwable throwable)
    {
        super(string, throwable);
    }

    public WrongTypeException(final Throwable throwable)
    {
        super(throwable);
    }

}
