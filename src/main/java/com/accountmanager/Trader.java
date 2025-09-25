package com.accountmanager;

// Handles buying and selling of stocks
public class Trader {
    private Broker broker;

    //Constructor
    // Construct one for each account? or pass values to functions in here?
    public Trader(Broker broker) {
        this.broker = broker;
    }

    // Buy stock
    public boolean buyStock(Account account, String stock, int quantityOfShares) {
        int stockPrice = broker.getStockPrice("stock");        // get stock price
        int cost = stockPrice * quantityOfShares;       // calculate total cost
        if (account.getAccountValue() > cost) {         // check account has enough to make purchase
            account.addOwnedStock(stock);               // add stock to account
            account.removeValue(cost);                  // reduce account value
            return true;                                // shares were bought
        }
        return false;   // shares were unable to be purchased
    }

    // Sell stock
    public boolean sellStock(Account account, String stock, int quantityOfShares) {
        if (account.findStock(stock)) {
            int stockPrice = broker.getStockPrice("stock");
            int cost = stockPrice * quantityOfShares;
            account.removeOwnedStock(stock);
            account.removeValue(cost);
            return true;
        }
        return false;
    }
}
