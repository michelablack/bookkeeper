package org.apache.bookkeeper.bookie.storage.ldb.bean;

import io.netty.buffer.ByteBuf;
import org.apache.bookkeeper.bookie.storage.ldb.DbLedgerStorageDataFormats.LedgerData;
import java.util.Map.*;

public class LedgerMetadataBean {
    private byte[] ledgerId;
    private LedgerData ledgerData;
    //private Entry<byte[], byte[]> entry;

    public LedgerMetadataBean(byte[] ledgerId, LedgerData ledgerData) {
        this.ledgerId = ledgerId;
        this.ledgerData = ledgerData;
    }


    public byte[] getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(byte[] ledgerId) {
        this.ledgerId = ledgerId;
    }

    public LedgerData getLedgerData() {
        return ledgerData;
    }

    public void setLedgerData(LedgerData ledgerData) {
        this.ledgerData = ledgerData;
    }

}
