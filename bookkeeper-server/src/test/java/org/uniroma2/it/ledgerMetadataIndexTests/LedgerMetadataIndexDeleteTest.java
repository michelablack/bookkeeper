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
public class LedgerMetadataIndexDeleteTest {
    private LedgerMetadataIndex ledgerMetadataIndex;
    private static MemoryAppender memoryAppender;
    private Logger logger;
    private static final String MSG = "Removed ledger ";

    private List<Entry<byte[], byte[]>> list;
    private boolean expectedResult;
    private LedgerMetadataBean beanSet;

    public LedgerMetadataIndexDeleteTest(LedgerMetadataBean beanList,
                                         LedgerMetadataBean beanSet,
                                         boolean expectedResult){
        this.configure(beanList, beanSet, expectedResult);
    }

    private void configure(LedgerMetadataBean beanList,
                           LedgerMetadataBean beanSet,
                           boolean expectedResult){
        this.beanSet = beanSet;
        this.expectedResult = expectedResult;
        this.list = new ArrayList<>();
        if (beanList!=null) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            Entry<byte[], byte[]> entry = newEntry(buffer.putLong(beanList.getLedgerId()).array(),
                    beanList.getLedger().toByteArray());
            this.list.add(entry);
        }
    }


    @Before
    public void setUp() throws IOException {
        logger = (Logger) LoggerFactory.getLogger(LedgerMetadataIndex.class);
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
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
     * Minimal test cases:
     * - If the list is empty and I try to delete a certain ledger -> I expect the deletion
     * not to be allowed
     * - If the list doesn't contain a ledger and I try to delete it -> I expect the deletion
     * not to be allowed
     * - If the list contain a ledger and I try to delete it -> I expect to be able to remove it
     */
    @Parameters
    public static Collection<Object[]> getParameters() {

        LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom("123".getBytes())).build();
        LedgerData ledgerData1 = LedgerData.newBuilder().setExists(false).setFenced(false)
                .setMasterKey(ByteString.copyFrom("\0".getBytes())).build();

        LedgerMetadataBean bean1 = new LedgerMetadataBean(1L, ledgerData);
        LedgerMetadataBean bean2 = new LedgerMetadataBean(-1L, ledgerData1);
        LedgerMetadataBean bean3 = new LedgerMetadataBean(0, ledgerData1);

        bean3.union(bean1);

        return Arrays.asList(new Object[][]{
                //minimal test set
                {null, bean1, false},
                {bean3, bean3, true},
                {bean1, bean2, false},


        });
    }

    @Test
    public void deleteTest() throws IOException {
        boolean result;
        this.ledgerMetadataIndex.delete(this.beanSet.getLedgerId());
        result = memoryAppender.contains(MSG+this.beanSet.getLedgerId(), Level.DEBUG);
        assertEquals(this.expectedResult, result);

    }

    @After
    public void cleanEnv(){
        this.list.clear();
        memoryAppender.stop();
        this.logger.detachAppender(memoryAppender);
        memoryAppender.reset();
    }
}
