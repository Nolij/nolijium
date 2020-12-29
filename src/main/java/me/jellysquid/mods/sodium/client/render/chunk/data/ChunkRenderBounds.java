package me.jellysquid.mods.sodium.client.render.chunk.data;

import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderBounds {
    public static final ChunkRenderBounds ALWAYS_FALSE = new ChunkRenderBounds(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

    public final float x1, y1, z1;
    public final float x2, y2, z2;

    public ChunkRenderBounds(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;

        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    public ChunkRenderBounds(ChunkSectionPos origin) {
        this.x1 = origin.getMinX();
        this.y1 = origin.getMinY();
        this.z1 = origin.getMinZ();

        this.x2 = origin.getMaxX() + 1;
        this.y2 = origin.getMaxY() + 1;
        this.z2 = origin.getMaxZ() + 1;
    }

    public static class Builder {
        // Bit-mask of the blocks set on each axis
        private int x = 0, y = 0, z = 0;

        public void addBlock(int x, int y, int z) {
            // Accumulate bits on each axis for the given block position. This avoids needing to
            // branch multiple times trying to determine if the bounds need to grow. The min/max
            // value of each axis can later be extracted by counting the trailing/leading zeroes.
            this.x |= 1 << x;
            this.y |= 1 << y;
            this.z |= 1 << z;
        }

        public ChunkRenderBounds build(ChunkSectionPos origin) {
            // If no bits were set on any axis, return the default bounds
            if ((this.x | this.y | this.z) == 0) {
                return new ChunkRenderBounds(origin);
            }

            int x1 = origin.getMinX() + left(this.x);
            int x2 = origin.getMinX() + right(this.x);

            int y1 = origin.getMinY() + left(this.y);
            int y2 = origin.getMinY() + right(this.y);

            int z1 = origin.getMinZ() + left(this.z);
            int z2 = origin.getMinZ() + right(this.z);

            // Expand the bounding box by 8 blocks (half a chunk) in order to deal with diagonal surfaces
            return new ChunkRenderBounds(
                    Math.max(x1, origin.getMinX()) - 8.0f,
                    Math.max(y1, origin.getMinY()) - 8.0f,
                    Math.max(z1, origin.getMinZ()) - 8.0f,

                    Math.min(x2 + 1, origin.getMaxX()) + 8.0f,
                    Math.min(y2 + 1, origin.getMaxY()) + 8.0f,
                    Math.min(z2 + 1, origin.getMaxZ()) + 8.0f
            );
        }

        // Return the left-bound of the bit-masked axis
        private static int left(int i) {
            return Integer.numberOfTrailingZeros(i);
        }

        // Return the right-bound of the bit-masked axis
        private static int right(int i) {
            return Integer.numberOfTrailingZeros(i << 16);
        }
    }
}