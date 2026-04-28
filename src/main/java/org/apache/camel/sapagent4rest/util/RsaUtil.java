package org.apache.camel.sapagent4rest.util;

import cn.hutool.crypto.asymmetric.KeyType;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@Slf4j
public class RsaUtil {

    @Value("${camel.custom.httpBase}")
    private String customHttpBase;

    private static Boolean quitInterfaceEncryption = false;

    @PostConstruct
    void init() {
        try {
            String httpBase = customHttpBase + "/userInfo/getSystemParam";
            String s = HttpUtil.doGet(httpBase, new HashMap<>());

            if (StringUtils.isNoneBlank(s)) {
                JSONObject jsonObject = JSONObject.parseObject(s);
                Boolean interfaceEncryption = jsonObject.getBoolean("quitInterfaceEncryption");
                if (interfaceEncryption != null) {
                    quitInterfaceEncryption = interfaceEncryption;
                }
            }
        } catch (Exception exception) {
            log.error("run param err", exception);
        }
        log.info("run param quitInterfaceEncryption={}", quitInterfaceEncryption);
    }


    public static String decryptStr(String str) {
        if (quitInterfaceEncryption || str == null) {
            return str;
        }
        RsaWrapper rsa = new RsaWrapper("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJD5+T1j2Oi4sPNeD/Q4xaLzYhMJNq2chUzihm4PlUgxcjJB6RioIPewE9NmkuFTShWfAvckZDQ5rD07Dwye6TQPnS69FiqDDl3Yqo4/sTNbXp2w1VbE2JxbvAajFx3j961ERrKQ3cTwqDJaS/y63h6yk2WulwqVuMngTN2Zja3jAgMBAAECgYAw+E7pHqXxDhmvSvGGo/qWLTHOjkkq4XjhXI7d9GOPbCxPr5XF31yrbY101ev9Mn4OyZRd2KwEPAri7+UIdVD94lSGuruBpQ9YhyB7hsl1+iaNax7JC58EAy/JUye9UbRPTovkknbL1jgsAXMoGo3AQjRSdKherBdqNvcxu2UN8QJBAKaLC674SxRTF+d9a0/dgHEG2o5MzpLPSahW4/m3utm9xvdCZxCa7wwpJMnU0oQwYDCfuz07PXAyrY2qPEX5j9sCQQDe2Vf+ZeQk7Y9NBS+G/BWWQJefPayPhZv+RsvTWbcA6NtFJpT0IbBI/YqOYcyUlTfmLF6mecW3iMo/B2q/XFyZAkBb3LRcFZ6sHk1AJcDsc5wmPAaPmTQUbHwPe6YSFtLcyb4WoqzgzuTuSz98iIR534kfKOwJkUSJP5rVWkIWwtojAkEAiFZSRlrR9GG+fTGB/UR2dIE6Aft6eigU4sEvbDOECbNsiubq1F1T+6PaQ/fDcNfF5jUZByKS+xFeIqeIAGEiaQJAFu+aFZNkMsuInD5wPu70GC0HZFr/5qh6Y8dvgRCjy7Ix+pzQkG/al5TRHWDzIHS8iuszgyMeOs7MJP3DaYArug==", null);
        return rsa.decryptStr(str, KeyType.PrivateKey);
    }

    public static String encrypt(String str) {
        if (quitInterfaceEncryption) {
            return str;
        }

        RsaWrapper rsa = new RsaWrapper(null, "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCIPmpicL5Owsio3iuRfeEob7zw4bEtdCAsyrXA6a8w8l47uSnPVSxy3OfZtFcOHquHqnZHnHJaYjR710kcbAFBdYx3Q1C2Csx1UZt4Us5b90uhs8Gf3quG4y2RucjW0idG0VpA7Wezu7WB9IbIceoAcvOUwjhJk/ScolyWtobIGwIDAQAB");
        if (str == null) {
            str = "";
        }

        return rsa.encryptBcd(str, KeyType.PublicKey);
    }

    public static Boolean getQuitInterfaceEncryption() {
        return quitInterfaceEncryption;
    }

    public static void setQuitInterfaceEncryption(Boolean quitInterfaceEncryption) {
        RsaUtil.quitInterfaceEncryption = quitInterfaceEncryption;
    }
}
