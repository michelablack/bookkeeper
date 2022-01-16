package org.uniroma2.it.writeCacheTests;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.bookkeeper.bookie.storage.ldb.WriteCache;
import org.apache.bookkeeper.common.allocator.ByteBufAllocatorBuilder;
import org.apache.bookkeeper.common.allocator.impl.ByteBufAllocatorBuilderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@RunWith(value=Parameterized.class)
public class WriteCacheGetTest {
    private static final ByteBufAllocatorBuilder builder = new ByteBufAllocatorBuilderImpl();
    private static final int MAX_SEGMENT_SIZE = 64;
    private static final int MAX_CACHE_SIZE = 80;
    private Logger logger;
    private long ledgerId;
    private long entryId;
    private WriteCache writeCache;
    private ByteBuf expectedResult;
    private ByteBuf entry;

    public WriteCacheGetTest(long ledgerId, long entryId, String entryValue, String expectedValue) {
        ByteBufAllocator allocator = builder.build();
        ByteBuf entry = null;
        ByteBuf expectedResult = null;
        if (entryValue != null)
            entry = allocator.buffer(MAX_CACHE_SIZE).writeBytes(entryValue.getBytes());
        if (expectedValue != null)
            expectedResult = allocator.buffer(MAX_CACHE_SIZE).writeBytes(expectedValue.getBytes());
        this.configure(ledgerId, entryId, entry, expectedResult);
    }

    private void configure(long ledgerId, long entryId, ByteBuf entry, ByteBuf expectedResult) {
        this.logger = Logger.getLogger("WRITE");
        this.ledgerId = ledgerId;
        this.entryId = entryId;
        this.entry = entry;
        this.expectedResult = expectedResult;
    }


    @Before
    public void setUp() {
        writeCache = new WriteCache(builder.build(),MAX_CACHE_SIZE, MAX_SEGMENT_SIZE);
    }

    @Parameters
    public static Collection<Object[]> getParameters() {
        char[] fill = new char[8];
        Arrays.fill(fill, 'v');
        String VALID_WORD = String.valueOf(fill);

        return Arrays.asList(new Object[][]{
                //minimal test set
                {-1L, 0, null, null},
                {0, -1L, VALID_WORD, null},
                {1L, 1L, VALID_WORD, VALID_WORD},

        });
    }

    @Test
    public void get() {
        try {
            writeCache.put(this.ledgerId, this.entryId, this.entry);
        } catch (Exception e){
            assertNull(this.expectedResult);
            this.logger.log(Level.SEVERE, "Failed to put in cache\n");
        }
        ByteBuf result = null;
        try {
            result = writeCache.get(this.ledgerId, this.entryId);
        } catch (Exception e){
            assertNull(this.expectedResult);
            this.logger.log(Level.SEVERE, "Failed to get from cache\n");
        }
        assertEquals(this.expectedResult, result);
    }

    // Test done in order to increase coverage
    @Test
    public void getDoublePut() {
        try {
            writeCache.put(this.ledgerId+1L, this.entryId+1L, this.entry);
            writeCache.put(this.ledgerId, this.entryId, this.entry);
        } catch (Exception e){
            assertNull(this.expectedResult);
            this.logger.log(Level.SEVERE, "Failed to put in cache\n");
        }
        ByteBuf result = null;
        try {
            result = writeCache.get(this.ledgerId, this.entryId);
        } catch (Exception e){
            assertNull(this.expectedResult);
            this.logger.log(Level.SEVERE, "Failed to get from cache\n");
        }
        assertEquals(this.expectedResult, result);
    }


    @After
    public void tearDown() throws Exception {
        writeCache.clear();
        writeCache.close();
    }
}