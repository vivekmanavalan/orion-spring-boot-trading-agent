package com.vmware.tanzu.demos.sta.tradingagent.trade;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

@Component
public class Trade {

    @Autowired
    RestTemplate restTemplate;

    // Queue and sum for the short moving average (e.g., 10 periods)
    static Queue<Double> shortWindow = new LinkedList<>();
    static double shortSum = 0.0;
    static int shortWindowSize = 20;

    // Queue and sum for the long moving average (e.g., 50 periods)
    static Queue<Double> longWindow = new LinkedList<>();
    static double longSum = 0.0;
    static int longWindowSize = 50;

    static boolean state = false;
    static boolean prevState = false;

    private static int sellSkipCount = 0;

    private static double avgBuyPrice = 0;

    private static double cashBalance = 1000000;
    private static double portfolio = 0;

    private static double lastBuy = 0;
    private static double lastSell = 0;

    @Scheduled(fixedRate = 100)

//    @RequestMapping(value = "/template/products")
    public void getProductList() {
//        sellOrBuy(1);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        System.out.println("calling...");


        String response = restTemplate.exchange("https://sta.az.run.withtanzu.com/api/v1/stocks/aapl", HttpMethod.GET, entity, String.class).getBody();

        JSONObject jsonObject = new JSONObject(response);

        double price = jsonObject.getDouble("price");
        System.out.println("price : " + price);

        shortWindow.add(price);
        longWindow.add(price);

        shortSum += price;
        longSum += price;

        // If the windows are larger than the specified sizes, remove the oldest value
        if (shortWindow.size() > shortWindowSize) {
            shortSum -= shortWindow.poll();
        }

        if (longWindow.size() > longWindowSize) {
            longSum -= longWindow.poll();
        }


        if (sellSkipCount > 3 && price > avgBuyPrice) {
            System.out.println("special sell triggered!!");
            sell(price, true);
        }

        if (sellSkipCount > 20) {
            System.out.println("special sell triggered!!");
            sell(price, true);
        }


        double shortMovingAverage = 0;
        // Calculate and print the moving averages
        if (shortWindow.size() == shortWindowSize) {
            shortMovingAverage = shortSum / shortWindowSize;
//                    System.out.println("Short Moving Average: " + shortMovingAverage);
        }

        double longMovingAverage = 0;

        if (longWindow.size() == longWindowSize) {
            longMovingAverage = longSum / longWindowSize;
//                    System.out.println("Long Moving Average: " + longMovingAverage);
        }

        prevState = state;
        if (longMovingAverage > shortMovingAverage) {
            state = false;
        }

        if (longMovingAverage < shortMovingAverage) {
            state = true;
        }

        if (prevState && !state) {
            buy(price);
        }

        if (!prevState && state) {
            sell(price, false);
        }



    }

    private  void buy(double price) {

        if (cashBalance <= 0 ) {
            return;
        }

        int qty = (int) (cashBalance / price);

        if (qty <= 0) {
            return;
        }

        if (avgBuyPrice == 0) {
            avgBuyPrice = price;
        } else {
            avgBuyPrice = ((avgBuyPrice * portfolio) + (price * qty) ) / (portfolio + qty);
        }

        double totValue = qty * price;

        cashBalance = cashBalance - totValue;
        portfolio = portfolio + qty;
        lastBuy = price;
        sellOrBuy(( qty));

        System.out.println("buy:" + price + " qty:" + qty + " cash balance " + cashBalance + " portfolio : " + portfolio + " avgBuyPrice: " + avgBuyPrice);
    }

    private  void sell(double price, boolean forcedSell) {

        if (portfolio <= 0){
            return;
        }

        if (!forcedSell && avgBuyPrice > price) {
            sellSkipCount++;
            System.out.println("sell skipped for : " + price + " sell skipped count : " + sellSkipCount);
            return;
        }

        sellSkipCount = 0;

        double totValue = portfolio * price;
        cashBalance = cashBalance + totValue;
        sellOrBuy(((int) portfolio)/-1);
        portfolio = 0;

        System.out.println("sell:" + price + " cash balance " + cashBalance + " portfolio : " + portfolio);
    }


    public void sellOrBuy(int qty){



        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getToken("sta_onionspas","70035d58-03ae-4901-816f-cce2ef045cb0"));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("shares", String.valueOf(qty));
        body.add("symbol", "aapl");
        body.add("user", "onionspas");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://sta.az.run.withtanzu.com/api/v1/bids",
                request,
                String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            // Payment link created successfully
        } else {
            throw new RuntimeException("Failed to create payment link: " + response.getStatusCode());
        }
    }


    public String getToken( String clientId, String clientSecret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope","bid");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                 "https://login.sso.az.run.withtanzu.com/oauth2/token",
                request,
                String.class);

        if (response.getStatusCode().is2xxSuccessful()) {

            return new JSONObject(response.getBody()).getString("access_token");
        } else {
            throw new RuntimeException("Failed to get token: " + response.getStatusCode());
        }
    }





}
