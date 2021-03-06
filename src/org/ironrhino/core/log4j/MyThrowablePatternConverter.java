package org.ironrhino.core.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;

@Plugin(name = "MyThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "th" })
public class MyThrowablePatternConverter extends ThrowablePatternConverter {

	protected MyThrowablePatternConverter(String name, String style, String[] options, Configuration config) {
		super(name, style, options, config);
	}

	@Override
	public void format(LogEvent event, final StringBuilder buffer) {
		boolean b = event.getThrown() != null && options.anyLines();
		if (b)
			buffer.append('\t');
		super.format(event, buffer);
		if (b)
			buffer.append('\n');
	}

	public static MyThrowablePatternConverter newInstance(Configuration config, String[] options) {
		return new MyThrowablePatternConverter("Throwable", "throwable", options, config);
	}

}
