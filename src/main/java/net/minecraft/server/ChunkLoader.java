package net.minecraft.server;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChunkLoader implements IChunkLoader {

    private File a;
    private boolean b;

    // Async executors - only used when enabled in config
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService loadExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
    );

    // Chunk cache - only used when enabled in config
    private final Map<Long, Chunk> chunkCache = java.util.Collections.synchronizedMap(
            new LinkedHashMap<Long, Chunk>(256, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<Long, Chunk> e) { return size() > 256; }
            }
    );

    private long chunkKey(int i, int j) { return ((long) i << 32) | (j & 0xFFFFFFFFL); }

    public ChunkLoader(File file1, boolean flag) {
        this.a = file1;
        this.b = flag;
    }

    private File a(int i, int j) {
        String s = "c." + Integer.toString(i, 36) + "." + Integer.toString(j, 36) + ".dat";
        String s1 = Integer.toString(i & 63, 36);
        String s2 = Integer.toString(j & 63, 36);
        File file1 = new File(this.a, s1);

        if (!file1.exists()) {
            if (!this.b) return null;
            file1.mkdir();
        }

        file1 = new File(file1, s2);
        if (!file1.exists()) {
            if (!this.b) return null;
            file1.mkdir();
        }

        file1 = new File(file1, s);
        return !file1.exists() && !this.b ? null : file1;
    }

    // Async load — falls back to sync if disabled
    public CompletableFuture<Chunk> asyncLoad(World world, int i, int j) {
        if (!DonutConfig.getBoolean("async-chunk-load-enabled", true)) {
            return CompletableFuture.completedFuture(a(world, i, j));
        }

        long key = chunkKey(i, j);
        if (DonutConfig.getBoolean("chunk-cache-enabled", true) && chunkCache.containsKey(key)) {
            return CompletableFuture.completedFuture(chunkCache.get(key));
        }

        return CompletableFuture.supplyAsync(() -> a(world, i, j), loadExecutor);
    }

    public Chunk a(World world, int i, int j) {
        long key = chunkKey(i, j);

        // Check cache if enabled
        if (DonutConfig.getBoolean("chunk-cache-enabled", true) && chunkCache.containsKey(key)) {
            return chunkCache.get(key);
        }

        File file1 = this.a(i, j);

        if (file1 != null && file1.exists()) {
            try (BufferedInputStream fileinputstream = new BufferedInputStream(new FileInputStream(file1))) {
                NBTTagCompound nbttagcompound = CompressedStreamTools.a((InputStream) fileinputstream);

                if (!nbttagcompound.hasKey("Level")) {
                    System.out.println("Chunk file at " + i + "," + j + " is missing level data, skipping");
                    return null;
                }

                if (!nbttagcompound.k("Level").hasKey("Blocks")) {
                    System.out.println("Chunk file at " + i + "," + j + " is missing block data, skipping");
                    return null;
                }

                Chunk chunk = a(world, nbttagcompound.k("Level"));

                if (!chunk.a(i, j)) {
                    System.out.println("Chunk file at " + i + "," + j + " is in the wrong location; relocating. (Expected " + i + ", " + j + ", got " + chunk.x + ", " + chunk.z + ")");
                    nbttagcompound.a("xPos", i);
                    nbttagcompound.a("zPos", j);
                    chunk = a(world, nbttagcompound.k("Level"));
                }

                chunk.h();

                // Store in cache if enabled
                if (DonutConfig.getBoolean("chunk-cache-enabled", true)) {
                    chunkCache.put(key, chunk);
                }

                return chunk;
            } catch (Exception exception) {
                System.out.println("Failed to load chunk at " + i + "," + j);
                exception.printStackTrace();
            }
        }

        return null;
    }

    public void a(World world, Chunk chunk) {
        // Remove from cache if enabled
        if (DonutConfig.getBoolean("chunk-cache-enabled", true)) {
            chunkCache.remove(chunkKey(chunk.x, chunk.z));
        }

        if (DonutConfig.getBoolean("async-chunk-save-enabled", true)) {
            // Async save
            saveExecutor.submit(() -> doSave(world, chunk));
        } else {
            // Sync save — vanilla behaviour
            doSave(world, chunk);
        }
    }

    private void doSave(World world, Chunk chunk) {
        world.k();
        File file1 = this.a(chunk.x, chunk.z);

        if (file1.exists()) {
            WorldData worlddata = world.q();
            worlddata.b(worlddata.g() - file1.length());
        }

        try {
            File file2 = new File(this.a, "tmp_chunk_" + chunk.x + "_" + chunk.z + ".dat");
            try (BufferedOutputStream fileoutputstream = new BufferedOutputStream(new FileOutputStream(file2))) {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                nbttagcompound.a("Level", (NBTBase) nbttagcompound1);
                a(chunk, world, nbttagcompound1);
                CompressedStreamTools.a(nbttagcompound, (OutputStream) fileoutputstream);
            }

            if (file1.exists()) file1.delete();

            if (!file2.renameTo(file1)) {
                System.out.println("[Donut] Warning: rename failed for chunk " + chunk.x + "," + chunk.z);
            }

            WorldData worlddata1 = world.q();
            worlddata1.b(worlddata1.g() + file1.length());
        } catch (Exception exception) {
            System.out.println("[Donut] Failed to save chunk at " + chunk.x + "," + chunk.z);
            exception.printStackTrace();
        }
    }

    public static void a(Chunk chunk, World world, NBTTagCompound nbttagcompound) {
        world.k();
        nbttagcompound.a("xPos", chunk.x);
        nbttagcompound.a("zPos", chunk.z);
        nbttagcompound.setLong("LastUpdate", world.getTime());
        nbttagcompound.a("Blocks", chunk.b);
        nbttagcompound.a("Data", chunk.e.a);
        nbttagcompound.a("SkyLight", chunk.f.a);
        nbttagcompound.a("BlockLight", chunk.g.a);
        nbttagcompound.a("HeightMap", chunk.heightMap);
        nbttagcompound.a("TerrainPopulated", chunk.done);
        chunk.q = false;
        NBTTagList nbttaglist = new NBTTagList();

        Iterator iterator;
        NBTTagCompound nbttagcompound1;

        for (int i = 0; i < chunk.entitySlices.length; ++i) {
            iterator = chunk.entitySlices[i].iterator();
            while (iterator.hasNext()) {
                Entity entity = (Entity) iterator.next();
                chunk.q = true;
                nbttagcompound1 = new NBTTagCompound();
                if (entity.c(nbttagcompound1)) {
                    nbttaglist.a((NBTBase) nbttagcompound1);
                }
            }
        }

        nbttagcompound.a("Entities", (NBTBase) nbttaglist);
        NBTTagList nbttaglist1 = new NBTTagList();
        iterator = chunk.tileEntities.values().iterator();

        while (iterator.hasNext()) {
            TileEntity tileentity = (TileEntity) iterator.next();
            nbttagcompound1 = new NBTTagCompound();
            tileentity.b(nbttagcompound1);
            nbttaglist1.a((NBTBase) nbttagcompound1);
        }

        nbttagcompound.a("TileEntities", (NBTBase) nbttaglist1);
    }

    public static Chunk a(World world, NBTTagCompound nbttagcompound) {
        int i = nbttagcompound.e("xPos");
        int j = nbttagcompound.e("zPos");
        Chunk chunk = new Chunk(world, i, j);

        chunk.b = nbttagcompound.j("Blocks");
        chunk.e = new NibbleArray(nbttagcompound.j("Data"));
        chunk.f = new NibbleArray(nbttagcompound.j("SkyLight"));
        chunk.g = new NibbleArray(nbttagcompound.j("BlockLight"));
        chunk.heightMap = nbttagcompound.j("HeightMap");
        chunk.done = nbttagcompound.m("TerrainPopulated");

        if (!chunk.e.a()) {
            chunk.e = new NibbleArray(chunk.b.length);
        }

        if (chunk.heightMap == null || !chunk.f.a()) {
            chunk.heightMap = new byte[256];
            chunk.f = new NibbleArray(chunk.b.length);
            chunk.initLighting();
        }

        if (!chunk.g.a()) {
            chunk.g = new NibbleArray(chunk.b.length);
            chunk.a();
        }

        NBTTagList nbttaglist = nbttagcompound.l("Entities");
        if (nbttaglist != null) {
            for (int k = 0; k < nbttaglist.c(); ++k) {
                NBTTagCompound nbttagcompound1 = (NBTTagCompound) nbttaglist.a(k);
                Entity entity = EntityTypes.a(nbttagcompound1, world);
                chunk.q = true;
                if (entity != null) chunk.a(entity);
            }
        }

        NBTTagList nbttaglist1 = nbttagcompound.l("TileEntities");
        if (nbttaglist1 != null) {
            for (int l = 0; l < nbttaglist1.c(); ++l) {
                NBTTagCompound nbttagcompound2 = (NBTTagCompound) nbttaglist1.a(l);
                TileEntity tileentity = TileEntity.c(nbttagcompound2);
                if (tileentity != null) chunk.a(tileentity);
            }
        }

        return chunk;
    }

    public void a() {}

    public void b() {
        saveExecutor.shutdown();
        loadExecutor.shutdown();
        try {
            saveExecutor.awaitTermination(30, TimeUnit.SECONDS);
            loadExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void b(World world, Chunk chunk) {}
}
