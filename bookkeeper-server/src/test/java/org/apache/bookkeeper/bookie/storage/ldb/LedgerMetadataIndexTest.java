package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import org.apache.bookkeeper.bookie.storage.ldb.bean.LedgerMetadataBean;
import org.apache.bookkeeper.bookie.storage.ldb.bean.WriteCacheBean;
import org.apache.bookkeeper.stats.StatsLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.junit.MockitoJUnitRunner;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorageFactory.DbConfigType;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorage.CloseableIterator;
import org.apache.bookkeeper.bookie.storage.ldb.DbLedgerStorageDataFormats.LedgerData;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.*;
import java.util.Map.*;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

@RunWith(value = Parameterized.class)
public class LedgerMetadataIndexTest {

    private final KeyValueStorage ledgersDb = null;
    private LedgerMetadataIndex ledgerMetadataIndex;
    private LedgerMetadataBean bean;
    private final LedgerData expectedResult;
    private List<Entry<byte[], byte[]>> list;

    public LedgerMetadataIndexTest(LedgerMetadataBean bean, LedgerData expectedResult) {
        this.bean = bean;
        this.expectedResult = expectedResult;
    }

    /**@Mock
    KeyValueStorageFactory keyValueFactoryInterface = mock(KeyValueStorageFactory.class);
    @Mock
    KeyValueStorage keyValueInterface = mock(KeyValueStorage.class);
    @Mock
    StatsLogger statsLoggerInterface = mock(StatsLogger.class);*/

    @Before
    public void configure() throws IOException {

        KeyValueStorageFactory keyValueFactoryInterface = mock(KeyValueStorageFactory.class);
        KeyValueStorage keyValueInterface = mock(KeyValueStorage.class);

        when(keyValueFactoryInterface.
                newKeyValueStorage(null,"ledgers", DbConfigType.Small, null))
                .thenReturn(keyValueInterface);
        list = new ArrayList<>();
        //LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(false)
        //        .setMasterKey(ByteString.copyFrom("123".getBytes())).build();
        LedgerData ledgerData = this.bean.getLedgerData();
        Entry<byte[], byte[]> entry = newEntry("1".getBytes(), ledgerData.toByteArray());
        //Entry<byte[], byte[]> entry = newEntry(this.bean.getLedgerId(), ledgerData.toByteArray());
        list.add(entry);
        ArrayList<Entry<byte[], byte[]>> copyList = new ArrayList<>(list);
        CloseableIterator<Entry<byte[], byte[]>> iterator = new CloseableIterator<Entry<byte[], byte[]>>() {
            @Override
            public boolean hasNext() throws IOException {
                if (!copyList.isEmpty()){
                    copyList.remove(0);
                    return true;
                }
                return false;
            }

            @Override
            public Entry<byte[], byte[]> next() throws IOException {
                return entry;
            }

            @Override
            public void close() throws IOException {

            }
        };
        when(keyValueInterface.iterator()).thenReturn(iterator);

        ledgerMetadataIndex = new LedgerMetadataIndex(null, keyValueFactoryInterface, null, null);
    }

    private static Entry<byte[], byte[]> newEntry(byte[] key, byte[] value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    @Parameters
    public static Collection<Object[]> getParameters() {
        LedgerData ledgerData = LedgerData.newBuilder().setExists(true).setFenced(false)
               .setMasterKey(ByteString.copyFrom("123".getBytes())).build();
        return Arrays.asList(new Object[][]{
                //minimal test set
                {new LedgerMetadataBean("1".getBytes(), ledgerData), ledgerData},
                {new LedgerMetadataBean("0".getBytes(), ledgerData), null},
                {new LedgerMetadataBean("-1".getBytes(), ledgerData), null},
        });
    }

    @Test
    public void get() throws IOException {
        byte[] ledgerId = this.bean.getLedgerId();
        System.out.println("LEDGER ID "+Arrays.toString(ledgerId));
        /**LedgerData ledgerData = null;
        for (Entry<byte[], byte[]> entry :list) {
            System.out.println(Arrays.toString(entry.getKey()));
            if (ArrayUtil.getLong(entry.getKey(), 0)==ArrayUtil.getLong(ledgerId,0)) {
                ledgerData = LedgerData.parseFrom(entry.getValue());
                System.out.println(ledgerData);
            }
            break;
        }*/
        LedgerData ledgerData = ledgerMetadataIndex.get(ArrayUtil.getLong(ledgerId, 0));
        assertEquals(ledgerData, this.expectedResult);
    }


}
