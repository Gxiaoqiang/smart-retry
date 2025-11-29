package com.smart.retry.core;

import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryContainer;
import com.smart.retry.common.SmartRetryExit;
import com.smart.retry.common.SmartRetryRunFlag;
import com.smart.retry.common.constant.RetryTaskStatus;
import com.smart.retry.common.innovation.SmartInnovation;
import com.smart.retry.common.model.RetryTask;
import com.smart.retry.common.utils.GsonTool;
import com.smart.retry.core.config.SmartExecutorConfigure;
import com.smart.retry.core.innovation.DefaultInnovation;
import org.slf4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.*;

/**
 * @Author xiaoqiang
 * @Version SimpleContainer.java, v 0.1 2025年02月18日 00:24 xiaoqiang
 * @Description: TODO
 */
public class SimpleContainer implements RetryContainer {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SimpleContainer.class);


    private static final Integer MAX_QUEUE_SIZE = 3000;

    private static RetryConfiguration retryConfiguration;

    private static SmartExecutorConfigure smartConfigure;

    private static ThreadPoolExecutor consumerExecutor;

    private static BlockingQueue<Runnable> consumerQueue;

    private static ThreadPoolTaskScheduler taskScheduler;

    public SimpleContainer(RetryConfiguration retryConfiguration, SmartExecutorConfigure smartExecutorConfigure) {
        this.retryConfiguration = retryConfiguration;
        this.smartConfigure = smartExecutorConfigure;
    }


    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (consumerExecutor != null) {
                consumerExecutor.shutdown();
            }
            if (taskScheduler != null) {
                taskScheduler.shutdown();
            }
        }));
    }


    @Override
    public void start() {
        initTaskExecutor(smartConfigure);

        Thread producerTask = new Thread(new ProducerTask());
        producerTask.start();

        if (smartConfigure.getDeadTask().getDeadTaskCheck()) {
            Thread deadLetterTask = new Thread(new DeadLetterTask());
            deadLetterTask.setDaemon(true);
            deadLetterTask.start();
        }


        if (smartConfigure.getClearTask().getEnabled()) {
            initTaskScheduler();
            CronTrigger trigger = new CronTrigger(smartConfigure.getClearTask().getCron());
            taskScheduler.schedule(new ClearTask(), trigger);

        }
    }



    private synchronized static void initTaskExecutor(SmartExecutorConfigure smartConfigure) {

        if (consumerExecutor != null) {
            return;
        }

        int corePoolSize = smartConfigure.getExecutor().getCorePoolSize();
        int maxPoolSize = smartConfigure.getExecutor().getMaxPoolSize();
        int queueSize = smartConfigure.getExecutor().getQueueCapacity();
        String name = smartConfigure.getExecutor().getName();
        consumerQueue = new ArrayBlockingQueue<>(queueSize);


        consumerExecutor = new ThreadPoolExecutor(corePoolSize,
                maxPoolSize,
                1L, TimeUnit.SECONDS,
                consumerQueue,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, name);
                    }
                },
                //采用拒绝策略为callerRunsPolicy，即当线程池队列满时，直接在调用者线程中运行被拒绝的任务
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void initTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("clear-task-scheduler-");
        scheduler.initialize();
        taskScheduler = scheduler;
    }


    @Override
    public void destroy() {

    }

    private static String getUniqueKey(RetryTask retryTask) {
        return retryTask.getTaskCode() + "-" + retryTask.getUniqueKey();
    }

    static class ConsumerTask implements Runnable {

        private RetryTask retryTask;
        private RetryConfiguration retryConfiguration;

        public ConsumerTask(RetryTask retryTask, RetryConfiguration retryConfiguration) {
            this.retryTask = retryTask;
            this.retryConfiguration = retryConfiguration;
        }

        @Override
        public void run() {
            String uniqueKey = getUniqueKey(retryTask);

            try {

                SmartInnovation innovation = new DefaultInnovation(retryTask, retryConfiguration);
                innovation.invoke();
            } catch (Throwable e) {
                LOGGER.error("[ConsumerTask-run error,retryTask:{} ", GsonTool.toJsonString(retryTask), e);
            } finally {
                RetryTaskCache.removeTaskFlag(uniqueKey);
            }

        }
    }


    class ClearTask implements Runnable {

        @Override
        public void run() {
            try {
                int deleteCount = retryConfiguration.getRetryTaskAcess().deleteHistoryRetryTask(smartConfigure.getClearTask().getBeforeDays(), smartConfigure.getClearTask().getLimitRows());
                LOGGER.info("[ClearTask#run] delete expired retry task count {},expire days {},limit rows {} ", deleteCount, smartConfigure.getClearTask().getBeforeDays(), smartConfigure.getClearTask().getLimitRows());
            } catch (Exception e) {
                LOGGER.error("[ClearTask#run] error ", e);
            }
        }


    }

    /**
     * 死信处理任务
     * 死信任务：当任务已经变更状态为执行中，20分钟后，任务状态没有变更，则认为任务执行失败，进行死信处理
     * 处理逻辑是：将任务状态设置为失败，并记录失败原因，通知相关人员进行处理
     */
    class DeadLetterTask implements Runnable {
        @Override
        public void run() {

            while (true) {
                if (SmartRetryExit.isExit()) {
                    return;
                }
                try {
                    TimeUnit.SECONDS.sleep(15);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!smartConfigure.getDeadTask().getDeadTaskCheck()) {
                    continue;
                }
                try {

                    List<RetryTask> allRetryTask = retryConfiguration.getRetryTaskAcess().listDeadTask(smartConfigure.getDeadTask().getTaskMaxExecuteTimeout());
                    if (CollectionUtils.isEmpty(allRetryTask)) {
                        continue;
                    }
                    //将任务重新设置为待执行状态,
                    // TODO 考虑超时的任务是否需要在内存中做线程的中断
                    for (RetryTask retryTask : allRetryTask) {

                        RetryTask retryTaskDo = new RetryTask();
                        retryTaskDo.setId(retryTask.getId());
                        retryTaskDo.setStatus(RetryTaskStatus.WAITING.getCode());
                        retryTaskDo.setRetryNum(retryTask.getRetryNum() + 1);
                        retryConfiguration.getRetryTaskAcess().updateRetryTask(retryTaskDo);

                    }
                    //List<RetryTask> deadLetterTasks = retryTaskRepo.listDeadTask(smartConfigure.getDeadTask().getMaxExecuteTime());
                    //List<RetryTask> deadLetterTasks = retryTaskRepo.findDeadLetterTasks();
                } catch (Exception e) {
                    LOGGER.error("[SimpleContainer#DeadLetterTask]run error,error msg {} ", e.getMessage(), e);

                }
            }

        }
    }

    class ProducerTask implements Runnable {
        private long SLEEP_BASE_TIME_MILLISECONDS = smartConfigure.getTaskFindInterval();
        private long MAX_SLEEP_TIME_MILLISECONDS = 20 * SLEEP_BASE_TIME_MILLISECONDS;

        private long sleepTimes = 0L;


        private void sleep() {
            try {
                long totalTime =
                        sleepTimes * SLEEP_BASE_TIME_MILLISECONDS + SLEEP_BASE_TIME_MILLISECONDS;
                long sleepTime = Math.min(totalTime, MAX_SLEEP_TIME_MILLISECONDS);
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        @Override
        public void run() {
            loop1:
            while (SmartRetryExit.isExit()) {

                if (!SmartRetryRunFlag.getFlag()) {
                    continue;
                }

                try {
                    List<RetryTask> allRetryTask = retryConfiguration.getRetryTaskAcess().listRetryTask();

                    //如果当前任务为空则，sleepTimes = 0 ,睡眠一段时间后，进入下个阶段
                    if (CollectionUtils.isEmpty(allRetryTask)) {
                        sleepTimes = 0;
                        sleep();
                        continue loop1;
                    }

                    //如果当前队列中积累的任务大于等于最大任务数，则线程睡眠，睡眠一定时间后重新拉取
                    if (consumerQueue.size() >= MAX_QUEUE_SIZE) {
                        sleepTimes++;
                        sleep();
                        continue loop1;
                    }
                    //如果当前队列中积累的任务+allRetryTask.size() 大于等于最大任务数，则线程睡眠，睡眠一定时间后重新拉取
                    if (consumerQueue.size() + allRetryTask.size() >= MAX_QUEUE_SIZE) {
                        sleepTimes++;
                        sleep();
                        continue loop1;
                    } else {
                        sleepTimes = 0L;
                    }
                    produceTask(allRetryTask);
                    sleep();

                } catch (Exception e) {
                    LOGGER.error("[ProducerTask-run,error ", e);
                }
            }
        }

        private void produceTask(List<RetryTask> allRetryTask) {
            for (RetryTask retryTask : allRetryTask) {
                doProduceTask(retryTask, retryConfiguration);


                //consumerExecutor.execute(new ConsumerTask(retryTask, retryConfiguration));
            }
        }
    }

    private static void doProduceTask(RetryTask retryTask, RetryConfiguration retryConfiguration) {
        //任务存在则不处理，避免重复处理
        if (checkTaskExists(retryTask)) return;

        CompletableFuture<Void> future = CompletableFuture.runAsync(new ConsumerTask(retryTask, retryConfiguration), consumerExecutor);
    }

    private static void initTaskConsumerExecutor(SmartExecutorConfigure smartConfigure) {
        if (consumerExecutor != null) {
            return;
        }
        initTaskExecutor(smartConfigure);
    }

    static void invokeTaskAsync(RetryTask retryTask,
                           RetryConfiguration retryConfiguration,
    SmartExecutorConfigure smartConfigure) {
        initTaskConsumerExecutor(smartConfigure);
        doProduceTask(retryTask, retryConfiguration);
    }

    static void invokeTaskSync(RetryTask retryTask,
                           RetryConfiguration retryConfiguration) {
        //任务存在则不处理，避免重复处理
        if (checkTaskExists(retryTask)) return;
        new ConsumerTask(retryTask, retryConfiguration).run();
    }

    private static boolean checkTaskExists(RetryTask retryTask) {
        String uniqueKey = getUniqueKey(retryTask);
        Boolean exists = RetryTaskCache.retryTasks.putIfAbsent(uniqueKey, true);
        //插入成功，则任务不存在
        if(exists == null){
            return false;
        }
        //插入失败，则任务已经存在
        return true;
    }

}
