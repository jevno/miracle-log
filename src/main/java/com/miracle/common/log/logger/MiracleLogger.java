package com.miracle.common.log.logger;

import com.google.common.base.Splitter;
import com.miracle.common.log.utils.CatUtils;
import com.miracle.common.miracle_utils.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import java.util.Map;

public class MiracleLogger extends ExtendedLoggerWrapper {
    private final ExtendedLoggerWrapper innerLogger;

    private static final String FQCN = MiracleLogger.class.getName();

    public static final Level BUSINESS = Level.forName("BUSINESS", 350);

    private MiracleLogger(final Logger logger)
    {
        super((AbstractLogger) logger, logger.getName(), logger.getMessageFactory());
        this.innerLogger = this;
    }

    public static MiracleLogger getLogger()
    {
        final Logger wrapped = LogManager.getLogger();
        return new MiracleLogger(wrapped);
    }

    public static MiracleLogger getLogger(final Class<?> loggerName) {
        final Logger wrapped = LogManager.getLogger(loggerName);
        return new MiracleLogger(wrapped);
    }

    public static MiracleLogger getLogger(final Class<?> loggerName, final MessageFactory factory) {
        final Logger wrapped = LogManager.getLogger(loggerName, factory);
        return new MiracleLogger(wrapped);
    }

    public static MiracleLogger getLogger(final Object value) {
        final Logger wrapped = LogManager.getLogger(value);
        return new MiracleLogger(wrapped);
    }

    public static MiracleLogger getLogger(final Object value, final MessageFactory factory) {
        final Logger wrapped = LogManager.getLogger(value, factory);
        return new MiracleLogger(wrapped);
    }

    public static MiracleLogger getLogger(final String name) {
        final Logger wrapped = LogManager.getLogger(name);
        return new MiracleLogger(wrapped);
    }

    public static MiracleLogger getLogger(final String name, final MessageFactory factory) {
        final Logger wrapped = LogManager.getLogger(name, factory);
        return new MiracleLogger(wrapped);
    }

    public boolean isNull(Object obj, String warnMsg)
    {
        if(obj == null)
        {
            innerLogger.warn(warnMsg);
        }
        return obj == null;
    }

    public void business(String avroType, String[] keysArray, Object[] paramsArray)
    {
        if(keysArray != null && paramsArray != null && keysArray.length > 0)
        {
            if(keysArray.length != paramsArray.length)
            {
                innerLogger.warn("keysArray length {} must be equal paramsArray length {}",
                        keysArray.length, paramsArray.length);
                return;
            }

            String[] newKeysArray = new String[keysArray.length + 2];
            Object[] newParamsArray = new Object[keysArray.length + 2];

            StringBuilder sb = new StringBuilder();
            //append _id_, _ts_ default
            sb.append("_id_=");
            String msgId = CatUtils.getCatCurrMsgId();
            newKeysArray[0] = "_id_";
            newParamsArray[0] = msgId;
            sb.append(msgId==null?"":msgId);
            sb.append(",_ts_=");
            newKeysArray[1] = "_ts_";
            newParamsArray[1] = System.currentTimeMillis();
            sb.append(System.currentTimeMillis());

            for(int i = 0; i < keysArray.length; i++)
            {
                newKeysArray[i+2] = keysArray[i];
                newParamsArray[i+2] = paramsArray[i];
                sb.append(",");
                sb.append(keysArray[i]);
                sb.append("=");
                sb.append(paramsArray[i]==null?"null":paramsArray[i].toString());
            }
            innerLogger.logIfEnabled(FQCN, BUSINESS, null, sb.toString(),
                    avroType, newKeysArray, newParamsArray);
        }
    }

    public void business(String avroType, final String message, final Object... params) {
        if(StringUtils.isEmpty(message) || params == null || params.length == 0)
        {
            return;
        }
        int paramsArrayLen = params.length;
        Object[] paramsArray = new Object[paramsArrayLen];
        int idx = 0;
        for(Object param : params)
        {
            paramsArray[idx++] = param;
        }
        Map<String, String> parameterMap = Splitter.on(',').withKeyValueSeparator('=').split(message);
        String[] keysArray = new String[parameterMap.keySet().size()];
        keysArray = parameterMap.keySet().toArray(keysArray);
        business(avroType, keysArray, paramsArray);
    }

    public void business(String avroType, Map<String, Object> kvMap) {
        if(kvMap != null && kvMap.size() > 0)
        {
            int len = kvMap.size();
            String[] keysArray = new String[len];
            Object[] paramsArray = new Object[len];
            keysArray = kvMap.keySet().toArray(keysArray);
            paramsArray = kvMap.values().toArray(paramsArray);
            business(avroType, keysArray, paramsArray);
        }
    }
}
