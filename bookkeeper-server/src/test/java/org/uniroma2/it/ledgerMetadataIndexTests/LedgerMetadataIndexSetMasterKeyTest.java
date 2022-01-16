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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(value = Parameterized.class)
public class LedgerMetadataIndexSetMasterKeyTest {
    private static MemoryAppender memoryAppender;
    private Logger logger;

    private static final String[] MSG = {"Inserting new ledger "};
    private static final String[] MSG1 = {"Replace old master key "," with new master key " };
    private static final String[] MSG2 = {"Ledger ", " masterKey in db can only be set once."};

    private LedgerMetadataIndex ledgerMetadataIndex;
    private LedgerMetadataBean bean;
    private String expectedLog;
    private byte[] masterKey;
    private Class<? extends Exception> expectedException;
    private long ledgerId;
    private ConcurrentLongHashMap<LedgerData> map;
    private List<Entry<byte[], byte[]>> list;

    public LedgerMetadataIndexSetMasterKeyTest(ConcurrentLongHashMap<LedgerData> map,
                                   long ledgerId,
                                   byte[] masterKey,
                                   String expectedLog,
                                   boolean exception){
        this.configure(map, ledgerId, masterKey, expectedLog,  exception);
    }

    private void configure(ConcurrentLongHashMap<LedgerData> map,
                           long ledgerId,
                           byte[] masterKey,
                           String expectedLog,
                           boolean exception){
        this.map = map;
        this.ledgerId = ledgerId;
        this.expectedLog = expectedLog;
        this.masterKey = masterKey;
        this.list = new ArrayList<>();
        for (long id: this.map.keys()){
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            Entry<byte[], byte[]> entry = newEntry(buffer.putLong(id).array(),
                    this.map.get(id).toByteArray());
            this.list.add(entry);
        }
        if (exception) {
            this.expectedException = IOException.class;
        }
    }


    @Before
    public void setUp() throws IOException {
        logger = (Logger) LoggerFactory.getLogger(LedgerMetadataIndex.class);
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(memoryAppender);
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
     * - Setting Master Key of a ledger that is not in the list -> I expect the ledger to be created
     * and the MasterKey to be inserted;
     * - Updating the MasterKey of a ledger that is inside the list, but whom MasterKey flag has not
     * been set -> I expect the ledger MasterKey to be updated
     * - Updating the MasterKey of a ledger that is inside the list, but whom MasterKey flag has been set ->
     * I expect the ledger MasterKey to NOT be updated and to throw an exception, because MasterKey in a DB
     * has to be unique for that ledger
     *
     * @return
     */
    @Parameters
    public static Collection<Object[]> getParameters() {

        //ledgerData with void string as masterKey
        LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom(new byte[0])).build();
        //ledgerData with valid string as masterKey
        LedgerData ledgerData1 = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom("1234".getBytes())).build();

        LedgerMetadataBean bean1 = new LedgerMetadataBean(0, ledgerData);
        LedgerMetadataBean bean2 = new LedgerMetadataBean(-1L, ledgerData);
        LedgerMetadataBean bean3 = new LedgerMetadataBean(1L, ledgerData1);


        return Arrays.asList(new Object[][]{
                //minimal test set
                {bean1.getMap(), 1L, new byte[0], MSG[0]+1L, false},
                {bean2.getMap(), -1L, "1234".getBytes(), MSG1[0]+ Arrays.toString(new byte[0]) +
                        MSG1[1]+ Arrays.toString("1234".getBytes()), false},
                {bean3.getMap(), 1L, "4321".getBytes(), MSG2[0]+1L+MSG2[1], true},
        });
    }

    @Test
    public void setMasterKey(){
        try {
            this.ledgerMetadataIndex.setMasterKey(this.ledgerId, this.masterKey);
            assert(memoryAppender.contains(this.expectedLog, Level.DEBUG));
        } catch (Exception e) {
            assert(memoryAppender.contains(this.expectedLog, Level.WARN));
            assertEquals(this.expectedException, e.getClass());
        }

    }

    @After
    public void cleanEnv(){
        this.list.clear();
        memoryAppender.stop();
        this.logger.detachAppender(memoryAppender);
        memoryAppender.reset();
    }
}
