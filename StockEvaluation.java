import java.util.Properties;
import java.util.Scanner;
import java.util.*;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class StockEvaluation {

    static Connection conn = null;

    public static void main(String[] args) throws Exception {
        // Get connection properties
        String paramsFile = "ConnectionParameters.txt"; // File with connection parameters
        if (args.length >= 1) {
            paramsFile = args[0];
        }
        Properties connectprops = new Properties();
        connectprops.load(new FileInputStream(paramsFile));

        try {
            // Get connection to MySQL database server
            Class.forName("com.mysql.jdbc.Driver");

            String dburl = connectprops.getProperty("dburl");
            String username = connectprops.getProperty("user");
            conn = DriverManager.getConnection(dburl, connectprops);
            System.out.printf("Database connection %s %s established.%n", dburl, username);

            // showCompanies();
            Deque<StockPrice> colTrades = new ArrayDeque<StockPrice>(); //Deque for collection of trades
            Deque<StockPrice> adjData = new ArrayDeque<StockPrice>(); //Deque for adjusting stock splits

            // Enter Ticker and TransDate, Fetch data for that ticker and date
            Scanner in = new Scanner(System.in);
            while (true) {

                System.out.print("Enter ticker [start/end dates] (YYYY.MM.DD): ");
                String[] data = in.nextLine().trim().split("\\s+");
                if (data.length < 1 || data[0].equals("")){
                    break;
                }
                if (retrieveCompany(data[0]) == false){
                  continue;
                }
                else if (data.length == 1 ){
                    colTrades = retrieveAllPriceVolume(data[0]);
                }

                else if (data.length == 3){
                    // collection of trades for cycle of time
                    colTrades = retrievePriceVolume(data[0], data[1], data[2]);
                }
                else{
                  continue;
                }

                // System.out.println(colTrades.getLast().getTransDate());
                adjData = stockAdjust(colTrades);

                // System.out.println(data.length);
                movingAvg(adjData); // Calucate moving average for data sets

            }
            System.out.println("Database connection closed.");
            conn.close();
        } catch (SQLException ex) {
            System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n",
                                    ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
        }
    }

    static void showCompanies() throws SQLException {
        /** void -> void
        * PRE: Takes in no parameters. Creates and executes a query to be returned
        * POST: Outputs a list of stock companies from a MySQL database
        **/

        // Create and execute a query
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("select Ticker, Name from Company");

        // Show results
        while (results.next()) {
            System.out.printf("%5s %s%n", results.getString("Ticker"), results.getString("Name"));
        }
        stmt.close(); //close query
    }


    // Section 2.2
    static boolean retrieveCompany(String ticker) throws SQLException {
      /** string -> boolean
      * PRE: Takes in a ticker for a stock company to be requested from database
      * POST: Returns a boolean result if the company ticker information is in
      * in the database
      **/


      boolean inDatabase = true; //Checks if query is contained in database

      // Prepare query
      PreparedStatement pstmt = conn.prepareStatement(
                "select Name" +
                "  from Company " +
                "  where Ticker = ?");
      pstmt.setString(1, ticker);
      ResultSet rs = pstmt.executeQuery();

      // Did we get anything? If so, output data.
      if (rs.next()) {
          System.out.printf("%s%n",
                  rs.getString(1));
          inDatabase = true;
      } else {
          System.out.printf("Ticker %s not found.%n", ticker);
          inDatabase = false;
      }
      pstmt.close();
      return inDatabase;
    }

    static Deque<StockPrice> retrieveAllPriceVolume(String ticker) throws SQLException{
      /** string -> Deque<StockPrice>
      * PRE: Takes in parameters for ticker without start end dates. Will
      * be formatted for a PreparedStatement query
      * POST: Returns a collection of unadjusted stock prices of a company
      **/
      Deque<StockPrice> stock_data = new ArrayDeque<StockPrice>();

      // Prepare query
      PreparedStatement pstmt = conn.prepareStatement(
                "select *" +
                "  from PriceVolume " +
                "  where Ticker = ? " +
                "  order by TransDate DESC;");
      pstmt.setString(1, ticker);
      ResultSet rs = pstmt.executeQuery();
      boolean checkRS = rs.next();

      // Did we get anything? If so, output data.
      if (checkRS) {
        StockPrice beginRS = new StockPrice(rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6));
        stock_data.add(beginRS);
        while (rs.next()) {

          // System.out.printf("TransDate: %s, Open: %2f, High: %.2f, Low: %.2f, Close: %.2f%n",
          //         rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6));
          StockPrice tuple = new StockPrice(rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6));

          stock_data.add(tuple);

        }
        // Confirming number of stocks in the collection
        // System.out.println(stock_data.size());
      } else {
          System.out.printf("Ticker %s not found.%n", ticker);
      }
      pstmt.close();
      return stock_data;
    }

    // 2.3
    static Deque<StockPrice> retrievePriceVolume(String ticker, String start, String end) throws SQLException{
      /** string, string, string -> Deque<StockPrice>
      * PRE: Takes in parameters for ticker, start date, and end date. Will
      * be formatted for a preparedStatement query
      * POST: Returns a collection of unadjusted stock prices of a company
      * for a interval of transaction dates
      **/

      Deque<StockPrice> stock_data = new ArrayDeque<StockPrice>();

      // Prepare query
      PreparedStatement pstmt = conn.prepareStatement(
                "select *" +
                "  from PriceVolume " +
                "  where Ticker = ? and TransDate between ? and ? " +
                "  order by TransDate DESC;");
      pstmt.setString(1, ticker);
      pstmt.setString(2, start);
      pstmt.setString(3, end);
      ResultSet rs = pstmt.executeQuery();
      boolean checkRS = rs.next();

      // Did we get anything? If so, output data.
      if (checkRS) {
        StockPrice beginRS = new StockPrice(rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6));
        stock_data.add(beginRS);
        while (rs.next()) {

          // System.out.printf("TransDate: %s, Open: %2f, High: %.2f, Low: %.2f, Close: %.2f%n",
          //         rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6));
          StockPrice tuple = new StockPrice(rs.getString(2),rs.getDouble(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6));

          stock_data.add(tuple);

        }
        // Confirming number of stocks in the collection
        System.out.println(stock_data.size());
      } else {
          System.out.printf("Ticker %s not found.%n", ticker);
      }
      pstmt.close();
      return stock_data;
    }

    // 2.4
    static Deque<StockPrice> stockAdjust(Deque<StockPrice> rawData){
      /** Deque -> Deque
      * PRE: Takes in a deque with stock prices to be adjusted to normalization
      * based on when a stock split occured
      * POST: Returns normalized data prices for a deque with data type
      * StockPrice
      **/

      // Adjusts prices on trading day
      Double normalizer = 1d;
      // Saves unadjusted next trading day
      Double nextOpenUn = 0d;
      // Counts the number of splits
      Double splitCounter = 0d;

      Iterator<StockPrice> it = rawData.iterator(); // Iterator



      while (it.hasNext()){
        // Reflects current trading day

        StockPrice element = it.next();
        // Closing price unadjusted
        Double closingPrice = element.getClose();
        // Calculates splits for 2:1
        if ((Math.abs((closingPrice / nextOpenUn) - 2d)) < 0.20d){

          // Increases normalizer based on split
          normalizer = normalizer*2d;
          // System.out.println(nextOpenUn);
          System.out.printf("2:1 split on %s  %2f --> %2f \n", element.getTransDate(),element.getClose(),nextOpenUn);

          // Updates the new open unadjusted prices
          nextOpenUn = element.getOpen();
          // System.out.println();

          // Sets to the correct prices
          element.setOpen(element.getOpen()/normalizer);
          element.setLow(element.getLow()/normalizer);
          element.setHigh(element.getHigh()/normalizer);
          element.setClose(element.getClose()/normalizer);


          splitCounter++;
        }
        // Calculates splits for 3:1
        else if ((Math.abs((closingPrice / nextOpenUn) - 3d)) < 0.30d){
          normalizer = normalizer*3d;

          System.out.printf("3:1 split on %s  %2f --> %2f \n", element.getTransDate(),element.getClose(),nextOpenUn);

          // Updates the new open unadjusted
          nextOpenUn = element.getOpen();

          // Sets to the correct prices
          element.setOpen(element.getOpen()/normalizer);
          element.setLow(element.getLow()/normalizer);
          element.setHigh(element.getHigh()/normalizer);
          element.setClose(element.getClose()/normalizer);

          splitCounter++;
        }

        // Calculates splits for 3:2
        else if ((Math.abs((closingPrice / nextOpenUn) - (3/2d))) < 0.15d){
          normalizer = normalizer*(1.5d);

          System.out.printf("3:2 split on %s  %2f --> %2f \n", element.getTransDate(),element.getClose(),nextOpenUn);

          // Updates the new open unadjusted
          nextOpenUn = element.getOpen();

          // Sets to the correct prices
          element.setOpen(element.getOpen()/normalizer);
          element.setLow(element.getLow()/normalizer);
          element.setHigh(element.getHigh()/normalizer);
          element.setClose(element.getClose()/normalizer);

          splitCounter++;
        }
        else{
          nextOpenUn = element.getOpen();

          // System.out.println("Before normalize for open " + element.getOpen());

          // Sets to the correct prices
          element.setOpen(element.getOpen()/normalizer);
          element.setLow(element.getLow()/normalizer);
          element.setHigh(element.getHigh()/normalizer);
          element.setClose(element.getClose()/normalizer);

          // System.out.println("After normalize for open " + element.getOpen());

        }

      }
      System.out.printf("%f splits in %d trading days \n\n", splitCounter, rawData.size());

      return rawData;

    }
    // 2.8 & 2.9
    static void movingAvg(Deque<StockPrice> adjData){
      /** Deque -> Void
      * PRE: Takes in a collection of adjusted prices of stock data
      * POST: Computes moving average for 50 days, optimizes when to purchase
      * and sell stock based on moving average, and keeps track of net cash
      * and the number of transactions, and then outputs investment results.
      **/
      Deque<StockPrice> movingAvDeque = new ArrayDeque<StockPrice>(); //Collection of moving averages

      // Calculates moving average
      Double countMovAvg = 0d;
      Double movAvgSum = 0d; //Sum of 50 days
      Double movAvg = 0d; // Moving average for 50 days

      // Calculates total shares and net gain
      Double prevClose = 1d;
      Double totShares = 0d; //Total shares purchased
      Double netCash = 0d; //Total net cash
      Integer numbTrans = 0; //Total number of transactions

      // Works
      // Calculates moving average for intial 50 values
      while(movingAvDeque.size() < 50d)  {
          StockPrice dayLooking = adjData.getLast();

          movAvgSum = movAvgSum + adjData.removeLast().getClose();

          countMovAvg++; //Increases count for number of days
          movingAvDeque.add(dayLooking);
          prevClose = movingAvDeque.getLast().getClose();
        }
      // Debugging purposes
      // System.out.println("Day fifty " + movingAvDeque.getLast().getTransDate()+ " , average = " + movAvgSum/50d);


      StockPrice currentDay = adjData.getLast();
      Double nextDayOpen = 0d;
      Double numBuys = 0d; // Counter for number of purchases for a stock
      Double numSells = 0d; // Counter for number of sell offs for a stock
      StockPrice nextDay = adjData.getLast();

      while(adjData.size() > 1){
        currentDay = adjData.removeLast(); // good
        // System.out.println(currentDay.getTransDate());

        movAvg = movAvgSum/50d; //Calculates moving average

        // Update movAvg for the next day and remove the first element added to the Moving average deque
        movAvgSum = movAvgSum + currentDay.getClose() - movingAvDeque.removeFirst().getClose();

        // Add the current day looking at to the moving avg deque
        movingAvDeque.add(currentDay);

        // Once removed, get the open price of the next day
        nextDayOpen = adjData.getLast().getOpen();
        nextDay = adjData.getLast();

        // Decides whether to buy
        if((currentDay.getClose()) < movAvg && ((currentDay.getClose()/currentDay.getOpen()) <= 0.97d)){
          // Find next day?
          netCash = netCash - (nextDayOpen *100d)- 8d;
          totShares = totShares + 100d;

          // System.out.printf("Buy: %s 100 shares @ %f, Total shares = %f, cash shares = %f\n", nextDay.getTransDate(), nextDay.getOpen(), totShares,netCash);

          numbTrans++; // increase counter for total transactions

          prevClose = movingAvDeque.getLast().getClose();

        }

        //Decides whether to sell
        else if((totShares >= 100) && (currentDay.getOpen() > movAvg) && (currentDay.getOpen()/prevClose)>=1.01d ){

          netCash = netCash + (((currentDay.getOpen() + currentDay.getClose())/2)*100d) - 8d;
          totShares = totShares - 100d;
          // System.out.printf("Sell: %s 100 shares @ %f, Total shares = %f, cash shares = %f\n", currentDay.getTransDate(), currentDay.getOpen(), totShares,netCash);
          numbTrans++;

          prevClose = movingAvDeque.getLast().getClose();

        }
        else{
          // System.out.printf("%s open: %f, high: %f, low: %f, close: %f (average %f) \n", currentDay.getTransDate(), currentDay.getOpen(), currentDay.getHigh(),currentDay.getLow(),currentDay.getClose(), movAvg);
          prevClose = movingAvDeque.getLast().getClose();
        }
      }

      // For debugging
      // System.out.printf("Final sale: %s %f shares @ %f, cash shares = %f (average %f)\n", adjData.getLast().getTransDate(), totShares, adjData.getLast().getOpen(), netCash, movAvg);

      System.out.printf("Executing investment stradegy\nTransactions executed: %d \nNet cash: %f\n\n",numbTrans,netCash);
    }

}
