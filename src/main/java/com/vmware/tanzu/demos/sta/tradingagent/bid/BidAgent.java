package com.vmware.tanzu.demos.sta.tradingagent.bid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

interface BidAgent {
    List<BidAgentRequest> execute(Context context);

    interface Context {
        List<Stock> stocks();

        Map<String, Integer> shares();

        BigDecimal userBalance();

        String user();
    }
}
