package dev.linqfy.bigCasares.modules.moderation.anticheat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Bounded selection from one current online-player snapshot. */
final class ScanBatch {
    private ScanBatch() { }

    static <T> List<T> select(List<T> players, int start, int limit) {
        int count = Math.min(players.size(), Math.max(0, limit));
        List<T> selected = new ArrayList<>(count);
        if (count == 0) return selected;
        int first = Math.floorMod(start, players.size());
        for (int i = 0; i < count; i++) selected.add(players.get((first + i) % players.size()));
        return selected;
    }

    static <T> List<T> select(List<T> players, int start, int limit, Random random) {
        int count = Math.min(players.size(), Math.max(0, limit));
        if (count == 0) return new ArrayList<>();
        if (count == players.size()) return select(players, start, count);
        int stable = Math.max(1, count / 2);
        List<T> selected = select(players, start, stable);
        List<T> remainder = new ArrayList<>(players.size() - stable);
        int first = Math.floorMod(start, players.size());
        for (int i = stable; i < players.size(); i++) remainder.add(players.get((first + i) % players.size()));
        Collections.shuffle(remainder, random);
        selected.addAll(remainder.subList(0, count - stable));
        return selected;
    }
}
