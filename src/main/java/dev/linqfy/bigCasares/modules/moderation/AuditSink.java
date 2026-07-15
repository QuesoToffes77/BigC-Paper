package dev.linqfy.bigCasares.modules.moderation;

@FunctionalInterface
public interface AuditSink {

    void publish(AuditEvent event);
}
