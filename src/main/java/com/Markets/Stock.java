package com.Markets;

import com.accountmanager.Option;

import java.util.ArrayList;

public class Stock extends TradeItem {

    private ArrayList<Option> options;  // list of options for this stock


    public Stock(String symbol, String name) {
        super(symbol, name);
    }


    public ArrayList<Option> getOptions() {
        return options;
    }


}
