package dev.linqfy.bigCasares.modules.auth;

import dev.linqfy.bigCasares.communication.CredentialCommandSanitizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.beans.PropertyChangeListener;

/** Suppresses Paper's pre-preprocess command log for Auth credential labels. */
final class CredentialCommandLogGuard implements AutoCloseable {
    static final String PAPER_LOGGER = "net.minecraft.server.network.ServerGamePacketListenerImpl";
    private static final String COMMAND_FORMAT = "{} issued server command: {}";

    private final LoggerContext context;
    private final Filter filter = new Filter();
    private final PropertyChangeListener listener = event -> {
        if (LoggerContext.PROPERTY_CONFIG.equals(event.getPropertyName())) {
            installFilter();
        }
    };
    private LoggerConfig installed;
    private boolean closed;

    private CredentialCommandLogGuard(LoggerContext context) {
        this.context = context;
    }

    static CredentialCommandLogGuard install() {
        if (!(LogManager.getContext(false) instanceof LoggerContext context)) {
            throw new IllegalStateException("Auth requires Log4j Core to protect credential command logs");
        }
        CredentialCommandLogGuard guard = new CredentialCommandLogGuard(context);
        context.addPropertyChangeListener(guard.listener);
        try {
            guard.installFilter();
        } catch (RuntimeException | Error failure) {
            guard.close();
            throw failure;
        }
        return guard;
    }

    private synchronized void installFilter() {
        if (closed) {
            return;
        }
        LoggerConfig target = context.getConfiguration().getLoggerConfig(PAPER_LOGGER);
        if (installed == target) {
            return;
        }
        if (installed != null) {
            installed.removeFilter(filter);
        }
        target.addFilter(filter);
        installed = target;
    }

    @Override
    public synchronized void close() {
        closed = true;
        context.removePropertyChangeListener(listener);
        if (installed != null) {
            installed.removeFilter(filter);
            installed = null;
        }
    }

    static final class Filter extends AbstractFilter {
        @Override
        public Result filter(LogEvent event) {
            if (!PAPER_LOGGER.equals(event.getLoggerName())) {
                return Result.NEUTRAL;
            }
            Message message = event.getMessage();
            if (message == null || !COMMAND_FORMAT.equals(message.getFormat())) {
                return Result.NEUTRAL;
            }
            Object[] parameters = message.getParameters();
            if (parameters == null || parameters.length < 2 || !(parameters[1] instanceof String command)) {
                return Result.NEUTRAL;
            }
            return command.stripLeading().startsWith("/") && CredentialCommandSanitizer.isCredentialCommand(command)
                ? Result.DENY : Result.NEUTRAL;
        }
    }

}
