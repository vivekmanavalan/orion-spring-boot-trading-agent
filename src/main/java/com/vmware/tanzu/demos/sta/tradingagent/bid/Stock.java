package com.vmware.tanzu.demos.sta.tradingagent.bid;

import java.math.BigDecimal;

record Stock(
        String symbol,
        BigDecimal price,
        int shares
) {
}
