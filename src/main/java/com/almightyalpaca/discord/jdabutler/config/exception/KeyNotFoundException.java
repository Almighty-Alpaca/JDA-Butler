package com.almightyalpaca.discord.jdabutler.config.exception;

public class KeyNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 8626730187149822914L;

	public KeyNotFoundException() {
		super();
	}

	public KeyNotFoundException(final String string) {
		super(string);
	}

	public KeyNotFoundException(final String string, final Throwable throwable) {
		super(string, throwable);
	}

	public KeyNotFoundException(final Throwable throwable) {
		super(throwable);
	}
}
