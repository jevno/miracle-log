package com.miracle.common.log.patternconverter;

import com.miracle.common.log.utils.CatUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@Plugin(name = "MsgIdPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "y", "msgId" })
public class MsgIdPatternConverter extends LogEventPatternConverter {

    private static final MsgIdPatternConverter INSTANCE =
            new MsgIdPatternConverter();

    public static MsgIdPatternConverter newInstance(
            final String[] options) {
        return INSTANCE;
    }

    private MsgIdPatternConverter(){
        super("MsgId", "msgId");
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String msgId = CatUtils.getCatRootMsgId();
        toAppendTo.append(msgId==null?"":msgId);
    }
}
