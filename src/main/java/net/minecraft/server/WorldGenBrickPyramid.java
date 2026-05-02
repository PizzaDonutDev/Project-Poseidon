package net.minecraft.server;

import java.util.Random;

public class WorldGenBrickPyramid extends WorldGenerator {

    private static final int PYRAMID_HEIGHT = 20;
    private static final int BRICK_BLOCK        = 45;
    private static final int COBBLE_BLOCK       = 4;
    private static final int MOSSY_COBBLE_BLOCK = 48;

    @Override
    public boolean a(World world, Random rand, int x, int y, int z) {
        if (!DonutConfig.getBoolean("pyramids-enabled", true)) return false;
        int startY = y - 8;
        int baseRadius = PYRAMID_HEIGHT - 1;

        // Force load every chunk the pyramid will touch
        int minChunkX = (x - baseRadius) >> 4;
        int maxChunkX = (x + baseRadius) >> 4;
        int minChunkZ = (z - baseRadius) >> 4;
        int maxChunkZ = (z + baseRadius) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                world.chunkProvider.getOrCreateChunk(cx, cz);
            }
        }

        // Place blocks
        for (int layer = 0; layer < PYRAMID_HEIGHT; layer++) {
            int blockY = startY + layer;
            if (blockY < 0) continue;
            if (blockY > 127) break;

            int radius = (PYRAMID_HEIGHT - 1) - layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int blockId;
                    int roll = rand.nextInt(10);
                    if (roll < 6) {
                        blockId = BRICK_BLOCK;        // 60% brick
                    } else if (roll < 9) {
                        blockId = COBBLE_BLOCK;       // 30% cobblestone
                    } else {
                        blockId = MOSSY_COBBLE_BLOCK; // 10% mossy cobblestone
                    }
                    world.setTypeId(x + dx, blockY, z + dz, blockId);
                }
            }
        }

        return true;
    }
}
