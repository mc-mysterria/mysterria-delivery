package net.mysterria.delivery.domain.delivery.model.source;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ServiceCategory {
    ITEMS("items", "items"),
    SUBSCRIPTIONS("subscriptions", "subscriptions"),
    PERMISSIONS("permissions", "permissions"),
    DISCORD_ROLES("discord roles", "discord_roles"),
    DUNGEON_KEYS("dungeon keys", "dungeon_keys"),
    BATTLEPASS("battlepass", "battlepass"),
    COSMETICS("cosmetics", "cosmetics"),
    OTHER("other", "other"),
    FALLBACK("", "fallback");

    private final String backendName;
    private final String translationKey;

    public static ServiceCategory fromBackendName(String name) {
        return Arrays.stream(values())
                .filter(category -> category != FALLBACK && category.backendName.equalsIgnoreCase(name))
                .findFirst()
                .orElse(FALLBACK);
    }
}
