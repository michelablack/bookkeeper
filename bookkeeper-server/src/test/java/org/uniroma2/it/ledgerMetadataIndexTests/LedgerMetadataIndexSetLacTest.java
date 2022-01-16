package org.uniroma2.it.ledgerMetadataIndexTests;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.bookkeeper.bookie.storage.ldb.DbLedgerStorageDataFormats.LedgerData;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorage;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorageFactory;
import org.apache.bookkeeper.bookie.storage.ldb.LedgerMetadataIndex;
import org.mockito.Mockito;
import org.uniroma2.it.utils.LedgerMetadataBean;
import org.uniroma2.it.utils.MemoryAppender;
import org.uniroma2.it.utils.MyRunnable;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.bookkeeper.util.collections.ConcurrentLongHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * LAC -> Last Add Confirmed. It's the highest entry that was committed by the client.
 * The LastEntryId gets put to this value when a ledger is closed, and it's the
 * end of the ledger.
 * Explicit LAC -> It has been introduced to periodically advance LAC if there are not
 * subsequent entries added.
 */
@RunWith(value = Parameterized.class)
public class LedgerMetadataIndexSetLacTest {
    private static MemoryAppender memoryAppender;
    private Logger logger;

    private static final String MSG = "Set explicitLac on ledger ";

    private LedgerMetadataIndex ledgerMetadataIndex;
    private LedgerMetadataBean bean;
    private String expectedLog;
    private Class<? extends Exception> expectedException;
    private long ledgerId;
    private Thread thread;
    private ByteBuf bb;
    private ConcurrentLongHashMap<LedgerData> map;
    private List<Entry<byte[], byte[]>> list;

    public LedgerMetadataIndexSetLacTest(ConcurrentLongHashMap<LedgerData> map,
                                            long ledgerId,
                                            byte[] bb,
                                            String expectedLog){
        this.configure(map, ledgerId, bb, expectedLog);
    }

    private void configure(ConcurrentLongHashMap<LedgerData> map,
                           long ledgerId,
                           byte[] bb,
                           String expectedLog){
        this.map = map;
        this.bb = Unpooled.copiedBuffer(bb);
        this.ledgerId = ledgerId;
        this.expectedLog = expectedLog;
        this.list = new ArrayList<>();
        for (long id : this.map.keys()) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            Entry<byte[], byte[]> entry = newEntry(buffer.putLong(id).array(),
                    this.map.get(id).toByteArray());
            this.list.add(entry);
        }
    }


    @Before
    public void setUp() throws IOException {
        logger = (Logger) LoggerFactory.getLogger(LedgerMetadataIndex.class);
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(memoryAppender);
        logger.setLevel(Level.DEBUG);
        memoryAppender.start();

        KeyValueStorageFactory keyValueFactoryInterface = mock(KeyValueStorageFactory.class);
        KeyValueStorage keyValueInterface = mock(KeyValueStorage.class);
        StatsLogger statsLogger = mock(StatsLogger.class);
        ledgerMetadataIndex = LedgerMetadataIndexImpl.
                createMetadataIndex(this.list, keyValueFactoryInterface, keyValueInterface, statsLogger);
    }

    private static Entry<byte[], byte[]> newEntry(byte[] key, byte[] value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    /**
     * Minimal cases of tests:
     * - Setting LAC to a ledger that already has -> I expect the LAC to be updated,
     * because it means that a new entry has been added
     * - Setting LAC to a ledger that has not, but that,
     * when I try to add to, is no more in the list -> It means that the LAC for
     * that ledger was already set, so I don't need to set anything.
     * - Setting LAC to a ledger that has not ->
     * I expect LAC to be set for that ledger.
     */
    @Parameters
    public static Collection<Object[]> getParameters() {

        //ledgerData with void string as explicitLac set
        LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom(new byte[0]))
                .setExplicitLac(ByteString.copyFrom("\0".getBytes())).build();
        //ledgerData with no explicitLac set
        LedgerData ledgerData1 = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom(new byte[0])).build();

        LedgerMetadataBean bean1 = new LedgerMetadataBean(0, ledgerData);
        LedgerMetadataBean bean2 = new LedgerMetadataBean(-1L, ledgerData1);


        return Arrays.asList(new Object[][]{
                //minimal test set
                {bean1.getMap(), 0, new byte[0], String.valueOf(0)},
                {bean2.getMap(), -1L, "VALID".getBytes(), String.valueOf(-1L)},

                //kill mutation
                {bean2.getMap(), 1L, "VALID".getBytes(), null}
        });
    }

    @Test
    public void setExplicitLacTest() throws IOException {
        this.ledgerMetadataIndex.setExplicitLac(this.ledgerId, this.bb);
        if (this.ledgerId<=0)
            assert (memoryAppender.contains(MSG + this.expectedLog, Level.DEBUG));
    }

    /**@Test
    public void deleteTest() throws IOException {
        Runnable r = new MyRunnable(this.ledgerMetadataIndex, this.ledgerId);
        this.thread= new Thread(r);
        this.thread.start();

        this.ledgerMetadataIndex.setExplicitLac(this.ledgerId, this.bb);
        assert(!memoryAppender.contains(MSG+this.expectedLog, Level.DEBUG));

        this.thread.interrupt();
    }*/

    @After
    public void cleanEnv(){
        this.list.clear();
        memoryAppender.stop();
        this.logger.detachAppender(memoryAppender);
        memoryAppender.reset();

    }
}
