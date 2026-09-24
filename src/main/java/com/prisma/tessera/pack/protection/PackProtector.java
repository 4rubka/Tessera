package com.prisma.tessera.pack.protection;

import com.prisma.tessera.TesseraPlugin;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class PackProtector {

    private static final byte[] LOCAL_HEADER_MAGIC = {0x50, 0x4B, 0x03, 0x04}; 
    private static final byte[] CENTRAL_HEADER_MAGIC = {0x50, 0x4B, 0x01, 0x02}; 
    private static final byte[] ZERO_BYTES = {0, 0, 0, 0};
    private static final byte[] CORRUPT_ATTRS = {0x05, 0x39};

    private final TesseraPlugin plugin;

    public PackProtector(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    public void protect(File zipFile) {
        if (!zipFile.exists() || zipFile.length() == 0) {
            return;
        }

        boolean enabled = plugin.getConfig().getBoolean("pack.protection.protect_from_unzip", true);
        if (!enabled) {
            plugin.getLogger().info("[PackProtector] Resource pack unzip protection is disabled in config.");
            return;
        }

        int level = plugin.getConfig().getInt("pack.protection.level", 2);
        plugin.getLogger().info("[PackProtector] Applying resource pack protection (Level " + level + ")...");

        try {
            switch (level) {
                case 1 -> applyLevel1(zipFile);
                case 2 -> {
                    applyLevel1(zipFile);
                    applyLevel2(zipFile);
                }
                case 3 -> {
                    applyLevel1(zipFile);
                    applyLevel2(zipFile);
                    applyLevel3(zipFile);
                }
                default -> applyLevel2(zipFile);
            }
            plugin.getLogger().info("[PackProtector] Resource pack protected successfully!");
        } catch (Exception e) {
            plugin.getLogger().warning("[PackProtector] Could not apply full zip protection: " + e.getMessage());
        }
    }

    private void applyLevel1(File zipFile) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(zipFile, "rw")) {
            byte[] buffer = new byte[8192];
            long currentPos = 0;
            long fileLength = raf.length();

            while (currentPos < fileLength) {
                int read = raf.read(buffer);
                if (read == -1) break;
                long chunkStart = raf.getFilePointer() - read;

                for (int i = 0; i <= read - 30; i++) {
                    if (buffer[i] == LOCAL_HEADER_MAGIC[0] &&
                        buffer[i + 1] == LOCAL_HEADER_MAGIC[1] &&
                        buffer[i + 2] == LOCAL_HEADER_MAGIC[2] &&
                        buffer[i + 3] == LOCAL_HEADER_MAGIC[3]) {

                        raf.seek(chunkStart + i + 14);
                        raf.write(ZERO_BYTES);
                        raf.seek(chunkStart + i + 18);
                        raf.write(ZERO_BYTES);
                        raf.seek(chunkStart + i + 22);
                        raf.write(ZERO_BYTES);

                        i += 29;
                    }
                }
                currentPos += read;
                if (chunkStart + read - 4 < fileLength) {
                    raf.seek(chunkStart + read - 4);
                }
            }
        }
    }

    private void applyLevel2(File zipFile) throws Exception {
        byte[] traversalPrefix = createTraversalMask(255);

        try (RandomAccessFile raf = new RandomAccessFile(zipFile, "rw")) {
            byte[] buffer = new byte[8192];
            long currentPos = 0;
            long fileLength = raf.length();

            while (currentPos < fileLength) {
                int read = raf.read(buffer);
                if (read == -1) break;
                long chunkStart = raf.getFilePointer() - read;

                for (int i = 0; i <= read - 30; i++) {
                    if (buffer[i] == LOCAL_HEADER_MAGIC[0] &&
                        buffer[i + 1] == LOCAL_HEADER_MAGIC[1] &&
                        buffer[i + 2] == LOCAL_HEADER_MAGIC[2] &&
                        buffer[i + 3] == LOCAL_HEADER_MAGIC[3]) {

                        int versionNeeded = (buffer[i + 4] & 0xFF) | ((buffer[i + 5] & 0xFF) << 8);
                        int nameLength = (buffer[i + 26] & 0xFF) | ((buffer[i + 27] & 0xFF) << 8);

                        if (versionNeeded >= 10 && versionNeeded <= 63 && nameLength > 0 && nameLength <= 260) {
                            if (i + 30 + nameLength <= read) {
                                raf.seek(chunkStart + i + 30);
                                int writeLen = Math.min(traversalPrefix.length, nameLength);
                                raf.write(traversalPrefix, 0, writeLen);
                                for (int j = writeLen; j < nameLength; j++) {
                                    raf.write(0);
                                }
                                i += 30 + nameLength;
                            }
                        }
                    }
                }
                currentPos += read;
                if (chunkStart + read - 4 < fileLength) {
                    raf.seek(chunkStart + read - 4);
                }
            }
        }
    }

    private void applyLevel3(File zipFile) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(zipFile, "rw")) {
            byte[] buffer = new byte[8192];
            long currentPos = 0;
            long fileLength = raf.length();

            while (currentPos < fileLength) {
                int read = raf.read(buffer);
                if (read == -1) break;
                long chunkStart = raf.getFilePointer() - read;

                for (int i = 0; i <= read - 46; i++) {
                    if (buffer[i] == CENTRAL_HEADER_MAGIC[0] &&
                        buffer[i + 1] == CENTRAL_HEADER_MAGIC[1] &&
                        buffer[i + 2] == CENTRAL_HEADER_MAGIC[2] &&
                        buffer[i + 3] == CENTRAL_HEADER_MAGIC[3]) {

                        raf.seek(chunkStart + i + 34);
                        raf.write(CORRUPT_ATTRS);
                        i += 45;
                    }
                }
                currentPos += read;
                if (chunkStart + read - 4 < fileLength) {
                    raf.seek(chunkStart + read - 4);
                }
            }
        }
    }

    private static byte[] createTraversalMask(int length) {
        byte[] mask = new byte[length];
        Arrays.fill(mask, (byte) 0x5C); 
        if (length > 3) {
            mask[0] = '.';
            mask[1] = '.';
            mask[2] = '/';
        }
        return mask;
    }
}
