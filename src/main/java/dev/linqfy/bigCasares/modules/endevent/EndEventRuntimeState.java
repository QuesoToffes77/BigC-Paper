package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record EndEventRuntimeState(
    boolean delayedReveal,
    Optional<EndEventLocation> portal,
    Optional<EndEventLocation> eggBlock,
    Set<UUID> optedInOperators,
    Optional<EndEventBorderSnapshot> originalBorder,
    Map<UUID, String> originalGameModes,
    Map<UUID, List<ItemStack>> escrow,
    Optional<UUID> winner,
    Optional<Instant> banAt
) {

    public EndEventRuntimeState {
        portal = portal == null ? Optional.empty() : portal;
        eggBlock = eggBlock == null ? Optional.empty() : eggBlock;
        optedInOperators = optedInOperators == null
            ? Set.of()
            : Set.copyOf(new LinkedHashSet<>(optedInOperators));
        originalBorder = originalBorder == null ? Optional.empty() : originalBorder;
        originalGameModes = originalGameModes == null
            ? Map.of()
            : Map.copyOf(new LinkedHashMap<>(originalGameModes));
        escrow = copyEscrow(escrow);
        winner = winner == null ? Optional.empty() : winner;
        banAt = banAt == null ? Optional.empty() : banAt;
    }

    public static EndEventRuntimeState empty() {
        return new EndEventRuntimeState(
            false, Optional.empty(), Optional.empty(), Set.of(), Optional.empty(),
            Map.of(), Map.of(), Optional.empty(), Optional.empty()
        );
    }

    private static Map<UUID, List<ItemStack>> copyEscrow(Map<UUID, List<ItemStack>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<ItemStack>> copy = new LinkedHashMap<>();
        source.forEach((id, items) -> {
            List<ItemStack> itemCopies = new ArrayList<>();
            if (items != null) {
                items.forEach(item -> itemCopies.add(item == null ? null : item.clone()));
            }
            copy.put(id, List.copyOf(itemCopies));
        });
        return Map.copyOf(copy);
    }
}
