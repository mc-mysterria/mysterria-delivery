package net.mysterria.delivery.domain.delivery.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.mysterria.delivery.domain.delivery.model.QueuedDelivery;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class DeliveryLedger {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    private final Path file;
    private final Logger logger;
    private final State state;

    public DeliveryLedger(File dataFolder, Logger logger) {
        this.file = dataFolder.toPath().resolve("ledger.json");
        this.logger = logger;
        this.state = load(dataFolder.toPath());
        save();
        retireLegacyQueue(dataFolder.toPath());
    }

    public synchronized boolean isDelivered(String purchaseId) {
        return state.delivered.containsKey(purchaseId);
    }

    public synchronized boolean isQueued(String purchaseId) {
        return state.queued.containsKey(purchaseId);
    }

    public synchronized void queue(QueuedDelivery delivery) {
        state.queued.put(delivery.getPurchaseId(), delivery);
        save();
    }

    public synchronized List<QueuedDelivery> takeQueued(UUID playerUuid) {
        List<QueuedDelivery> taken = new ArrayList<>();
        state.queued.values().removeIf(delivery -> delivery.getPlayerUuid().equals(playerUuid) && taken.add(delivery));
        if (!taken.isEmpty()) {
            save();
        }
        return taken;
    }

    public synchronized void markDelivered(String purchaseId) {
        state.delivered.put(purchaseId, Instant.now().toString());
        save();
    }

    public synchronized void unmarkDelivered(String purchaseId) {
        state.delivered.remove(purchaseId);
        save();
    }

    public synchronized void markFailed(QueuedDelivery delivery, String reason) {
        delivery.setFailureReason(reason);
        state.failed.put(delivery.getPurchaseId(), delivery);
        save();
    }

    public synchronized int queuedCount() {
        return state.queued.size();
    }

    public synchronized List<QueuedDelivery> failed() {
        return List.copyOf(state.failed.values());
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("ledger.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save the delivery ledger", e);
        }
    }

    private State load(Path dataFolder) {
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                State loaded = GSON.fromJson(reader, State.class);
                return loaded != null ? loaded : new State();
            } catch (IOException | JsonParseException e) {
                throw new IllegalStateException("ledger.json is unreadable, fix or restore it before starting: " + e.getMessage(), e);
            }
        }

        State migrated = new State();
        Path legacyQueue = dataFolder.resolve("queue.json");
        if (Files.exists(legacyQueue)) {
            try (Reader reader = Files.newBufferedReader(legacyQueue, StandardCharsets.UTF_8)) {
                List<QueuedDelivery> queued = GSON.fromJson(reader, new TypeToken<List<QueuedDelivery>>() {}.getType());
                if (queued != null) {
                    queued.forEach(delivery -> migrated.queued.put(delivery.getPurchaseId(), delivery));
                }
                logger.info("Migrated " + migrated.queued.size() + " queued deliveries from queue.json");
            } catch (IOException | JsonParseException e) {
                throw new IllegalStateException("queue.json is unreadable, fix or remove it before starting: " + e.getMessage(), e);
            }
        }
        return migrated;
    }

    private void retireLegacyQueue(Path dataFolder) {
        Path legacyQueue = dataFolder.resolve("queue.json");
        if (Files.exists(legacyQueue)) {
            try {
                Files.move(legacyQueue, dataFolder.resolve("queue.json.migrated"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to retire queue.json", e);
            }
        }
    }

    @Data
    @NoArgsConstructor
    private static class State {
        private Map<String, QueuedDelivery> queued = new LinkedHashMap<>();
        private Map<String, String> delivered = new LinkedHashMap<>();
        private Map<String, QueuedDelivery> failed = new LinkedHashMap<>();
    }
}
