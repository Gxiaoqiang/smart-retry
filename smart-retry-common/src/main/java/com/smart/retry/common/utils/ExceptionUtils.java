package com.smart.retry.common.utils;

/**
 * @author gao.gwq
 * @version 1.0
 * @date 2022/5/6  19:21
 * @Description TODO
 */
public class ExceptionUtils {
    public static String createStackTrackMessage(Throwable e) {
        if (e == null) {
            return "";
        }
        StringBuilder messsage = new StringBuilder();
        if (e != null) {
            messsage.append(e.getClass()).append(": ").append(e.getMessage()).append("\n");
            StackTraceElement[] elements = e.getStackTrace();
            for (StackTraceElement stackTraceElement : elements) {
                messsage.append("\t").append(stackTraceElement.toString()).append("\n");
            }
        }
        return messsage.toString();
    }

    // 定义要记录的每个异常堆栈的最大深度
    private static final int MAX_STACK_DEPTH = 6;

    /**
     * 创建一个精简版的异常堆栈跟踪消息。
     * 主要包含：
     * 1. 异常类型和消息
     * 2. 堆栈跟踪的前几行（由 MAX_STACK_DEPTH 控制）
     * 3. 如果有 cause，则递归处理 cause（同样精简）
     *
     * @param e 要处理的 Throwable 对象
     * @return 格式化后的字符串
     */
    public static String createConciseStackTraceMessage(Throwable e) {
        if (e == null) {
            return "";
        }

        StringBuilder message = new StringBuilder();

        // --- 记录当前异常 ---
        appendExceptionInfo(message, e, "");

        // --- 记录 Cause Chain (可选) ---
        Throwable cause = e.getCause();
        int depth = 0;
        while (cause != null && depth < 12) { // 防止循环引用导致的无限循环，限制深度
            // 使用 "Caused by: " 前缀，符合标准异常输出格式
            appendExceptionInfo(message, cause, "Caused by: ");
            cause = cause.getCause();
            depth++;
        }

        return message.toString();
    }

    /**
     * 将单个异常的信息（类型、消息、部分堆栈）追加到 StringBuilder。
     *
     * @param sb       目标 StringBuilder
     * @param t        要处理的 Throwable
     * @param prefix   添加到异常行前的前缀（例如 "Caused by: "）
     */
    private static void appendExceptionInfo(StringBuilder sb, Throwable t, String prefix) {
        // 添加异常类型和消息
        sb.append(prefix).append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");

        // 添加部分堆栈跟踪
        StackTraceElement[] elements = t.getStackTrace();
        int elementsToPrint = Math.min(elements.length, MAX_STACK_DEPTH);
        for (int i = 0; i < elementsToPrint; i++) {
            sb.append("\tat ").append(elements[i].toString()).append("\n");
        }

        // 如果堆栈被截断，添加提示
        if (elements.length > MAX_STACK_DEPTH) {
            sb.append("\t... ").append(elements.length - MAX_STACK_DEPTH).append(" more\n");
        }
    }


    // --- 用于测试的示例 ---

    public static void main(String[] args) {
        try {
            // 模拟一个带有深层调用和 cause 的异常
            level1();
        } catch (Exception e) {
            System.out.println("--- Original Full Stack Trace ---");
            e.printStackTrace(); // 标准完整堆栈输出

            System.out.println("\n--- Concise Stack Trace Message ---");
            String conciseMessage = createConciseStackTraceMessage(e); // 使用我们的新方法
            System.out.println(conciseMessage);

            System.out.println("\n--- Comparison with Standard toString() ---");
            System.out.println(e); // 标准 toString() 通常只包含第一行和消息
        }
    }

    // 示例方法链，用于生成深层堆栈
    private static void level1() throws Exception {
        try {
            level2();
        } catch (RuntimeException re) {
            // 抛出一个带 cause 的新异常
            throw new Exception("Error occurred in level1", re);
        }
    }

    private static void level2() throws RuntimeException {
        try {
            level3();
        } catch (IllegalArgumentException iae) {
            throw new RuntimeException("Wrapped in RuntimeException", iae);
        }
    }

    private static void level3() throws IllegalArgumentException {
        // 最终抛出原始异常
        int a = 1/0;
        //throw new IllegalArgumentException("Something went wrong deep inside level3!");
        // 故意制造一个较长的堆栈跟踪以便观察效果
//        for(int i=0; i< 20; i++){
//            new Exception().printStackTrace();
//        }
    }

}
