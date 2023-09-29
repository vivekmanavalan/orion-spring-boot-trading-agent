package com.vmware.tanzu.demos.sta.tradingagent.bid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "app.agent.strategy", havingValue = "random")
class RandomBidAgent implements BidAgent {
    private final Random random = new Random();

    @Override
    public List<BidAgentRequest> execute(Context ctx) {
        final int i = random.nextInt(ctx.stocks().size());
        final boolean buy = random.nextBoolean();
        return List.of(new BidAgentRequest(ctx.stocks().get(i).symbol(), buy ? 100 : -100));
    }

    @Override
    public String toString() {
        return "RANDOM";
    }
}
