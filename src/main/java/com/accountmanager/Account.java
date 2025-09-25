package com.accountmanager;

import com.Markets.Stock;

import java.util.ArrayList;
import java.util.Map;

// One account
public class Account {

    private int accountValue; // Current total account value stored in cents
    private int availableBalance; // the amount the user can currently trade
    private String accountName; // User defined name of account
    private Map<Stock,Integer> holdings;  // stocks the account owns
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

    public void updateAccountValue() {
        accountValue = availableBalance;
        for (Map.Entry<Stock,Integer> entry : holdings.entrySet()) {
            Stock stock = entry.getKey();
            int holdingAmount = entry.getValue();
            if (holdingAmount > 0) {
                accountValue += (stock.getCurrentPrice() * holdingAmount);
            }
        }
    }

    // Add stock to owned list
    public boolean addHolding(Stock stock, int amount) {
        this.holdings.put(stock,amount);
        return true;
    }

    // Remove stock from owned list
    public boolean removeHolding(Stock stock) {
        if(this.holdings.containsKey(stock)) {
            this.holdings.remove(stock);     // stock was removed
            return true;
        }
        return false;       // stock was not removed
    }

    // return number of shares held of a specific stock
    public int getNumberOfShares(Stock stock) {
        return holdings.get(stock);
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
    public String[][] getHoldings() {
        String[][] array = new String[this.watchlistStocks.size()][2];
        for (int i = 0; i < this.watchlistStocks.size(); i++) {
            array[i][0] = this.watchlistStocks.get(i);
            //array[i][1] = Integer.toString(this.holdings.get(this.watchlistStocks.get(i)));
        }
        return array;
    }

    // get watchlist of stocks
    public String[]  getWatchedStocks() {
        return this.watchlistStocks.toArray(new String[0]);
    }

    // check if stock is owned by account
    public boolean ownsStock(String stock) {
        return true;
    }

    @Override
    public String toString() {
        return "Account Name: " + accountName
                           + " Account Value: " + accountValue
                            + " Available Balance: " + availableBalance;}

}
