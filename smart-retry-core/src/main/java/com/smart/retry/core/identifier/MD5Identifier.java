package com.smart.retry.core.identifier;

import com.smart.retry.common.identifier.Identifier;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * @Author xiaoqiang
 * @Version MD5Identifier.java, v 0.1 2025年02月13日 19:15 xiaoqiang
 * @Description: TODO
 */
public class MD5Identifier implements Identifier {

    @Override
    public String identify( String taskCode, String argStr) {
        return DigestUtils.md5Hex( taskCode +":"+ argStr);
    }
}
