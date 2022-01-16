package org.uniroma2.it.utils;

import org.apache.bookkeeper.bookie.storage.ldb.LedgerMetadataIndex;

import java.io.IOException;

public class MyRunnable implements Runnable {
    LedgerMetadataIndex lmi;
    private final long ledgerId;
    public MyRunnable(LedgerMetadataIndex lmi, long ledgerId) {
        this.lmi = lmi;
        this.ledgerId = ledgerId;
    }

    public void run() {
        try {
            final long INTERVAL = 50;
            long start = System.nanoTime();
            long end=0;
            do{
                end = System.nanoTime();
            } while(start + INTERVAL >= end);
            this.lmi.delete(this.ledgerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
