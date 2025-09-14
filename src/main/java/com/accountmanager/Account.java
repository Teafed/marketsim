package com.accountmanager;

import java.util.ArrayList;

// One account
public class Account {

    private int accountValue; // Current account value stored in cents
    private String accountName; // User defined name of account
    private ArrayList<String> ownedStocks;  // stocks the account owns
    private ArrayList<String> watchlistStocks; // stocks the account is watching

    // constructor
    public Account(int accountValue, String accountName) {
        this.accountValue = accountValue;
        this.accountName = accountName;
    }

    // Set name
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    // Get name
    public String getName() {
        return accountName;
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
    public void addValue(int amount) {
        this.accountValue += amount;
    }

    // Remove value from account
    public void removeValue(int amount) {
        this.accountValue -= amount;
    }

    // Add stock to owned list
    public boolean addOwnedStock(String stock) {
        this.ownedStocks.add(stock);
        return true;
    }

    // Remove stock from owned list
    public boolean removeOwnedStock(String stock) {
        if(this.ownedStocks.contains(stock)) {
            this.ownedStocks.remove(stock);     // stock was removed
            return true;
        }
        return false;       // stock was not removed
    }

    // Add stock to watchlist
    // Check for errors on input from broker class
    public boolean addWatchedStock(String stock) {
        if (!this.watchlistStocks.contains(stock)) {
            this.watchlistStocks.add(stock);      // stock successfully added
            return true;
        }
        return false;       // stock was not added
    }

    // Remove stock from watchlist
    public boolean removeWatchedStock(String stock) {
        if(this.watchlistStocks.contains(stock)) {
            this.watchlistStocks.remove(stock);       //stock was removed
            return true;
        }
        return false;           // stock was not removed
    }

    // Get list of owned stocks
    public String[]  getOwnedStocks() {
        return this.ownedStocks.toArray(new String[0]);
    }

    // get watchlist of stocks
    public String[]  getWatchedStocks() {
        return this.watchlistStocks.toArray(new String[0]);
    }

    // check if stock is owned by account
    public boolean findStock(String stock) {
        return ownedStocks.contains(stock);
    }


}
