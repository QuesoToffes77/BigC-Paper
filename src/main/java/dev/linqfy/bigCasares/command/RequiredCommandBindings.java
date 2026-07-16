package dev.linqfy.bigCasares.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class RequiredCommandBindings {

    private RequiredCommandBindings() {
    }

    public static <T> List<T> resolve(Function<String, T> resolver, List<String> commandNames) {
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(commandNames, "commandNames");
        List<T> commands = new ArrayList<>(commandNames.size());
        for (String commandName : commandNames) {
            String normalizedName = Objects.requireNonNull(commandName, "commandName").trim();
            if (normalizedName.isEmpty()) {
                throw new IllegalArgumentException("commandName must not be blank");
            }
            T command = resolver.apply(normalizedName);
            if (command == null) {
                throw new IllegalStateException("Required command is not declared: " + normalizedName);
            }
            commands.add(command);
        }
        return List.copyOf(commands);
    }
}
