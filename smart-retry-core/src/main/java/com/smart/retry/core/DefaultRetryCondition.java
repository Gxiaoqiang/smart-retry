package com.smart.retry.core;

import com.smart.retry.common.RetryCondition;
import com.smart.retry.common.constant.ExecuteResultStatus;
import com.smart.retry.common.constant.RetryOccurType;
import com.smart.retry.common.model.MethodChain;
import com.smart.retry.common.model.RetryAttemptContext;

/**
 * @Author xiaoqiang
 * @Version DefaultRetryCondition.java, v 0.1 2025年02月15日 17:15 xiaoqiang
 * @Description: TODO
 */
public class DefaultRetryCondition implements RetryCondition {
    private RetryAttemptContext retryAttemptContext;
    public DefaultRetryCondition(RetryAttemptContext retryAttemptContext) {
        this.retryAttemptContext = retryAttemptContext;
    }
    @Override

    public boolean needRetry() {
        RetryOccurType retryOccurType = retryAttemptContext.getRetryOccurType();
        //如果是基于异常的重试，判断条件
        if (retryOccurType.equals(RetryOccurType.EXCEPTION)) {
            if (retryAttemptContext.getThrowable() == null) {
                return false;
            }
            boolean retryFlag = judgeCanRetryByException();
            if (retryFlag == true) {
                retryAttemptContext.getCurrentMethodChain().setRetry(true);
                //4 判断当前的方法是不是重试链的tail，如果不是tail不进行重试
                boolean isLowest = checkChainException();
                if (isLowest) {
                    return true;
                }
                return false;
            }
        }
        if (retryOccurType.equals(RetryOccurType.RESULT)) {
            if (retryAttemptContext.getResult() == null) {
                return false;
            }
            if (ExecuteResultStatus.FAIL.equals(retryAttemptContext.getResult())) {
                return true;
            }
        }

        return false;
    }


    /**
     * 判断存在异常情况下是否满足进行重试的条件
     *
     * @return
     */
    private boolean judgeCanRetryByException() {
        //如果includes为空，所有的异常都上报，如果不为空 只上报包含这里面的异常
        Class[] includes = retryAttemptContext.getIncludes();

        Class[] excludes = retryAttemptContext.getExcludes();

        //如果这个参数是0，则所有的异常都需要注册重试任务
        boolean retryFlag = false;
        if (includes.length == 0 && excludes.length == 0) {
            //return doRetry(retryable, invocation);
            retryFlag = true;
        }
        if (includes.length > 0) {
            for (Class includeEx : includes) {
                if (includeEx.isAssignableFrom(retryAttemptContext.getThrowable().getClass())) {
                    retryFlag = true;
                    break;
                }
            }
        }
        if (includes.length == 0 && excludes.length > 0) {
            boolean flag = true;
            for (Class exclude : excludes) {
                if (exclude.isAssignableFrom(retryAttemptContext.getThrowable().getClass())) {
                    flag = false;
                }
            }
            retryFlag = flag;
        }

        return retryFlag;
    }

    /**
     * 如果抛出的异常符合重试的条件，如果存在链式重试的调用，判断当前的方法 是不是最底层的方法。
     * 如果不是最底层的方法，不需要重试
     *
     * @param
     * @return
     * @example ：A-->B-->C
     * A、B、C 上有注解了重试的方法，C中抛出了异常，切符合A、B的重试的条件。 则只能有C抛出异常的时候才会生成重试的任务，A和B不会生成重试任务
     */

    private boolean checkChainException() {
        boolean retryFlag = false;
        MethodChain methodChainModel = RetrySnapshot.getChainByMethod(retryAttemptContext.getMethod());
        if (methodChainModel.isRetry() && methodChainModel.isTail()) {
            retryFlag = true;
        }
        return retryFlag;
    }


}
