package com.smart.retry.common;

import com.smart.retry.common.model.RetryTask;

import java.util.Date;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version RetryTaskAcess.java, v 0.1 2025年02月12日 15:34 xiaoqiang
 * @Description: TODO
 */
public interface RetryTaskAccess {


    /**
     * 获取所有死任务 执行时间超过设置的最大的执行时间
     * @return
     */
    List<RetryTask> listDeadTask(int maxExecuteTime);
    /**
     * 获取所有待重试任务
     * @return
     */
    List<RetryTask> listRetryTask();

    /**
     * 获取待重试任务（支持预加载窗口和数量限制）
     * @param maxNextPlanTime 最大下次执行时间，null 则默认 now()
     * @param limit 每次拉取的最大数量
     * @return 待重试任务列表
     */
    List<RetryTask> listRetryTask(java.util.Date maxNextPlanTime, int limit);


    RetryTask getRetryTask(long taskId);

    /**
     * 保存重试任务
     * @param retryTask
     * @return
     */
    long saveRetryTask(RetryTask retryTask);

    /**
     * 更新重试任务
     * @param retryTask
     */
    void updateRetryTask(RetryTask retryTask);

    /**
     * 原子认领重试任务（乐观锁 CAS）。
     *
     * <p>仅当任务仍为 WAITING(0)/FAIL(3) 且 {@code retry_num >= 1}
     * （且调用方传入的 {@code shardingKey} 与任务本身一致）时，
     * 原子地将任务置为 RUNNING(1)、{@code retry_num - 1}，并写入 executor 与 next_plan_time。
     * 并发下只有一个调用方受影响行数为 1，其余返回 0。
     *
     * @param id          任务 ID
     * @param executor    认领执行方标识（通常是 IP）
     * @param nextPlanTime 认领时计算好的下次执行时间
     * @param shardingKey 任务分片键
     * @return 受影响行数：1=认领成功，0=已被他人认领/状态不满足
     */
    int claimRetryTask(Long id, String executor, Date nextPlanTime, Long shardingKey);

    /**
     * 条件化写入终态（乐观锁 CAS 守卫）。
     *
     * <p>仅当任务仍由 {@code executor} 持有租约（DB {@code executor} == 传入 {@code executor}）
     * 且 {@code retry_num} 与内存副本一致（== 传入 {@code retryNum}，认领后扣减值）时，
     * 原子地将任务写入终态 {@code status}（SUCCESS/FAIL）与 {@code next_plan_time}。
     *
     * <p>用于替代 finally 中的无条件 {@link #updateRetryTask}：防止 stale 副本覆盖
     * DeadLetterTask 复活后的状态（retry_num 已 +1）或新租约持有者写入的 RUNNING
     * （executor 已变更），从而彻底关闭跨实例"认领后覆盖复活状态"的竞态窗口。
     *
     * @param id          任务 ID
     * @param status      终态：SUCCESS(2) 或 FAIL(3)
     * @param executor    认领时写入的租约持有者标识（通常是 IP）
     * @param retryNum    认领后内存中的剩余重试次数（扣减后值）
     * @param nextPlanTime 下次执行时间（FAIL 时用于重试调度）
     * @param attribute   错误信息/执行上下文，null 时不更新（保留历史）
     * @return 受影响行数：1=写入成功，0=租约已失效/状态漂移（任务已被复活或转交他人）
     */
    int markRetryTaskTerminal(Long id, int status, String executor, int retryNum, Date nextPlanTime, String attribute);

    /**
     * 条件化标记"未注册 taskCode"任务为失败（乐观锁 CAS 守卫）。
     *
     * <p>当本实例未注册任务对应的 taskCode（无法执行）时，将任务原子地置为 FAIL(3)
     * 并扣减一次 {@code retry_num}。仅当任务仍为 WAITING(0)/FAIL(3)（未被他人认领成 RUNNING、
     * 未被写为终态）且 {@code retry_num} 与内存副本一致（== 传入 {@code retryNum}，扣减前值）
     * 时生效，防止分片重叠窗口下覆盖他方已认领的 RUNNING。
     *
     * @param id        任务 ID
     * @param executor  本实例标识（通常是 IP）
     * @param retryNum  扣减前的剩余重试次数（用于 CAS 守卫）
     * @param attribute 失败原因
     * @return 受影响行数：1=写入成功，0=任务已被认领/状态已变化
     */
    int markNullTaskObjectFail(Long id, String executor, int retryNum, String attribute);

    /**
     * 条件化复活死信任务（乐观锁 CAS 守卫）。
     *
     * <p>仅当任务仍为 RUNNING(1) 且 {@code gmt_modified < deadTaskTime}（确认超时）时，
     * 原子地将任务置为 WAITING(0)、{@code retry_num + 1} 并清空 executor。
     * 防止复活已终态任务，或覆盖认领方在超时窗口内刚写入的终态（先查后改的 TOCTOU）。
     *
     * @param id           任务 ID
     * @param deadTaskTime 超时判定时间点（通常为 now - maxExecuteTime）
     * @return 受影响行数：1=复活成功，0=任务已终态/已被其他实例复活
     */
    int reviveDeadRetryTask(Long id, Date deadTaskTime);


    /**
     * 删除重试任务
     * @param taskId
     */
    void deleteRetryTask(long taskId);


    /**
     * 停止重试任务
     * @param taskId
     */
    void stopRetryTask(long taskId);

    /**
     * 删除历史的重试任务
     * @param clearBeforeDays 多少天之前的任务
     * @param limitRows 每次限制删除的条数
     * @return
     */

    int  deleteHistoryRetryTask(int clearBeforeDays, int limitRows);

}
