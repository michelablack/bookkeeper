package org.uniroma2.it.ledgerMetadataIndexTests;

import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorage;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorage.CloseableIterator;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorageFactory;
import org.apache.bookkeeper.bookie.storage.ldb.KeyValueStorageFactory.DbConfigType;
import org.apache.bookkeeper.bookie.storage.ldb.LedgerMetadataIndex;
import org.apache.bookkeeper.stats.StatsLogger;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class LedgerMetadataIndexImpl {


    public static LedgerMetadataIndex createMetadataIndex
            (List<Entry<byte[],byte[]>> list, KeyValueStorageFactory kvf, KeyValueStorage kv,
             StatsLogger sl) throws IOException {
        when(kvf.
                newKeyValueStorage(null,"ledgers", DbConfigType.Small, null))
                .thenReturn(kv);
        doNothing().when(sl).registerGauge(anyString(), any());
        ListIterator<Entry<byte[], byte[]>> listIterator = list.listIterator();
        CloseableIterator<Entry<byte[], byte[]>> iterator = new CloseableIterator<Entry<byte[], byte[]>>() {
            @Override
            public boolean hasNext() {
                return listIterator.hasNext();
            }

            @Override
            public Entry<byte[], byte[]> next(){
                return listIterator.next();
            }

            @Override
            public void close(){
            }
        };
        when(kv.iterator()).thenReturn(iterator);
        return new LedgerMetadataIndex(null, kvf,  null, sl);
    }
}
