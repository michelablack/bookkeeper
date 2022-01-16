package org.uniroma2.it.utils;

import io.netty.buffer.ByteBuf;

public class WriteCacheBean {
    private long ledgerId;
    private long entryId;
    private ByteBuf entry;
    private int maxCacheSize;
    private int maxSegmentSize;
    private boolean reinsert;


    public WriteCacheBean(long ledgerId, long entryId) {
        this.ledgerId = ledgerId;
        this.entryId = entryId;
    }

    public WriteCacheBean(long ledgerId, long entryId, ByteBuf entry, int maxCacheSize,
                          int maxSegmentSize, boolean reinsert) {
        this.ledgerId = ledgerId;
        this.entryId = entryId;
        this.entry = entry;
        this.maxCacheSize = maxCacheSize;
        this.maxSegmentSize = maxSegmentSize;
        this.reinsert = reinsert;
    }

    public WriteCacheBean(long ledgerId, long entryId, ByteBuf entry, int maxCacheSize, boolean reinsert) {
        this.ledgerId = ledgerId;
        this.entryId = entryId;
        this.entry = entry;
        this.maxCacheSize = maxCacheSize;
        this.reinsert = reinsert;
    }

    public long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public long getEntryId() {
        return entryId;
    }

    public void setEntryId(long entryId) {
        this.entryId = entryId;
    }

    public ByteBuf getEntry() {
        return entry;
    }

    public void setEntry(ByteBuf entry) {
        this.entry = entry;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    public int getMaxSegmentSize() {
        return maxSegmentSize;
    }

    public void setMaxSegmentSize(int maxSegmentSize) {
        this.maxSegmentSize = maxSegmentSize;
    }

    public boolean isReinsert() {
        return reinsert;
    }

}
