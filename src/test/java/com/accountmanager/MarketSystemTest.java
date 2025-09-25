package com.accountmanager;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class MarketSystemTest {

    //testAccountCreation();
    //testAccountAssignment();
    //testStockCreation();
    //testOptionCreation();


    @Test
    void testAccountCreation(){
        Account accountOne = new Account(10, "accountOneName");

        assertEquals(10, accountOne.getAccountValue());
        assertEquals("accountOneName", accountOne.getName());
    }

    @Test
    void testAccountFunctions(){
        Account accountOne = new Account(10, "accountOneName");
        assertEquals(10, accountOne.getAccountValue());
        accountOne.addValue(10);
        assertEquals(20, accountOne.getAccountValue());
        accountOne.removeValue(30);
        assertEquals(-10, accountOne.getAccountValue());
    }

    @Test
    void testAccountManager() {
        AccountManager accountManager = new AccountManager();
        Account accountOne =  new Account(10, "accountOneName");
        Account accountTwo = new Account(20, "accountTwoName");
        assertTrue(accountManager.addAccount(accountOne));
        assertTrue(accountManager.addAccount(accountTwo));
        assertEquals(2, accountManager.getNumberOfAccounts());
        assertTrue(accountManager.removeAccount(accountOne));
        assertTrue(accountManager.removeAccount(accountTwo));
        assertFalse(accountManager.removeAccount(accountOne));


    }
}