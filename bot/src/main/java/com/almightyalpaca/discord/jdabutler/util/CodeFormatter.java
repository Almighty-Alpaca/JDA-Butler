package com.almightyalpaca.discord.jdabutler.util;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CodeFormatter {
	private static final Pattern BLOCK_PATTERN = Pattern.compile("([\\s\\S]*)[`']{3}[a-zA-Z]*\\n([\\s\\S]*)\\n?[`']{3}([\\s\\S]*)");
	
	private static final Formatter FORMATTER = Formatter.createFormatter(JavaFormatterOptions.builder()
			.style(JavaFormatterOptions.Style.PALANTIR)
			.build());

	public static String format(String code, boolean fromFile) throws FormatterException {
		final Matcher matcher = BLOCK_PATTERN.matcher(code);
		final boolean isCodeBlock = matcher.find();

		final String prefix, suffix;
		if (isCodeBlock) {
			code = matcher.group(2).stripLeading();

			if (fromFile) {
				prefix = matcher.group(1).trim() + "\n\n";
				suffix = matcher.group(3).trim() + "\n\n";
			} else {
				prefix = matcher.group(1).trim();
				suffix = matcher.group(3).trim();
			}
		} else { //Replace potential lang at the start of the code
			prefix = "";
			code = code.trim();
			if (code.startsWith("java")) {
				code = code.substring(3);
			}
			suffix = "";
		}

		final String formattedCode = formatCode(code);
		final String resultMsg;
		if (fromFile) {
			resultMsg = prefix + formattedCode + suffix;
		} else {
			resultMsg = String.format("%s```java\n%s```%s", prefix, formattedCode, suffix);
		}

		return resultMsg;
	}

	private static String formatCode(String code) throws FormatterException {
		try { //Assume it's a complete file first
			return FORMATTER.formatSource(code);
		} catch (FormatterException e) { //Try formatting as partial code
			final String prefix = "public class Test{void x(){"; //We need this otherwise the formatter dies
			final String suffix = "}}";
			final String classCode = prefix + code + suffix;

			final String formattedCode = FORMATTER.formatSource(classCode);
			final String formattedUserCode = formattedCode.lines()
					.skip(2) //Skip class and method declaration
					.limit(formattedCode.lines().count() - 4) //Skip class and method closing curly brackets
					.collect(Collectors.joining("\n"));

			int i = 0;
			for (int userCodeLength = formattedUserCode.length(); i < userCodeLength; i++) {
				if (!Character.isWhitespace(formattedUserCode.charAt(i))) {
					break;
				}
			}

			final int removedSpaces = i;
			return formattedUserCode.lines()
					//Remove the 2 indentation from the class & method
					// And also reduce the insane indentation from this format style
					.map(s -> {
						if (s.isBlank()) { //Might have a air gap between code so don't try to substring, the beginIndex would be out of bounds
							return "";
						} else {
							return s.substring(removedSpaces).replace("    ", "   ");
						}
					})
					.collect(Collectors.joining("\n"));
		}
	}
}
