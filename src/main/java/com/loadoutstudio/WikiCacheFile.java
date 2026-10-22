/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import net.runelite.client.util.Filepath;

/** Public Wiki responses only. Called on the Wiki worker, never on the client/EDT. */
final class WikiCacheFile implements WikiClient.Store
{
    interface Directory { Filepath get() throws IOException; }
    private final Directory directory;
    WikiCacheFile(Directory directory) { this.directory=directory; }
    WikiCacheFile(Filepath directory) { this(()->directory); }
    public String load() throws IOException
    {
        Filepath file = directory.get().join("wiki-cache-v1.jsonl");
        if (!file.exists()) return "";
        if (file.size() > 8_000_000) throw new IOException("Wiki cache exceeds its size limit.");
        try (InputStream in = file.openInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) { if (out.size() + count > 8_000_000) throw new IOException("Wiki cache exceeds its size limit."); out.write(buffer, 0, count); }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    public void save(String data) throws IOException
    {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 8_000_000) throw new IOException("Wiki cache exceeds its size limit.");
        Filepath root=directory.get();root.createDirectories(); Filepath temporary=root.createTempFile("wiki-cache-", ".tmp");
        try
        {
            temporary.write(bytes);
            try{temporary.moveTo(root.join("wiki-cache-v1.jsonl"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){temporary.moveTo(root.join("wiki-cache-v1.jsonl"),StandardCopyOption.REPLACE_EXISTING);}
        }
        finally{temporary.deleteIfExists();}
    }
}
