package com.prisma.tessera.pack.server;

import com.prisma.tessera.TesseraPlugin;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class PackServer {

    private final TesseraPlugin plugin;
    private HttpServer httpServer;
    private ExecutorService executor;

    private String host;
    private int port;
    private String domain;
    private String packUrl;
    private String sha1Hex;
    private byte[] sha1Bytes;
    private UUID packUUID;
    private File packFile;

    private boolean rateLimitEnabled;
    private int maxRequestsPerIp;
    private long rateWindowMillis;
    private final Map<String, RateWindow> ipRequests = new ConcurrentHashMap<>();

    /** Download count for one IP within the current cooldown window. */
    private record RateWindow(long startedAt, AtomicInteger count) {
    }

    public PackServer(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("pack.server.enabled", true)) {
            plugin.getLogger().info("[PackServer] Resource pack HTTP server is disabled in config.");
            return;
        }

        stop();

        this.host = plugin.getConfig().getString("pack.server.host", "0.0.0.0");
        this.port = plugin.getConfig().getInt("pack.server.port", 8085);
        this.domain = plugin.getConfig().getString("pack.server.domain", "localhost:" + port);
        this.rateLimitEnabled = plugin.getConfig().getBoolean("pack.server.rate_limit.enabled", true);
        this.maxRequestsPerIp = plugin.getConfig().getInt("pack.server.rate_limit.max_requests_per_ip", 5);
        this.rateWindowMillis = Math.max(1, plugin.getConfig().getInt("pack.server.rate_limit.cooldown_minutes", 15)) * 60_000L;

        this.packFile = new File(plugin.getDataFolder(), "tessera-pack.zip");
        if (!packFile.exists()) {
            plugin.getLogger().warning("[PackServer] Resource pack file does not exist yet. It will be served once generated.");
        } else {
            calculateHash(packFile);
        }

        this.packUrl = "http://" + domain + "/tessera-pack.zip";

        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            this.executor = Executors.newFixedThreadPool(4);
            this.httpServer.setExecutor(executor);

            HttpHandler packHandler = exchange -> {
                String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

                if (rateLimitEnabled) {
                    // Counters reset after cooldown_minutes; without that an IP stays blocked until restart.
                    long now = System.currentTimeMillis();
                    RateWindow window = ipRequests.compute(clientIp, (ip, old) ->
                            old == null || now - old.startedAt() >= rateWindowMillis ? new RateWindow(now, new AtomicInteger()) : old);
                    if (window.count().incrementAndGet() > maxRequestsPerIp) {
                        String rateLimitMsg = "Rate limit exceeded. Too many download requests.\n";
                        exchange.sendResponseHeaders(429, rateLimitMsg.getBytes().length);
                        exchange.getResponseBody().write(rateLimitMsg.getBytes());
                        exchange.getResponseBody().close();
                        return;
                    }
                }

                if (!packFile.exists()) {
                    String notFoundMsg = "Tessera resource pack not generated yet.\n";
                    exchange.sendResponseHeaders(404, notFoundMsg.getBytes().length);
                    exchange.getResponseBody().write(notFoundMsg.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                try {
                    byte[] bytes = Files.readAllBytes(packFile.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"tessera-pack.zip\"");
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(bytes.length));
                    if (sha1Hex != null) {
                        exchange.getResponseHeaders().set("X-Checksum-SHA1", sha1Hex);
                    }
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().close();
                } catch (IOException e) {
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                }
            };

            HttpHandler statusHandler = exchange -> {
                String response = """
                        ======================================
                        Tessera Resource Pack Distribution Server
                        Version: %s
                        Pack URL: %s
                        SHA-1: %s
                        Pack Status: %s
                        ======================================
                        """.formatted(
                        plugin.getDescription().getVersion(),
                        packUrl,
                        sha1Hex != null ? sha1Hex : "Not computed",
                        packFile.exists() ? "Ready (" + packFile.length() + " bytes)" : "Missing"
                );
                byte[] bytes = response.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            };

            this.httpServer.createContext("/tessera-pack.zip", packHandler);
            this.httpServer.createContext("/pack.zip", packHandler);
            this.httpServer.createContext("/", statusHandler);

            this.httpServer.start();
            plugin.getLogger().info("[PackServer] Resource pack HTTP server started on " + host + ":" + port);
            plugin.getLogger().info("[PackServer] Serving pack at: " + packUrl);
        } catch (IOException e) {
            plugin.getLogger().severe("[PackServer] Failed to start embedded HTTP server: " + e.getMessage());
        }
    }

    public void updatePack(File file) {
        this.packFile = file;
        calculateHash(file);
    }

    public void calculateHash(File file) {
        if (!file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            this.sha1Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : sha1Bytes) {
                sb.append(String.format("%02x", b));
            }
            this.sha1Hex = sb.toString();
            this.packUUID = UUID.nameUUIDFromBytes(sha1Bytes);
            plugin.getLogger().info("[PackServer] Resource pack SHA-1: " + sha1Hex);
        } catch (Exception e) {
            plugin.getLogger().warning("[PackServer] Failed to compute pack SHA-1 hash: " + e.getMessage());
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        ipRequests.clear();
    }

    public String getPackUrl() {
        
        String externalUrl = plugin.getConfig().getString("pack.server.external_url", "");
        if (externalUrl != null && !externalUrl.isBlank()) {
            return externalUrl;
        }
        return packUrl;
    }

    public String getSha1Hex() {
        return sha1Hex;
    }

    public byte[] getSha1Bytes() {
        return sha1Bytes;
    }

    public UUID getPackUUID() {
        return packUUID;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return httpServer != null;
    }
}
