package com.accountmanager;

import java.util.ArrayList;

public class Stock {

    private final String symbol;      // the symbol the stock trades under
    private final String name;        // the full name of the company
    private int price;          // the current price of the stock
    private ArrayList<Option> options;  // list of options for this stock


    public Stock(String symbol, String name, int price) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public ArrayList<Option> getOptions() {
        return options;
    }

    public void updatePrice(int price){
        this.price = price;
    }
}
