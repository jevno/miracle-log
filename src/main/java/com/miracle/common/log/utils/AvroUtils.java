package com.miracle.common.log.utils;

import com.google.common.base.Joiner;
import com.miracle.common.miracle_utils.BeanUtils;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AvroUtils
{
    private static boolean isAvroBaseDataType(Class clazz)
    {
        return
                (
                        clazz.equals(String.class) ||
                        clazz.equals(Integer.class)||
                        clazz.equals(Long.class) ||
                        clazz.equals(Double.class) ||
                        clazz.equals(Float.class) ||
                        clazz.equals(Boolean.class) ||
                        clazz.isPrimitive()
                );
    }

    private static boolean isArray(Object obj)
    {
        if (null == obj) {
            return false;
        }
        return obj.getClass().isArray();
    }

    private static boolean isList(Object obj) {
        if (null == obj) {
            return false;
        }
        return Collection.class.isAssignableFrom(obj.getClass());
    }

    private static boolean isMap(Object obj) {
        if (null == obj) {
            return false;
        }
        return Map.class.isAssignableFrom(obj.getClass());
    }

    public static byte[] recordBinaryEncoder(GenericRecord record)
            throws IOException {
        byte[] serializedValue;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().binaryEncoder(bos, null);
            GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(
                    record.getSchema());
            writer.write(record, encoder);
            encoder.flush();
            serializedValue = bos.toByteArray();
            bos.close();
        } catch (IOException ex) {
            throw new IOException("Failed to write the Avro GenericRecord",
                    ex);
        }

        return serializedValue;
    }

    public static String recordJsonEncoder(GenericRecord record)
            throws IOException {
        String retString;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(),
                    bos);
            GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(
                    record.getSchema());
            writer.write(record, jsonEncoder);
            jsonEncoder.flush();
            retString = bos.toString();
            bos.close();
        } catch (IOException ex) {
            throw new IOException("Failed to serialize the Avro GenericRecord",
                    ex);
        }
        return retString;
    }

    private static void fillGenericRecord(Schema schema, GenericRecord record,
                                          String key, Object obj)
    {
        if(obj == null) {
            return;
        }
        if(isAvroBaseDataType(obj.getClass()))
        {
            if(schema.getField(key) != null)
            {
                record.put(key, obj);
            }
        }
        else if(isArray(obj))
        {
            if(schema.getField(key) != null)
            {
                String value = Joiner.on(',').join((Object[]) obj);
                record.put(key, value);
            }
        }
        else if(isList(obj))
        {
            if(schema.getField(key) != null)
            {
                String value = Joiner.on(',').join((Collection)obj);
                record.put(key, value);
            }
        }
        else if(isMap(obj))
        {
            try {
                Map<String, Object> childDataMap = (Map<String, Object>) obj;
                if (childDataMap.size() > 0) {
                    Set<String> subKeys = childDataMap.keySet();
                    for (String subKey : subKeys) {
                        Object subVal = childDataMap.get(subKey);
                        String newKey = key + "_" + subKey;
                        fillGenericRecord(schema, record, newKey, subVal);
                    }
                }
            }
            catch (Throwable t)
            {
                throw new AvroNotSupportException("avro map data failed", t);
            }
        }
        else
        {
            Map<String, Object> beanAttrMap = BeanUtils.beanToMap(obj);
            Set<String> subKeys = beanAttrMap.keySet();
            for(String subKey : subKeys)
            {
                Object subVal = beanAttrMap.get(subKey);
                String newKey = key + "_" + subKey;
                fillGenericRecord(schema, record, newKey, subVal);
            }
        }
    }

    public static byte[] binaryEncodeAvro(Schema schema, String[] keysArray,
                                    Object[] paramsArray) throws IOException {
        GenericRecord record = new GenericData.Record(schema);
        if(keysArray != null && paramsArray != null)
        {
            int idx = 0;
            for(String key : keysArray)
            {
                fillGenericRecord(schema, record, key, paramsArray[idx++]);
            }
        }
        return recordBinaryEncoder(record);
    }

    public static String jsonEncodeAvro(Schema schema, String[] keysArray,
                                        Object[] paramsArray) throws IOException {
        GenericRecord record = new GenericData.Record(schema);
        if(keysArray != null && paramsArray != null)
        {
            int idx = 0;
            for(String key : keysArray)
            {
                fillGenericRecord(schema, record, key, paramsArray[idx++]);
            }
        }
        return recordJsonEncoder(record);
    }
}
