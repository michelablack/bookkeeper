package org.uniroma2.it.utils;

import org.apache.bookkeeper.bookie.storage.ldb.DbLedgerStorageDataFormats.LedgerData;
import org.apache.bookkeeper.util.collections.ConcurrentLongHashMap;

import java.util.ArrayList;
import java.util.List;

public class LedgerMetadataBean {
    private long ledgerId;
    private List<Long> ledgerIds;
    private LedgerData ledger;
    private List<LedgerData> expectedResult;
    private ConcurrentLongHashMap<LedgerData> map;

    public LedgerMetadataBean(long ledgerId, LedgerData ledger) {
        this.ledgerId = ledgerId;
        this.ledger = ledger;
        this.map = new ConcurrentLongHashMap<>();
        this.expectedResult = new ArrayList<>();
        if (this.ledger!=null) this.map.put(ledgerId, ledger);
        this.expectedResult.add(ledger);
        this.ledgerIds = new ArrayList<>();
        this.ledgerIds.add(this.ledgerId);
    }

    public long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(long ledgerId) {
        this.ledgerId = ledgerId;
    }

    public LedgerData getLedger() {
        return ledger;
    }

    public void setLedger(LedgerData ledger) {
        this.ledger = ledger;
    }

    public ConcurrentLongHashMap<LedgerData> getMap() {
        return map;
    }

    public void setMap(ConcurrentLongHashMap<LedgerData> map) {
        this.map = map;
    }

    public List<LedgerData> getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(List<LedgerData> expectedResult) {
        this.expectedResult = expectedResult;
    }

    public List<Long> getLedgerIds() {
        return ledgerIds;
    }

    public void setLedgerIds(List<Long> ledgerIds) {
        this.ledgerIds = ledgerIds;
    }


    public void union(LedgerMetadataBean other) {
        this.map.put(other.getLedgerId(), other.ledger);
        this.ledgerIds.add(other.getLedgerId());
        this.expectedResult.add(other.getLedger());
    }

}
