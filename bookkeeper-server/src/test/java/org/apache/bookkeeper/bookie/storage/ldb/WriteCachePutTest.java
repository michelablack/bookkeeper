package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.bookkeeper.bookie.storage.ldb.bean.WriteCacheBean;
import org.apache.bookkeeper.common.allocator.ByteBufAllocatorBuilder;
import org.apache.bookkeeper.common.allocator.impl.ByteBufAllocatorBuilderImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(value=Parameterized.class)
public class WriteCachePutTest {
    private static final ByteBufAllocatorBuilder builder = new ByteBufAllocatorBuilderImpl();
    private WriteCache writeCache;
    private WriteCacheBean bean;
    private final boolean expectedResult;
    private final long expectedNum;


    @Before
    public void setUp() throws Exception {
        writeCache = new WriteCache(builder.build(), this.bean.getMaxCacheSize(), this.bean.getMaxSegmentSize());

    }

    @Parameters
    public static Collection<Object[]> getParameters() {
        ByteBufAllocator allocator = builder.build();
        int segments = 64;
        int capacity = segments+segments/4;
        String VALID_WORD = "valid!!!";
        String INVALID_WORD = "invalid_word!!!!!";
        String REPEATED = new String(new char[segments/VALID_WORD.length()]).replace("\0", VALID_WORD);
        return Arrays.asList(new Object[][] {
                //minimal test set
                {new WriteCacheBean(-1, 0, null, capacity, segments, false), false, 0},
                {new WriteCacheBean(0, 1, allocator.buffer().setBytes(0,VALID_WORD.getBytes()),
                        capacity, segments, false), true, 1},
                {new WriteCacheBean(1, -1, allocator.buffer().setBytes(0,(REPEATED+VALID_WORD).getBytes()),
                        capacity, segments, true), false, 0},
                //coverage
                {new WriteCacheBean(1, 2, allocator.buffer(capacity).writeBytes(REPEATED.getBytes()),
                        capacity*2, segments, true), true, 2},
                {new WriteCacheBean(2, 2, allocator.buffer(capacity).writeBytes((REPEATED+VALID_WORD).getBytes()),
                        capacity*2, segments, true), false, 0},
                {new WriteCacheBean(1, 1, allocator.buffer(capacity).writeBytes(REPEATED.getBytes()),
                        capacity, segments, false), true, 1},
                {new WriteCacheBean(2, 2, allocator.buffer(capacity).writeBytes((REPEATED+VALID_WORD).getBytes()),
                        capacity, (int) segments, true), false, 0},
        });
    }

    public WriteCachePutTest(WriteCacheBean bean, boolean expectedResult, long expectedNum){
        this.bean = bean;
        this.expectedResult = expectedResult;
        this.expectedNum = expectedNum;
    }

    @Test
    public void put() {
        boolean result;
        long num;
        try {
            result = writeCache.put(this.bean.getLedgerId(), this.bean.getEntryId(), this.bean.getEntry());
            if (bean.isReinsert()){
                result = writeCache.put(this.bean.getLedgerId(), this.bean.getEntryId()+1, this.bean.getEntry());
            }
        } catch (Exception e) {
            result = false;
        }
        num = writeCache.count();
        assertEquals(result, this.expectedResult);
        assertEquals(num, this.expectedNum);
    }

    @After
    public void tearDown() throws Exception {
        writeCache.clear();
        writeCache.close();
    }


}