package com.vmware.tanzu.demos.sta.tradingagent.bid;

record BidAgentRequest(
        String symbol,
        int shares
) {
    @Override
    public String toString() {
        return String.format("%d x %s", shares, symbol);
    }
}
