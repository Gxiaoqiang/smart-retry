package com.smart.retry.mybatis.start.test;

import java.util.List;

/**
 * @Author xiaoqiang
 * @Version Merger.java, v 0.1 2025年11月15日 15:13 xiaoqiang
 * @Description: TODO
 */
public class Merger {
}

/**
 * 描述
 * 合并 k 个升序的链表并将结果作为一个升序的链表返回其头节点。
 *
 * 数据范围：节点总数0≤n≤5000
 * 每个节点的val满足∣val∣<=1000|val| <= 1000
 *         ∣val∣<=1000
 * 要求：时间复杂度O(nlogn)
 *
 *
 * 输入：
 *         [{1,2},{1,4,5},{3，6}]
 *
 * 返回值：
 *         {1,1,2,3，4,5,6}
 */

class ListNode {
    int val;
    ListNode next = null;
    public ListNode(int val) {
        this.val = val;
    }
}


