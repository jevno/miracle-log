package com.miracle.common.log.utils;

public class AvroNotSupportException extends RuntimeException {
    public AvroNotSupportException(String message)
    {
        super(message);
    }

    public AvroNotSupportException(String message, Throwable t)
    {
        super(message, t);
    }
}
