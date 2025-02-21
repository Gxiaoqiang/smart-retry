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

}
