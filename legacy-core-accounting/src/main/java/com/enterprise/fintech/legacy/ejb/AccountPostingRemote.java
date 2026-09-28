package com.enterprise.fintech.legacy.ejb;

import javax.ejb.Remote;
import java.rmi.RemoteException;

/**
 * Enterprise JavaBean (EJB 3.x) Remote Business Interface for
 * Core Banking Account Posting and Ledger Settlement.
 *
 * Typically deployed on Oracle WebLogic Application Server.
 */
@Remote
public interface AccountPostingRemote {

    /**
     * Executes double-entry journal posting between source and destination accounts.
     */
    PostingResult postJournalEntry(JournalEntryRequest request) throws RemoteException;

    /**
     * Queries current ledger balance and available limit for given account ID.
     */
    AccountBalanceResponse queryBalance(String accountId) throws RemoteException;

    /**
     * Validates account standing and active status in core banking database.
     */
    boolean isAccountActive(String accountId) throws RemoteException;
}
