package com.minieessential.feature.screenshot;

import com.minieessential.MiniEssential;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Feature 2 - Offline Screenshot Manager: backend.
 *
 * Reads .minecraft/screenshots strictly from the local game directory.
 * No network, no remote services.
 */
public class ScreenshotManager {
    /** Local screenshots folder: .minecraft/screenshots */
    public static final Path SCREENSHOTS_DIR = MinecraftClient.getInstance().runDirectory.resolve("screenshots");

    /** A single screenshot entry. */
    public record ScreenshotEntry(Path path, String name, long lastModified, long size) {}

    /** List all .png screenshots, newest first. */
    public List<ScreenshotEntry> listScreenshots() {
        List<ScreenshotEntry> entries = new ArrayList<>();
        try {
            if (!Files.isDirectory(SCREENSHOTS_DIR)) {
                Files.createDirectories(SCREENSHOTS_DIR);
                return entries;
            }
            try (Stream<Path> stream = Files.list(SCREENSHOTS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                      .forEach(p -> {
                          try {
                              BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                              entries.add(new ScreenshotEntry(
                                      p,
                                      p.getFileName().toString(),
                                      attr.lastModifiedTime().toMillis(),
                                      attr.size()
                              ));
                          } catch (IOException e) {
                              MiniEssential.LOGGER.warn("Failed to read screenshot attributes: {}", p, e);
                          }
                      });
            }
        } catch (IOException e) {
            MiniEssential.LOGGER.warn("Failed to list screenshots in {}", SCREENSHOTS_DIR, e);
        }
        entries.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return entries;
    }

    /** Delete a screenshot file. */
    public void deleteScreenshot(Path path) throws IOException {
        Files.deleteIfExists(path);
    }
}
