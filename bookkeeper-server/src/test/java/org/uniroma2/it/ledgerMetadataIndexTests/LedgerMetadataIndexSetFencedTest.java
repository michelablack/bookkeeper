package org.uniroma2.it.ledgerMetadataIndexTests;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.protobuf.ByteString;
import org.apache.bookkeeper.bookie.storage.ldb.DbLedgerStorageDataFormats.LedgerData;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorage;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorageFactory;
import org.apache.bookkeeper.bookie.storage.ldb.LedgerMetadataIndex;
import org.uniroma2.it.utils.LedgerMetadataBean;
import org.uniroma2.it.utils.MemoryAppender;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.bookkeeper.util.collections.ConcurrentLongHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.LoggerFactory;
import org.uniroma2.it.utils.MyRunnable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * fenced -> Fencing is a local one-way idempotent operation.
 * Once a bookie fenced one of its ledgers, that bookie will not accept
 * any further regular writes to that ledger ever again.
 **/
@RunWith(value = Parameterized.class)
public class LedgerMetadataIndexSetFencedTest {
    private static MemoryAppender memoryAppender;
    private Logger logger;

    private static final String MSG = "Re-inserted fenced ledger ";
    private static final String MSG1 = "Set fenced ledger ";

    private Thread thread;
    private LedgerMetadataIndex ledgerMetadataIndex;
    private String expectedLog;
    private Class<? extends Exception> expectedException;
    private long ledgerId;
    private ConcurrentLongHashMap<LedgerData> map;
    private List<Entry<byte[], byte[]>> list;

    public LedgerMetadataIndexSetFencedTest(ConcurrentLongHashMap<LedgerData> map,
                                               long ledgerId,
                                               String expectedLog){
        this.configure(map, ledgerId, expectedLog);
    }

    private void configure(ConcurrentLongHashMap<LedgerData> map,
                           long ledgerId,
                           String expectedLog){
        this.map = map;
        this.ledgerId = ledgerId;
        this.expectedLog = expectedLog;
        this.list = new ArrayList<>();
        for (long id: this.map.keys()){
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
     * - Setting Fenced to true to a ledger that is already fenced -> I expect the method to return null
     * - Setting Fenced to true to a ledger that is not Fenced, but that,
     * when I try to fence, is no more in the list -> I expect the fenced ledger to be inserted in the
     * ledgers list
     * - Setting Fenced to true to a ledger that is not Fenced ->
     * I expect the ledger to be updated as a fenced ledger.
     */
    @Parameters
    public static Collection<Object[]> getParameters() {

        //ledgerData with void string as masterKey
        LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(true)
                .setMasterKey(ByteString.copyFrom(new byte[0])).build();
        //ledgerData with valid string as masterKey
        LedgerData ledgerData1 = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom("1234".getBytes())).build();

        LedgerMetadataBean bean1 = new LedgerMetadataBean(1L, ledgerData);
        LedgerMetadataBean bean2 = new LedgerMetadataBean(-1L, ledgerData1);


        return Arrays.asList(new Object[][]{
                //minimal test set
                {bean1.getMap(), 1L, null},
                {bean2.getMap(), -1L, String.valueOf(-1L)}
        });
    }

    @Test
    public void setFenced() throws IOException {
        boolean result;

        result = this.ledgerMetadataIndex.setFenced(this.ledgerId);
        if (this.expectedLog==null) {
            assertFalse(result);
        }
        else {
            assertTrue(result);
            assert(memoryAppender.contains(MSG1+this.expectedLog, Level.DEBUG));
        }
    }

    /**@Test
    public void deleteTest() throws IOException {
        boolean result;
        Runnable r = new MyRunnable(this.ledgerMetadataIndex, this.ledgerId);
        this.thread = new Thread(r);
        this.thread.start();

        result = this.ledgerMetadataIndex.setFenced(this.ledgerId);
        if (this.expectedLog==null) {
            assertFalse(result);
        }
        else {
            assertTrue(result);
            //This means I've deleted the ledger before it could get fenced.
            //For this reason I need to reinsert it in ledgers list.
            assert(memoryAppender.contains(MSG+this.expectedLog, Level.DEBUG));
        }

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
