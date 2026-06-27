package motd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MOTD implements DedicatedServerModInitializer {
	public static final String MOD_ID = "motd";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = Path.of("config");
	private static final Path CONFIG_FILE = CONFIG_DIR.resolve("motd.json");
	private static Config CONFIG;

	@Override
	public void onInitializeServer() {
		loadConfig();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (CONFIG == null) return;
			if (CONFIG.expiresAt != null && Instant.now().isAfter(CONFIG.expiresAt)) return;
			String msg = buildMessage();
			handler.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg), false);
		});
		LOGGER.info("MOTD loaded");
	}

	private static String buildMessage() {
		StringBuilder sb = new StringBuilder();

		if (CONFIG.header != null && !CONFIG.header.isBlank()) {
			sb.append(CONFIG.header.trim()).append("\n\n");
		}

		if (CONFIG.dateTimeFormat != null && !CONFIG.dateTimeFormat.isBlank()) {
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern(CONFIG.dateTimeFormat);
			sb.append(LocalDateTime.now(ZoneId.systemDefault()).format(fmt)).append("\n\n");
		}

		for (String entry : CONFIG.entries) {
			if (entry != null && !entry.isBlank()) {
				sb.append("- ").append(entry.trim()).append("\n");
			}
		}

		return sb.toString().stripTrailing();
	}

	private static void loadConfig() {
		try {
			if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
			if (!Files.exists(CONFIG_FILE)) {
				CONFIG = Config.defaults();
				saveConfig();
				return;
			}
			try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
				CONFIG = GSON.fromJson(reader, Config.class);
			}
			if (CONFIG == null) CONFIG = Config.defaults();
			if (CONFIG.entries == null) CONFIG.entries = new ArrayList<>();
			if (CONFIG.expiresAt == null && CONFIG.durationHours > 0) {
				CONFIG.expiresAt = Instant.now().plus(Duration.ofHours(CONFIG.durationHours));
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load config", e);
			CONFIG = Config.defaults();
		}
	}

	private static void saveConfig() throws Exception {
		try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
			GSON.toJson(CONFIG, writer);
		}
	}

	static class Config {
		String header;
		String dateTimeFormat;
		int durationHours;
		Instant expiresAt;
		List<String> entries;

		static Config defaults() {
			Config c = new Config();
			c.header = "Hallo,";
			c.dateTimeFormat = "dd.MM.yyyy HH:mm";
			c.durationHours = 24;
			c.expiresAt = Instant.now().plus(Duration.ofHours(24));
			c.entries = new ArrayList<>();
			c.entries.add("MOTD Mod hinzugefügt");
			c.entries.add("Inventory Totem hinzugefügt");
			return c;
		}
	}
}
