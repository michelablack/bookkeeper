package org.apache.bookkeeper.bookie.storage.ldb;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.bookkeeper.bookie.storage.ldb.bean.WriteCacheBean;
import org.apache.bookkeeper.common.allocator.ByteBufAllocatorBuilder;
import org.apache.bookkeeper.common.allocator.impl.ByteBufAllocatorBuilderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(value=Parameterized.class)
public class WriteCacheGetTest {
    private static final ByteBufAllocatorBuilder builder = new ByteBufAllocatorBuilderImpl();
    private WriteCacheBean bean;
    private WriteCache writeCache;
    private final ByteBuf expectedResult;
    private final  ByteBuf expectedLastEntry;

    public WriteCacheGetTest(WriteCacheBean bean, ByteBuf expectedResult, ByteBuf expectedLastEntry) {
        this.bean = bean;
        this.expectedResult = expectedResult;
        this.expectedLastEntry = expectedLastEntry;
    }

    @Before
    public void setUp() throws Exception {
        writeCache = new WriteCache(builder.build(),this.bean.getMaxCacheSize(), this.bean.getMaxSegmentSize());
    }

    @Parameters
    public static Collection<Object[]> getParameters() {
        ByteBufAllocator allocator = builder.build();
        int segments = 64;
        int capacity = segments+segments/4;
        String VALID_WORD = "valid!!!";
        String REPEATED = new String(new char[segments/VALID_WORD.length()]).replace("\0", VALID_WORD);
        return Arrays.asList(new Object[][]{
                //minimal test set
                {new WriteCacheBean(-1, 0, null, capacity, segments, false), null, null},
                {new WriteCacheBean(0, -1, null, capacity, segments, false), null, null},
                {new WriteCacheBean(0, 1, null, capacity, segments, false), null, null},

                /**{new WriteCacheBean(-1, 0), false},
                {new WriteCacheBean(0, -1), false},
                {new WriteCacheBean(1, 1), false},*/

                {new WriteCacheBean(1, 2, allocator.buffer(capacity).writeBytes((REPEATED).getBytes()),
                        capacity, segments, false), allocator.buffer(capacity).writeBytes((REPEATED).getBytes()),
                        allocator.buffer(capacity).writeBytes((REPEATED).getBytes())},
                {new WriteCacheBean(2, 3, allocator.buffer(capacity).writeBytes((REPEATED).getBytes()),
                        capacity, segments, true), allocator.buffer(capacity).writeBytes((REPEATED).getBytes()),
                        allocator.buffer(capacity).writeBytes((REPEATED).getBytes())},
        });
    }


    @Test
    public void get() {
        ByteBuf buf;
        ByteBuf lastEntry = null;
        boolean result;
        if (this.bean.getEntryId()>0 && this.bean.getLedgerId()>0){
            System.out.println(writeCache.put(this.bean.getLedgerId(), this.bean.getEntryId(), this.bean.getEntry()));
            if (this.bean.isReinsert()) System.out.println(writeCache.put(this.bean.getLedgerId(),
                    this.bean.getEntryId()+1, this.bean.getEntry()));
            //System.out.println(this.bean.getEntry());
        }
        try {
            buf = writeCache.get(this.bean.getLedgerId(), this.bean.getEntryId());
            lastEntry=buf;
            if (this.bean.isReinsert()) lastEntry = writeCache.getLastEntry(this.bean.getLedgerId());
            System.out.println(writeCache.get(this.bean.getLedgerId(), this.bean.getEntryId()));
            System.out.println(lastEntry);

        } catch (Exception e) {
            buf = null;
        }
        assertEquals(buf, this.expectedResult);
        assertEquals(lastEntry, this.expectedLastEntry);
    }

    @After
    public void tearDown() throws Exception {
        writeCache.clear();
        writeCache.close();
    }
}