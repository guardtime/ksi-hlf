package org.guardtime.ksi.hlf.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import java.util.Date;

public class FormatterFile extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getLoggerName() + "::" + new Date(record.getMillis()) + ": " + record.getMessage() + "\n";
    }
}