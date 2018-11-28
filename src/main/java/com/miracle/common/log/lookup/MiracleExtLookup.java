package com.miracle.common.log.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

@Plugin(name = "miracle", category = "Lookup")
public class MiracleExtLookup implements StrLookup {

    @Override
    public String lookup(String s) {
        return "";
    }

    @Override
    public String lookup(LogEvent logEvent, String s) {
        return lookup(s);
    }
}
