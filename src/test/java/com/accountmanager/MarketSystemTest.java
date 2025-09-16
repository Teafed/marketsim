package com.accountmanager;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class MarketSystemTest {

    //testAccountCreation();
    //testAccountAssignment();
    //testStockCreation();
    //testOptionCreation();

    @Test
    void example() {}

    @Test
    void testAccountCreation(){
        Account accountOne = new Account(10, "accountOneName");

        assertEquals(10, accountOne.getAccountValue());
    }
}