package com.smart.retry.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author xiaoqiang
 * @Version SmartConfigure.java, v 0.1 2025年09月19日 21:55 xiaoqiang
 * @Description: TODO
 */
@ConfigurationProperties(prefix = "spring.smart-retry")
public class SmartExecutorConfigure {


    public static class ClearTask {
        // 是否开启清除任务，默认关闭
        private Boolean enabled = false;

        //任务清理的时间，默认每天凌晨三点清理
        private String cron = "0 0 3 * * ?";


        //默认每次清理100行数据，防止清理时对数据库的压力
        private int limitRows = 100;

        // 默认清理一个月之前的任务,天数单位
        private int beforeDays = 30;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getBeforeDays() {
            return beforeDays;
        }

        public void setBeforeDays(int beforeDays) {
            if (beforeDays < 1) {
                throw new IllegalArgumentException("cleanDays must be greater than 0");
            }
            this.beforeDays = beforeDays;
        }

        public int getLimitRows() {
            return limitRows;
        }

        public void setLimitRows(int limitRows) {
            if (limitRows < 1) {
                throw new IllegalArgumentException("limitRows must be greater than 0");
            }
            this.limitRows = limitRows;
        }
    }





    public static class DeadTask {
        // 是否开启死信任务,当开启死信任务时，任务执行超过 {@link #taskMaxExecuteTimeout} 秒，
        // 则任务进入死信队列，等待人工干预，默认关闭
        private Boolean deadTaskCheck = false;
        // 任务执行超时时间，单位秒
        private int taskMaxExecuteTimeout = 30 * 60;

        public Boolean getDeadTaskCheck() {
            return deadTaskCheck;
        }

        public void setDeadTaskCheck(Boolean deadTaskCheck) {
            this.deadTaskCheck = deadTaskCheck;
        }

        public int getTaskMaxExecuteTimeout() {
            return taskMaxExecuteTimeout;
        }

        public void setTaskMaxExecuteTimeout(int taskMaxExecuteTimeout) {
            this.taskMaxExecuteTimeout = taskMaxExecuteTimeout;
        }

    }

    private ClearTask clearTask = new ClearTask();

    public ClearTask getClearTask() {
        return clearTask;
    }

    public void setClearTask(ClearTask clearTask) {
        this.clearTask = clearTask;
    }

    private DeadTask deadTask = new DeadTask();

    public DeadTask getDeadTask() {
        return deadTask;
    }

    public void setDeadTask(DeadTask deadTask) {

        this.deadTask = deadTask;
    }

    // 定时任务查询的时间间隔，控制任务查询频率,单位秒
    private int taskFindInterval = 20;

    private Executor executor = new Executor();

    public int getTaskFindInterval() {
        return taskFindInterval;
    }

    public void setTaskFindInterval(int taskFindInterval) {
        if (taskFindInterval < 1) {
            throw new IllegalArgumentException("taskFindInterval must be greater than 0");
        }
        this.taskFindInterval = taskFindInterval;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public static class Executor {
        private String name = "smart-retry-executor";
        private int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;
        private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        private int queueCapacity = 3000;
        private int keepAliveSeconds = 60;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }

}
