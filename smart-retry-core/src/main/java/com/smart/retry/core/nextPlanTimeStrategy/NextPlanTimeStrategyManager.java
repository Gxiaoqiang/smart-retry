package com.smart.retry.core.nextPlanTimeStrategy;

import com.smart.retry.common.constant.NextPlanTimeStrategyEnum;
import com.smart.retry.common.model.RetryTask;

import java.util.Date;

/**
 * @Author xiaoqiang
 * @Version NextTimeStrategyManager.java, v 0.1 2025年07月15日 19:21 xiaoqiang
 * @Description: TODO
 */
public class NextPlanTimeStrategyManager {

    public static Date nextTime(RetryTask retryTask){
        NextPlanTimeStrategyEnum retryTypeEnum = NextPlanTimeStrategyEnum.getByCode(retryTask.getNextPlanTimeStrategy());
        NextPlanTimeStrategy nextPlanTimeStrategy = null;
        if (retryTypeEnum == null) {
            nextPlanTimeStrategy = new FixedNextPlanTimeStrategy();
        } else if (retryTypeEnum.getCode() == NextPlanTimeStrategyEnum.FIXED.getCode()) {
            nextPlanTimeStrategy = new FixedNextPlanTimeStrategy();
        } else if (retryTypeEnum.getCode() == NextPlanTimeStrategyEnum.INCREMENTING.getCode()) {
            nextPlanTimeStrategy = new IncrementingNextPlanTimeStrategy();
        } else if (retryTypeEnum.getCode()==NextPlanTimeStrategyEnum.BACKOFF.getCode()) {
            nextPlanTimeStrategy = new BackOffNextPlanTimeStrategy();
        } else {
            nextPlanTimeStrategy = new FibonacciNextPlanTimeStrategy();
        }
        Date nextPlanTime = nextPlanTimeStrategy.nextExecuteTime(retryTask);
        return nextPlanTime;
    }
}
