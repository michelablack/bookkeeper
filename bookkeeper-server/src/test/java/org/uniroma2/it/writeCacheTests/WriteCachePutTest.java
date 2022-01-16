package org.uniroma2.it.writeCacheTests;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.bookkeeper.bookie.storage.ldb.WriteCache;
import org.uniroma2.it.utils.WriteCacheBean;
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

import static org.junit.Assert.assertEquals;

@RunWith(value=Parameterized.class)
public class WriteCachePutTest {
    private static final ByteBufAllocatorBuilder builder = new ByteBufAllocatorBuilderImpl();
    private static final int MAX_SEGMENT_SIZE = 64;
    private static final int REPEATED_TIMES = 8;
    private WriteCache writeCache;
    private WriteCacheBean bean;
    private boolean expectedResult;
    private long expectedNum;


    public WriteCachePutTest(long ledgerId, long entryId, String entryValue, int maxCacheSize,
                             boolean reinsert, boolean expectedResult, long expectedNum){
        ByteBufAllocator allocator = builder.build();
        ByteBuf entry = null;
        if (entryValue != null)
            entry = allocator.buffer(maxCacheSize).writeBytes(entryValue.getBytes());
        WriteCacheBean bean = new WriteCacheBean(ledgerId, entryId, entry, maxCacheSize, reinsert);
        this.configure(bean, expectedResult, expectedNum);
    }

    private void configure(WriteCacheBean bean, boolean expectedResult, long expectedNum) {
        this.bean = bean;
        this.expectedResult = expectedResult;
        this.expectedNum = expectedNum;
    }

    @Before
    public void setUp(){
        writeCache = new WriteCache(builder.build(), this.bean.getMaxCacheSize(), MAX_SEGMENT_SIZE);
    }


    @Parameters
    public static Collection<Object[]> getParameters() {
        char[] fill = new char[8];
        Arrays.fill(fill, 'v');
        String VALID_WORD = String.valueOf(fill);
        String REPEATED = new String(new char[REPEATED_TIMES]).replace("\0", VALID_WORD);

        return Arrays.asList(new Object[][] {
                //minimal test set
                {-1L, 0, null, 80, false, false, 0},
                {0, 1L, VALID_WORD, 80, false, true, 1L},
                {1, -1L, REPEATED+VALID_WORD, 80, true, false, 0},

                //increase coverage
                {1L, 2L, REPEATED, 160, true, true, 2L},
        });
    }


    @Test
    public void put() {
        boolean result;
        long num;
        try {
            result = writeCache.put(this.bean.getLedgerId(), this.bean.getEntryId(), this.bean.getEntry());
            if (bean.isReinsert()){
                result = writeCache.put(this.bean.getLedgerId(), this.bean.getEntryId()-1, this.bean.getEntry());
            }
        } catch (Exception e) {
            result = false;
        }
        num = writeCache.count();
        assertEquals(this.expectedResult, result);
        assertEquals(this.expectedNum, num);
    }

    @After
    public void tearDown(){
        writeCache.clear();
        writeCache.close();
    }
}