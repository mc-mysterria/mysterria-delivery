package net.mysterria.delivery;

import net.mysterria.delivery.domain.delivery.model.QueuedDelivery;
import net.mysterria.delivery.domain.delivery.service.DeliveryLedger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryLedgerTest {

    private static final Logger LOGGER = Logger.getLogger("test");
    private static final UUID PLAYER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void migratesTheLegacyQueueWithoutTurningNumbersIntoDecimals(@TempDir Path folder) throws Exception {
        Files.writeString(folder.resolve("queue.json"), """
                [{"purchaseId":"p-1","playerUuid":"%s","playerName":"steve",
                  "purchaseRequest":{"purchaseId":"p-1","metadata":{"commands":["emporium give {player} {reward}"],"reward":100}},
                  "queuedAt":"2026-01-01T10:00:00","retryCount":0}]
                """.formatted(PLAYER));

        DeliveryLedger ledger = new DeliveryLedger(folder.toFile(), LOGGER);

        assertTrue(ledger.isQueued("p-1"));
        assertTrue(Files.exists(folder.resolve("ledger.json")));
        assertFalse(Files.exists(folder.resolve("queue.json")));
        List<QueuedDelivery> taken = ledger.takeQueued(PLAYER);
        assertEquals("100", taken.getFirst().getPurchaseRequest().metadataValue("reward"));
    }

    @Test
    void deliveredPurchasesSurviveARestartAndQueuedOnesAreTakenOnce(@TempDir Path folder) {
        DeliveryLedger ledger = new DeliveryLedger(folder.toFile(), LOGGER);
        ledger.markDelivered("p-2");
        ledger.queue(QueuedDelivery.builder().purchaseId("p-3").playerUuid(PLAYER).build());

        DeliveryLedger restarted = new DeliveryLedger(folder.toFile(), LOGGER);
        assertTrue(restarted.isDelivered("p-2"));
        assertEquals(1, restarted.takeQueued(PLAYER).size());
        assertEquals(0, restarted.takeQueued(PLAYER).size());
        assertFalse(new DeliveryLedger(folder.toFile(), LOGGER).isQueued("p-3"));
    }
}
