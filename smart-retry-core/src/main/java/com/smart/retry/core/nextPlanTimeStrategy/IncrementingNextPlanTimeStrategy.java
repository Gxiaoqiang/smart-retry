package com.smart.retry.core.nextPlanTimeStrategy;

import com.smart.retry.common.model.RetryTask;

import java.util.Date;

/**
 *
 */
class IncrementingNextPlanTimeStrategy implements NextPlanTimeStrategy {
    @Override
    public Date nextExecuteTime(RetryTask retryTask) {
        long nextTime = retryTask.getNextPlanTime().getTime() + (retryTask.getOriginRetryNum() - retryTask.getRetryNum() + 1) * retryTask.getIntervalSecond() * 1000;
        return new Date(nextTime);
    }
}
