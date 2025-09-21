package com.smart.retry.test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version CharPlace.java, v 0.1 2025年09月21日 15:39 xiaoqiang
 * @Description: TODO
 */
public class CharPlace {

    /*
现在有一个大模型返回，当遇到大模型输出占位符的时候，需要对大模型的返回值做改写，请你实现该功能，
占位符：<url></url> <img></img>
{
  "钉钉官网":"https://www.dingtalk.com"
}
例如：
详情请访问，[钉钉官网](<url>钉钉官网</url>)
替换为:
详情请访问，[钉钉官网](https://www.dingtalk.com)
输入:
1:["详","情请","访问，","[","钉钉官","网]","(<","ur","l>","钉钉官网<","/","u","rl",">)"]
2:["详情请访问，[钉钉官网](<url>钉钉官网</url>)"]


public static void main(String args[]){
    List<String> lst new ArrayList();
    for (int i=0; i<lst.length(); i++) {
      // 实现replace函数
        String s = replace(lst.get(i));
        if (s.length() != 0) {
          send(s);
        }
    }
}
public static String replace(String str) {

}
*/

    //流式返回
    public static void main(String args[]) {
        List<String> list = new ArrayList();

        //list.add("详情请访问，[钉钉官网](<url>钉钉官网</url>)");
        list.add("详");
        list.add("情请");
        list.add("访问，");
        list.add("[");
        list.add("钉钉官");
        list.add("网]");
        list.add("(<");
        list.add("ur");
        list.add("l>");
        list.add("钉钉官网<");
        list.add("/");
        list.add("u");
        list.add("rl");
        list.add(">)");

        String targetUlr = "https://www.dingtalk.com";

        StringBuilder sb = new StringBuilder();
        int size = list.size();
        for (int i = 0; i < size; i++) {

            String tmp = list.get(i);
            StringBuilder tmpSb = new StringBuilder();
            int start = i;

            if (start + 1 < size
                    && start + 2 < size
                    && tmp.contains("<")
                    && list.get(start + 1).equals("ur")
                    && list.get(start + 2).equals("l>")
            ) {
                start++;
                tmpSb.append(tmp);
                int right = 0;
                for (; start < list.size(); start++) {
                    String startTmp = list.get(start);
                    tmpSb.append(startTmp);

                    if (startTmp.contains(">")) {
                        right++;
                    }
                    if (right == 2) {
                        break;
                    }
                }
            }
            i = start;
            if (tmpSb.length() > 0) {
                tmp = tmpSb.toString();
            }
            String result = replace(tmp, targetUlr);
            System.out.println(result);
            //send(result);

        }


    }

    public static String replace(String str, String targetUrl) {
        if (str.contains("<url>") && str.contains("</url>")) {
            String url = str.substring(str.indexOf("<url>") + 5, str.indexOf("</url>"));
            str = str.replace("<url>" + url + "</url>", targetUrl);
        }
        return str;
    }
}
