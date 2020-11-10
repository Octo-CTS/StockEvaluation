
import java.lang.*;

public class StockPrice{


  // declare variables

  public String trans_date; //Transaction date stock traded on
  public Double open_price; //Open price of the stock
  public Double high_price; //High price of the stock
  public Double low_price; //Low price of the stock
  public Double close_price; //Close price of the stock

  // structure the class format

  StockPrice(String trans_date, Double open_price, Double high_price,
  Double low_price, Double close_price){

      this.trans_date = trans_date;
      this.open_price = open_price;
      this.high_price = high_price;
      this.low_price = low_price;
      this.close_price = close_price;

  }


  //Getters and setters functions

  public void setTransDate(String transDate){
    this.trans_date = transDate;
  }

  public String getTransDate(){
    return trans_date;
  }

  public void setOpen(Double open){
    this.open_price = open;
  }

  public Double getOpen(){
    return open_price;
  }

  public void setHigh(Double high){
    this.high_price = high;
  }

  public Double getHigh(){
    return high_price;
  }

  public void setLow(Double low){
    this.low_price = low;
  }

  public Double getLow(){
    return low_price;
  }

  public void setClose(Double close){
    this.close_price = close;
  }

  public Double getClose(){
    return close_price;
  }
}
