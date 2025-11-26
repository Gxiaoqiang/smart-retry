package com.smart.retry.core.nextPlanTimeStrategy;

import com.smart.retry.common.model.RetryTask;

import java.util.Date;

/**
 * 固定间隔，每次间隔都是一样的
 */
class FixedNextPlanTimeStrategy implements NextPlanTimeStrategy {


    @Override
    public Date nextExecuteTime(RetryTask retryTask) {

        long nextTime = retryTask.getNextPlanTime().getTime() + retryTask.getIntervalSecond() * 1000;
        return new Date(nextTime);
    }
}
