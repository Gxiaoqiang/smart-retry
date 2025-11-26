package com.smart.retry.core.nextPlanTimeStrategy;

import com.smart.retry.common.model.RetryTask;

import java.util.Date;

/**
 * examples:指数回避策略
 * 第一次重试：间隔 = 100s
 * 第二次重试：间隔 = 100s * 2^1 = 200s
 * 第三次重试：间隔 = 100s * 2^2 = 400s
 * 第五次重试：间隔 = 100s * 2^4 = 1600s
 *
 * @Author xiaoqiang
 * @Version BackOffNextPlanTimeStrategy.java, v 0.1 2025年09月19日 11:19 xiaoqiang
 * @Description: TODO
 */
class BackOffNextPlanTimeStrategy implements NextPlanTimeStrategy {

    @Override
    public Date nextExecuteTime(RetryTask retryTask) {
        // 已重试次数（从1开始计算第几次重试）
        int attempt = retryTask.getOriginRetryNum() - retryTask.getRetryNum() + 1;

        // 指数退避：间隔 = 基础间隔 * 2^(attempt - 1)，第一次重试就是 base * 1
        long backoffIntervalMs = retryTask.getIntervalSecond() * 1000L * (1L << Math.max(0, attempt - 1));

        // 下次执行时间 = 当前计划时间 + 计算出的退避间隔
        long nextTime = retryTask.getNextPlanTime().getTime() + backoffIntervalMs;

        return new Date(nextTime);
    }

    public static void main(String[] args) {
        BackOffNextPlanTimeStrategy backOffNextPlanTimeStrategy = new BackOffNextPlanTimeStrategy();
        RetryTask retryTask = new RetryTask();
        retryTask.setIntervalSecond(300);
        retryTask.setOriginRetryNum(5);
        retryTask.setRetryNum(1);
        retryTask.setNextPlanTime(new Date());
        System.out.println(backOffNextPlanTimeStrategy.nextExecuteTime(retryTask));
    }

}
