package com.accountmanager;

import com.markets.*;


import java.util.LinkedHashSet;


/*
    A portfolio manages all trade items for an account. It provides information about which trade items an
    account owns as well as how many of each. A portfolio knows its total value and provides methods to
    update and return this value.
 */
public class Watchlist {

    private LinkedHashSet<TradeItem> watchlist;
    private final int maxSize = 50;
    private int watchlistSize;


    public Watchlist() {
        watchlist = new LinkedHashSet<>();
        this.watchlistSize = 0;
    }


    /**
     * Adds a specified TradeItem to the watchlist.
     * @param tradeItem The TradeItem to add
     * @return True if successful, False if list is full.
     */
    public boolean addWatchlistItem(TradeItem tradeItem) {
        if (watchlistSize < maxSize) {
            watchlist.add(tradeItem);
            watchlistSize++;
            return true;
        }
        else
            return false;
    }

    /**
     * Removes a specified TradeItem from the watchlist.
     * @param tradeItem The TradeItem to remove
     * @return True if successful, False if the list did not contain that item.
     */
    public boolean removeWatchlistItem(TradeItem tradeItem) {
        if (watchlist.remove(tradeItem)) {
            watchlistSize--;
            return true;
        }
        else
            return false;
    }

    public int  getWatchlistSize() {
        return watchlistSize;
    }


}