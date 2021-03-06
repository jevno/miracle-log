package com.miracle.common.log.appender;

import com.dianping.cat.Cat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;

import java.io.Serializable;

@Plugin(name = "CatAppender", category = "Core", elementType = "appender", printObject = true)
public class CatLog4j2Appender extends AbstractAppender {

    protected CatLog4j2Appender(String name, Filter filter, Layout<? extends Serializable> layout,
                                final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event) {
        try {
            Level level = event.getLevel();

            if (level.isMoreSpecificThan(Level.ERROR)) {
                logError(event);
            }
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        }
    }

    @PluginFactory
    public static CatLog4j2Appender createAppender(@PluginAttribute("name") String name,
                                                   @PluginElement("Filter") final Filter filter,
                                                   @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                   @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for CatLog4j2Appender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new CatLog4j2Appender(name, filter, layout, true);
    }

    private void logError(LogEvent event) {
        ThrowableProxy info = event.getThrownProxy();
        Message message = event.getMessage();
        if (info != null) {
            Throwable exception = info.getThrowable();

            if (message != null) {
                Cat.logError(message.getFormattedMessage(), exception);
            } else {
                Cat.logError(exception);
            }
        }
        else if(message != null)
        {
            Cat.logEvent("Error", "", "ERROR", message.getFormattedMessage());
        }

    }

}
