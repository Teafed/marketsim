// data class for symbol information
package com.etl;

public class SymbolData {
   private String symbol;
   private double price;
   private double change;
   private double changePercent;
   
   public SymbolData(String symbol, double price, double change, double changePercent) {
      this.symbol = symbol;
      this.price = price;
      this.change = change;
      this.changePercent = changePercent;
   }

   public String getSymbol() { return symbol; }
   public double getPrice() { return price; }
   public double getChange() { return change; }
   public double getChangePercent() { return changePercent; }
   
   @Override
   public String toString() {
      return symbol; // for display purposes
   }
}