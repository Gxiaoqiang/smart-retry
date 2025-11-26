package com.smart.retry.core.nextPlanTimeStrategy;

import com.smart.retry.common.model.RetryTask;

import java.util.Date;

/**
 * @Author xiaoqiang
 * @Version NextTimeStrategy.java, v 0.1 2025年07月15日 19:20 xiaoqiang
 * @Description: TODO
 */
interface NextPlanTimeStrategy {

    Date nextExecuteTime(RetryTask retryTask);
}
