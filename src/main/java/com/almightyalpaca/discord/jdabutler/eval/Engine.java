package com.almightyalpaca.discord.jdabutler.eval;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import com.google.common.util.concurrent.MoreExecutors;

import com.almightyalpaca.discord.jdabutler.util.StringUtils;

public enum Engine {

	JAVASCRIPT("JavaScript", "js", "javascript") {

		private final ScriptEngineManager engineManager = new ScriptEngineManager();

		@Override
		public Triple<Object, String, String> eval(final Map<String, Object> fields, final Collection<String> classImports, final Collection<String> packageImports, final int timeout, String script) {
			String importString = "";
			for (final String s : packageImports) {
				importString += s + ", ";
			}
			importString = StringUtils.replaceLast(importString, ", ", "");

			script = " (function() { with (new JavaImporter(" + importString + ")) {" + script + "} })();";
			return this.eval(fields, timeout, script, this.engineManager.getEngineByName("nashorn"));
		}

	},
	GROOVY("Groovy", "groovy") {

		@Override
		public Triple<Object, String, String> eval(final Map<String, Object> fields, final Collection<String> classImports, final Collection<String> packageImports, final int timeout,
				final String script) {
			String importString = "";
			for (final String s : classImports) {
				importString += "import " + s + ";";
			}
			for (final String s : packageImports) {
				importString += "import " + s + ".*;";
			}
			return this.eval(fields, timeout, importString + script, new GroovyScriptEngineImpl());
		}

	};

	public static final Collection<String>			DEFAULT_IMPORTS	= Arrays.asList("net.dv8tion.jda.core.entities.impl", "net.dv8tion.jda.core.managers", "net.dv8tion.jda.core.entities",
			"net.dv8tion.jda.core", "java.lang", "java.io", "java.math", "java.util", "java.util.concurrent", "java.time");

	private final static ScheduledExecutorService	service			= Executors.newScheduledThreadPool(1, r -> new Thread(r, "Eval-Thread"));

	private final List<String>						codes;

	private final String							name;

	Engine(final String name, final String... codes) {
		this.name = name;
		this.codes = new ArrayList<>();
		for (final String code : codes) {
			this.codes.add(code.toLowerCase());
		}
	}

	public static Engine getEngineByCode(String code) {
		code = code.toLowerCase();
		for (final Engine engine : Engine.values()) {
			if (engine.codes.contains(code)) {
				return engine;
			}
		}
		return null;
	}

	public static void shutdown() {
		MoreExecutors.shutdownAndAwaitTermination(Engine.service, 10, TimeUnit.SECONDS);
	}

	public abstract Triple<Object, String, String> eval(Map<String, Object> fields, final Collection<String> classImports, final Collection<String> packageImports, int timeout, String script);

	protected Triple<Object, String, String> eval(final Map<String, Object> fields, final int timeout, final String script, final ScriptEngine engine) {

		for (final Entry<String, Object> shortcut : fields.entrySet()) {
			engine.put(shortcut.getKey(), shortcut.getValue());
		}

		final StringWriter outString = new StringWriter();
		final PrintWriter outWriter = new PrintWriter(outString);
		engine.getContext().setWriter(outWriter);

		final StringWriter errorString = new StringWriter();
		final PrintWriter errorWriter = new PrintWriter(errorString);
		engine.getContext().setErrorWriter(errorWriter);

		final ScheduledFuture<Object> future = Engine.service.schedule(() -> engine.eval(script), 0, TimeUnit.MILLISECONDS);

		Object result = null;

		try {
			result = future.get(timeout, TimeUnit.SECONDS);
		} catch (final ExecutionException e) {
			errorWriter.println(e.getCause().toString());
		} catch (TimeoutException | InterruptedException e) {
			future.cancel(true);
			errorWriter.println(e.toString());
		}

		return new ImmutableTriple<>(result, outString.toString(), errorString.toString());
	}

	public List<String> getCodes() {
		return this.codes;
	}

	public String getName() {
		return this.name;
	}

	public static class Import {

		private final Type		type;

		private final String	name;

		public Import(final Import.Type type, final String name) {
			this.type = type;
			this.name = name;
		}

		public final String getName() {
			return this.name;
		}

		public final Type getType() {
			return this.type;
		}

		public enum Type {
			CLASS,
			PACKAGE
		}

	}
}
