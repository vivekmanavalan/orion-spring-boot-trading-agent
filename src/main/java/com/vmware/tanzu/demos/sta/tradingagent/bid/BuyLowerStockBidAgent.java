package com.vmware.tanzu.demos.sta.tradingagent.bid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.agent.strategy", havingValue = "buy-lower-stock")
class BuyLowerStockBidAgent implements BidAgent {
    private final Logger logger = LoggerFactory.getLogger(BuyLowerStockBidAgent.class);

    @Override
    public List<BidAgentRequest> execute(Context ctx) {
        // Sort input stocks against price.
        final List<Stock> sortedStocks = new ArrayList<>(ctx.stocks());
        sortedStocks.sort(Comparator.comparing(Stock::price));

        final Stock lowerStock = sortedStocks.get(0);
        logger.info("Found a stock with the lower value: {}", lowerStock.symbol());
        return List.of(new BidAgentRequest(lowerStock.symbol(), 100));
    }

    @Override
    public String toString() {
        return "BUY_LOWER_STOCK";
    }
}
