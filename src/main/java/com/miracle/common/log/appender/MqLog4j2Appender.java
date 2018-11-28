package com.miracle.common.log.appender;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.miracle.common.log.logger.MiracleLogger;
import com.miracle.common.log.utils.AvroUtils;

import com.miracle.common.miracle_utils.ConfigUtils;
import com.miracle.common.mq.MiracleHistory;
import com.miracle.common.mq.history.ActionHistory;
import org.apache.avro.Schema;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

@Plugin(name = "MqBusinessAppender", category = "Core", elementType = "appender", printObject = true)
public class MqLog4j2Appender extends AbstractAppender {

    private static final String BUSINESS_TOPIC = "kkbusiness";

    private static volatile MiracleHistory businessHistory = null;

    private static volatile Map<String, Schema> avroSchemaMap = Maps.newHashMap();

    private String mqConfigFilePath = null;
    private String avroConfFilePath = null;
    private volatile boolean bInitialized = false;

    protected MqLog4j2Appender(String name, String mqConfFile, String avroConfFile,
                               Filter filter, Layout<? extends Serializable> layout,
                                final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        mqConfigFilePath = mqConfFile;
        avroConfFilePath = avroConfFile;
    }

    private void init()
    {
        if(!bInitialized)
        {
            synchronized(this) {
                if(!bInitialized) {
                    bInitialized = true;
                    if (businessHistory == null) {
                        businessHistory = new MiracleHistory(mqConfigFilePath);
                    }
                    parseAvroConfFile(avroConfFilePath);
                }
            }
        }
    }

    private void parseAvroConfFile(String confFile)
    {
        String avroConfFullPath = ConfigUtils.getResourceFullPath(confFile);
        File file = new File(avroConfFullPath);
        String content = null;
        try {
            content = Files.toString(file, Charsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Read avro conf file failed", e);
            return;
        }

        Map<String, Schema> newSchemaMap = Maps.newHashMap();
        try {
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElem = jsonParser.parse(content);
            JsonArray jsonArray = jsonElem.getAsJsonArray();
            @SuppressWarnings("rawtypes")
            Iterator it = jsonArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = (JsonElement) it.next();
                Schema schema = new Schema.Parser().parse(elem.toString());
                newSchemaMap.put(schema.getName(), schema);
            }
        }
        catch(Throwable t)
        {
            LOGGER.error("Parse avro conf file failed", t);
            return;
        }
        avroSchemaMap = newSchemaMap;
    }

    @PluginFactory
    public static MqLog4j2Appender createAppender(@PluginAttribute("name") String name,
                                                  @PluginAttribute("mqConfFile") String mqConfFile,
                                                  @PluginAttribute("avroConfFile") String avroConfFile,
                                                  @PluginElement("Filter") final Filter filter,
                                                  @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                  @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for MqLog4j2Appender");
            return null;
        }
        if(mqConfFile == null) {
            LOGGER.error("No mq configure file provided for MqLog4j2Appender");
        }

        if(avroConfFile == null) {
            LOGGER.error("No avro configure file provided for MqLog4j2Appender");
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new MqLog4j2Appender(name, mqConfFile, avroConfFile, filter,
                layout, true);
    }

    @Override
    public void append(LogEvent event) {
        if(event.getLevel() != MiracleLogger.BUSINESS )
        {
            return;
        }
        init();
        if(businessHistory == null)
        {
            LOGGER.error("MqLog4j2Appender mqBusiness init failed, " +
                    "please check log4j2 configure file -- [MqBusinessAppender mqConfFile attribute]");
            return;
        }
        if(event.getMessage() != null) {
            Message logMessage = event.getMessage();
            Object[] parameters = logMessage.getParameters();
            if(parameters.length != 3) {
                LOGGER.error("Log4j business method should always has three parameters, now is {}",
                        parameters.length);
                return;
            }
            String avroType = (String)parameters[0];
            String[] keysArray = (String[])parameters[1];
            Object[] paramsArray = (Object[])parameters[2];

            Schema avroSchema = avroSchemaMap.get(avroType);
            if(avroSchema == null)
            {
                LOGGER.error("Not found avro schema for {}", avroType);
                return;
            }

            ActionHistory action = businessHistory.newActionHistory(BUSINESS_TOPIC);
            action.setMsgTag(avroType);
            action.addActionData("avroName", avroType);
            action.addActionData("avroNamespace", avroSchema.getNamespace());

            try {
                action.addActionData("avroData", (AvroUtils.jsonEncodeAvro(avroSchema, keysArray, paramsArray)));
            } catch (IOException e) {
                LOGGER.error("Encode avro data failed for type {}", avroType);
            } catch (Exception ex) {
                LOGGER.error("Log business failed cause: {}", ex.getMessage(), ex);
            }
            action.complete();
        }
    }
}
