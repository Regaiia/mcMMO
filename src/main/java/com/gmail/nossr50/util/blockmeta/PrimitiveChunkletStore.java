package com.gmail.nossr50.util.blockmeta;

public class PrimitiveChunkletStore implements ChunkletStore {
    private static final long serialVersionUID = -3453078050608607478L;

    /** X, Z, Y */
    private boolean[][][] store = new boolean[16][16][64];

    public boolean isTrue(int x, int y, int z) {
        return store[x][z][y];
    }

    public void setTrue(int x, int y, int z) {
        store[x][z][y] = true;
    }

    public void setFalse(int x, int y, int z) {
        store[x][z][y] = false;
    }

    public boolean isEmpty() {
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = 0; y < 64; y++) {
                    if(store[x][z][y]) return false;
                }
            }
        }
        return true;
    }
}
