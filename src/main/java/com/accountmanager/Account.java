package com.accountmanager;

import java.util.ArrayList;

// One account
public class Account {

    private int accountValue; // Current account value stored in cents
    private String accountName; // User defined name of account
    private ArrayList<String> stockSymbols;

    // constructor
    public Account(int accountValue, String accountName) {
        this.accountValue = accountValue;
        this.accountName = accountName;
    }

    // Set name
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    // Set value
    public void setAccountValue(int accountValue) {
        this.accountValue = accountValue;
    }

    // Get account value
    public int getAccountValue() {
        return accountValue;
    }

    // Add value to account
    public void addValue(int amount) {}
}
