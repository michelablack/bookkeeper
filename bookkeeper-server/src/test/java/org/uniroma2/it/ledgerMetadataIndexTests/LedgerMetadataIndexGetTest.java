package org.uniroma2.it.ledgerMetadataIndexTests;

import com.google.protobuf.ByteString;
import org.apache.bookkeeper.bookie.Bookie;
import org.apache.bookkeeper.bookie.storage.ldb.DbLedgerStorageDataFormats.LedgerData;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorage;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorageFactory;
import org.apache.bookkeeper.bookie.storage.ldb.LedgerMetadataIndex;
import org.uniroma2.it.utils.LedgerMetadataBean;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.bookkeeper.util.collections.ConcurrentLongHashMap;
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
public class LedgerMetadataIndexGetTest {

    private LedgerMetadataIndex ledgerMetadataIndex;
    private LedgerData expectedResult;
    private Class<? extends Exception> expectedException;
    private long ledgerId;
    private ConcurrentLongHashMap<LedgerData> map;
    private List<Entry<byte[], byte[]>> list;

    public LedgerMetadataIndexGetTest(ConcurrentLongHashMap<LedgerData> map,
                                      long ledgerId,
                                      LedgerData expectedResult,
                                      boolean exception){
        this.configure(map, ledgerId, expectedResult, exception);
    }

    private void configure(ConcurrentLongHashMap<LedgerData> map,
                           long ledgerId,
                           LedgerData expectedResult,
                           boolean exception){
        this.map = map;
        this.ledgerId = ledgerId;
        this.expectedResult = expectedResult;
        this.list = new ArrayList<>();
        for (long id: this.map.keys()){
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            Entry<byte[], byte[]> entry = newEntry(buffer.putLong(id).array(),
                        this.map.get(id).toByteArray());
            this.list.add(entry);
        }
        if (exception) {
            this.expectedException = Bookie.NoLedgerException.class;
            System.out.println(this.expectedException);
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

    @Parameters
    public static Collection<Object[]> getParameters() {

        LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(false)
                .setMasterKey(ByteString.copyFrom("123".getBytes())).build();
        LedgerData ledgerData1 = LedgerData.newBuilder().setExists(false).setFenced(false)
                .setMasterKey(ByteString.copyFrom("\0".getBytes())).build();

        LedgerMetadataBean bean1 = new LedgerMetadataBean(1L, ledgerData);
        LedgerMetadataBean bean2 = new LedgerMetadataBean(-1L, ledgerData1);
        LedgerMetadataBean bean3 = new LedgerMetadataBean(-1L, ledgerData1);

        bean3.union(bean1);

        return Arrays.asList(new Object[][]{
                //minimal test set
                {bean1.getMap(), 0, null, true},
                {bean2.getMap(), -1L, ledgerData1, false},
                //{bean3.getMap(), 1L, ledgerData, false},
        });
    }

    @Test
    public void get(){
        System.out.println(this.ledgerId);
        try {
            LedgerData result = this.ledgerMetadataIndex.get(ledgerId);
            assertEquals(this.expectedResult, result);
        } catch (Exception e) {
            assertEquals(this.expectedException, e.getClass());
        }

    }

    @After
    public void cleanEnv(){
        this.list.clear();
    }
}
