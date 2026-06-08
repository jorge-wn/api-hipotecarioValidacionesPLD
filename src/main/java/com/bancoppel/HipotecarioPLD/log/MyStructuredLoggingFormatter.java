package com.bancoppel.HipotecarioPLD.log;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MyStructuredLoggingFormatter implements StructuredLogFormatter<ILoggingEvent>  {

    private final JsonWriter<ILoggingEvent> writer = JsonWriter.<ILoggingEvent>of((members) -> {
 
        try {
            members.add("level", (event) -> event.getLevel());
            members.add("schemaVersion", "1.0.0");
            members.add("logType", (event) -> event.getMDCPropertyMap().get("logType"));
            members.add("sourceIP",  (event) -> event.getMDCPropertyMap().get("sourceIP"));
            members.add("status", (event) -> event.getMDCPropertyMap().get("status"));
            members.add("message", (event) -> event.getFormattedMessage());
            members.add("logOrigin", (event) -> event.getMDCPropertyMap().get("logOrigin")).whenNotEmpty();
            members.add("time", (event) -> event.getInstant());
            members.add("tracingId", (event) -> event.getMDCPropertyMap().get("tracingId")); /// TODO front end lo manda
            members.add("hostname", InetAddress.getLocalHost().getHostName());
            members.add("eventType", (event) -> event.getMDCPropertyMap().get("eventType"));
            members.add("application").usingMembers((application) -> {
                application.add("name", "HIPOTECARIOPLD");
                application.add("version", "1.0.0");
                application.add("env", "PROD");
                application.add("kind", "rest-service");
            });
            members.add("measurement").usingMembers((measurement) -> {
                measurement.add("method", (event) -> event.getMDCPropertyMap().get("method"));//event.getLoggerContextVO().getPropertyMap());
                measurement.add("elapsedTime", (event) -> event.getMDCPropertyMap().get("elapsedTime"));
            });
            members.add("destinationIP", (event) -> event.getMDCPropertyMap().get("destinationIP"));
            members.add("aditionalInfo", (event) -> event.getMDCPropertyMap().get("aditionalInfo"));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }).withNewLineAtEnd();

    @Override
    public String format(ILoggingEvent event) {
        return this.writer.writeToString(event);
    }
}