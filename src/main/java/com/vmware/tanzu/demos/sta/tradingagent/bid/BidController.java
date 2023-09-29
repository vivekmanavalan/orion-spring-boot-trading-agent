package com.vmware.tanzu.demos.sta.tradingagent.bid;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
class BidController {
    private final Logger logger = LoggerFactory.getLogger(BidController.class);
    private final String user;
    private final BidAgent bidAgent;
    private final RestTemplate client;

    BidController(@Value("${app.agent.user}") String user, BidAgent bidAgent, RestTemplate client) {
        this.user = user;
        this.bidAgent = bidAgent;
        this.client = client;
    }

    void execute(PrintWriter out) {
        out.println("Using bid agent: " + bidAgent);
        out.println("Current user: " + user);

        final ResponseEntity<UserInfo> userInfoResp;
        try {
            userInfoResp = client.getForEntity("/api/v1/users/" + user, UserInfo.class);
        } catch (RestClientException e) {
            logger.warn("Failed to lookup user: {}", user, e);
            out.println("Failed to lookup user: " + user);
            return;
        }
        final var userInfo = userInfoResp.getBody();
        final NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        out.println("User balance: " + nf.format(userInfo.balance()));

        out.println("Current time: " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withLocale(Locale.US).format(ZonedDateTime.now()));

        var stocks = client.getForObject("/api/v1/stocks", Stock[].class);
        if (stocks == null || stocks.length == 0) {
            logger.info("Found no stocks");
            out.println("Found no stocks");
            return;
        }

        // Sort stocks against symbol.
        Arrays.sort(stocks, Comparator.comparing(Stock::symbol));

        final var shares = new HashMap<String, Integer>(userInfo.stocks().size());
        userInfo.stocks().forEach(s -> shares.put(s.symbol, s.shares));

        final List<Stock> stockList = Stream.of(stocks).map(s -> new Stock(s.symbol(), s.price(), shares.getOrDefault(s.symbol(), 0))).toList();
        final String stockListStr = stockList.stream()
                .map(s -> String.format("%s=%s", s.symbol(), nf.format(s.price())))
                .collect(Collectors.joining(", "));
        logger.info("Executing bid agent {} for user {} with current stocks: {}", bidAgent, user, stockListStr);
        out.println("Current stocks: " + stockListStr);

        final var ctx = new BidAgent.Context() {
            @Override
            public List<Stock> stocks() {
                return stockList;
            }

            @Override
            public Map<String, Integer> shares() {
                return shares;
            }

            @Override
            public BigDecimal userBalance() {
                return userInfo.balance();
            }

            @Override
            public String user() {
                return user;
            }
        };

        final var requests = bidAgent.execute(ctx);
        if (requests.isEmpty()) {
            logger.info("No bid request sent");
            out.println("No bid request sent");
        } else {
            final Map<String, BigDecimal> stockValues = new HashMap<>(stocks.length);
            for (final Stock s : stocks) {
                stockValues.put(s.symbol(), s.price());
            }

            var totalBids = BigDecimal.ZERO;
            for (final BidAgentRequest agentReq : requests) {
                logger.info("Placing bid: {}", agentReq);
                final UserBidRequest userReq = new UserBidRequest(user, agentReq.symbol(), agentReq.shares());
                client.postForLocation("/api/v1/bids", userReq);
                out.println("Placing bid: " + agentReq);

                final BigDecimal stockValue = stockValues.get(agentReq.symbol());
                if (stockValue != null) {
                    totalBids = totalBids.add(stockValue.multiply(BigDecimal.valueOf(agentReq.shares())));
                }
            }
            out.println("Total bids: " + nf.format(totalBids));
        }
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    CharSequence sendBidRequest() {
        final var buffer = new StringWriter(128);
        final PrintWriter out = new PrintWriter(buffer);
        try {
            execute(out);
        } catch (Exception e) {
            logger.warn("Error when executing bid agent", e);
            out.println("Error when executing bid agent:");
            e.printStackTrace(out);
        }
        return buffer.getBuffer();
    }

    private record UserBidRequest(String user, String symbol, int shares) {
    }

    private record UserInfo(
            BigDecimal balance,
            List<UserShare> stocks
    ) {
    }

    private record UserShare(
            String symbol,
            int shares
    ) {
    }
}
