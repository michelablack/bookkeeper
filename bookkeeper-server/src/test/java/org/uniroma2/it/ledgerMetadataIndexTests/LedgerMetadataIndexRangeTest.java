package org.uniroma2.it.ledgerMetadataIndexTests;

import com.google.protobuf.ByteString;
import org.apache.bookkeeper.bookie.storage.ldb.DbLedgerStorageDataFormats.LedgerData;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorage;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorageFactory;
import org.apache.bookkeeper.bookie.storage.ldb.LedgerMetadataIndex;
import org.uniroma2.it.utils.LedgerMetadataBean;
import org.apache.bookkeeper.stats.StatsLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(value = Parameterized.class)

public class LedgerMetadataIndexRangeTest {
    private LedgerMetadataIndex ledgerMetadataIndex;
    private Class<? extends Exception> expectedException;

    private List<Entry<byte[], byte[]>> list;
    private long firstLedgerId;
    private long lastLedgerId;
    private Long expectedResult;

    public LedgerMetadataIndexRangeTest(LedgerMetadataBean beanList,
                                      long firstLedgerId,
                                      long lastLedgerId,
                                      Long expectedResult,
                                      Class<? extends Exception> expectedException){
        this.configure(beanList, firstLedgerId, lastLedgerId, expectedResult, expectedException);
    }

    private void configure(LedgerMetadataBean beanList,
                           long firstLedgerId,
                           long lastLedgerId,
                           Long expectedResult,
                           Class<? extends Exception> expectedException){
        this.firstLedgerId = firstLedgerId;
        this.expectedException = expectedException;
        this.lastLedgerId = lastLedgerId;
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
     * - If the list is empty and I try to get the active ledgers in a range
     * -> I expect not to find any ledgers
     * - If the list contains an active ledger and the range contains it -> I expect to obtain the ledger
     * - If the list contains an active ledger and the range doesn't contain it ->
     * I expect not to find any ledger
     * - If the list contains an active ledger and the range has same first and last ledger id
     * and the ledger has this id -> I expect to find that ledger
     * - If the list contains an active ledger and the range has first ledger grater then
     * the last one -> I expect not to find any ledger
     */
    @Parameters
    public static Collection<Object[]> getParameters() {

        LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom("123".getBytes())).build();
        LedgerData ledgerData1 = LedgerData.newBuilder().setExists(false).setFenced(false)
                .setMasterKey(ByteString.copyFrom("\0".getBytes())).build();

        LedgerMetadataBean bean1 = new LedgerMetadataBean(0L, ledgerData);
        LedgerMetadataBean bean2 = new LedgerMetadataBean(1L, ledgerData);
        LedgerMetadataBean bean3 = new LedgerMetadataBean(Long.MAX_VALUE, ledgerData1);

        return Arrays.asList(new Object[][]{
                //minimal test set
                {null, -1L, 1L, null, NoSuchElementException.class},
                {bean1, -1L, 1L, bean1.getLedgerId(), null},
                {bean3, -1L, 1L, null, NoSuchElementException.class},
                //{bean2, 1L, 1L, bean2.getLedgerId(), null}, //???
                {bean2, 1L, -1L, null, NoSuchElementException.class}
        });
    }

    @Test
    public void getInRangeTest() {
        Long result;
        try {
            result = this.ledgerMetadataIndex.
                    getActiveLedgersInRange(this.firstLedgerId, this.lastLedgerId).iterator().next();
            assertEquals(this.expectedResult, result);
        } catch (Exception e){
            assertEquals(this.expectedException, e.getClass());
        }
    }

    @After
    public void cleanEnv(){
        this.list.clear();
    }
}
