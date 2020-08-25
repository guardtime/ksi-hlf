package org.guardtime.ksi.hlf.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class FormatterCmd extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getLoggerName() + ": " + record.getMessage() + "\n";
    }
}