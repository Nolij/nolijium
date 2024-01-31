package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is fired to allow some control over the chunk render data before it is finalized.
 */
@ApiStatus.Internal
public class ChunkDataBuiltEvent {
    private final BuiltSectionInfo.Builder dataBuilder;

    public ChunkDataBuiltEvent(BuiltSectionInfo.Builder dataBuilder) {
        this.dataBuilder = dataBuilder;
    }

    public BuiltSectionInfo.Builder getDataBuilder() {
        return this.dataBuilder;
    }
}
