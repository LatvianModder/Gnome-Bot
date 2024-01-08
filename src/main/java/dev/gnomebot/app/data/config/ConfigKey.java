package dev.gnomebot.app.data.config;

public record ConfigKey<T, H extends ConfigHolder<T>>(ConfigType<T, H> type, String id, String title, T defaultValue) {
}
