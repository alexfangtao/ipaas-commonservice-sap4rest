package org.apache.camel.sapagent4rest.service;

import cn.hutool.crypto.asymmetric.RSA;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.sap.conn.jco.JCoException;
import jakarta.annotation.PostConstruct;
import org.apache.camel.sapagent4rest.CustomConstants;
import org.apache.camel.sapagent4rest.entity.LicenseCacheEntity;
import org.apache.camel.sapagent4rest.entity.SapConfig;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.apache.camel.sapagent4rest.util.HttpUtil;
import org.apache.camel.sapagent4rest.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.sapagent4rest.util.RsaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.hutool.crypto.asymmetric.KeyType;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CacheService {
    @Autowired
    private CamelContext context;

    @Value("${camel.custom.cache.db}")
    private Long cacheDb;

    @Value("${camel.custom.cache.rfc}")
    private Long cacheRfc;

    @Value("${camel.custom.httpBase}")
    private String customHttpBase;

    @Value("${camel.custom.cache.license}")
    private Long cacheLicense;

    @Value("${camel.custom.sap.destination}")
    private String destinationConfig;

    @Autowired
    private RsaUtil rsaUtil;

    private LoadingCache<String, Boolean> rfcRefreshConfigCache;




    LoadingCache<String, SapConfig> configCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .build(new CacheLoader<String, SapConfig>() {
                @Override
                public SapConfig load(String s) throws Exception {
                    SapConfig sapConfig = null;
                    try {
                        sapConfig = getSapConfig(s);
                    } catch (Exception e) {
                        log.error("configCache执行load时异常", e);
                    }

                    return sapConfig;
                }

                @Override
                public SapConfig reload(String key, SapConfig oldValue) throws Exception {
                    try {
                        return getSapConfig(key);
                    } catch (Exception e) {
                        log.error("configCache执行reload时异常", e);
                        oldValue.setFailureTime(System.currentTimeMillis() + (cacheDb *
                                1000 * 60));
                    }

                    return oldValue;
                }
            });


    LoadingCache<String, LicenseCacheEntity> licenseCache = Caffeine.newBuilder()
            .maximumSize(10)
            .build(new CacheLoader<String, LicenseCacheEntity>() {
                @Override
                public LicenseCacheEntity load(String s) throws Exception {
                    LicenseCacheEntity license = null;
                    try {
                        license = getLicense();
                    } catch (Exception e) {
                        log.error("licenseCache执行load时发生异常", e);
                    }

                    return license;
                }

                @Override
                public LicenseCacheEntity reload(String key, LicenseCacheEntity oldValue) throws Exception {
                    try {
                        return getLicense();
                    } catch (Exception e) {
                        log.error("licenseCache执行reload时发生异常", e);
                        oldValue.setFailureTime(System.currentTimeMillis() + (cacheLicense *
                                1000 * 60));
                    }

                    return oldValue;
                }
            });

    @PostConstruct
    public void init() {
        rfcRefreshConfigCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(cacheRfc, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Boolean>() {
                    @Override
                    public Boolean load(String s) throws Exception {
                        try {
                            String saveHttpPath = customHttpBase + "/sapConfigInfo/b";
                            SapConfig sapConfig = getConfigCacheValue(s);
                            removeCamelCache(context, sapConfig, saveHttpPath);
                        } catch (Exception exception) {
                            log.error("rfcRefresh error", exception);
                        }

                        return true;
                    }
                });
    }

    /**
     * 包装缓存获取逻辑，支持穿刺异常时继续使用旧缓存
     *
     * @param s
     * @return
     */
    public SapConfig getConfigCacheValue(String s) {
        SapConfig sapConfig = configCache.get(s);

        if (sapConfig != null && sapConfig.getFailureTime() <= System.currentTimeMillis()) {
            CompletableFuture<SapConfig> refresh = configCache.refresh(s);

            try {
                sapConfig = refresh.get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("getConfigCacheValue方法缓存refresh阻塞线程结果时异常", e);
                return sapConfig;
            }
        }
        return sapConfig;
    }

    /**
     * 包装缓存获取逻辑，支持穿刺异常时继续使用旧缓存
     *
     * @param key
     * @return
     */
    public LicenseCacheEntity getLicenseCacheValue(String key) {
        LicenseCacheEntity licenseCacheEntity = licenseCache.get(key);
        if (licenseCacheEntity != null && licenseCacheEntity.getFailureTime() <= System.currentTimeMillis()) {
            CompletableFuture<LicenseCacheEntity> refresh = licenseCache.refresh(key);

            try {
                licenseCacheEntity = refresh.get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("getLicenseCacheValue方法缓存refresh阻塞线程结果时异常", e);
                return licenseCacheEntity;
            }
        }
        return licenseCacheEntity;
    }

    public void verifyConfig(Exchange exchange) throws Exception {
        String desName = exchange.getIn().getHeader(CustomConstants.DES_NAME, String.class);
        String rfc = exchange.getIn().getHeader(CustomConstants.RFC_LOWERCASE, String.class);
        String id = exchange.getIn().getHeader(CustomConstants.ID, String.class);


        if (isEmptyHeader(rfc) || isEmptyHeader(desName)) {
            throw new RequestParamException(CustomConstants.RFC + "和SAP实例" + "不能为空!");
        }
        String error = String.format("请求%s/%s/%s：", id,desName, rfc);
        // 验证请求实例是否为本服务实例
        String destination = destinationConfig;
        if (!desName.equals(destination)) {
            throw new RequestParamException(error + desName + "与服务实例" + destination + "不匹配!");
        }

        String key = desName + CustomConstants.STRING_AT + rfc;
        // 1.缓存读取
        SapConfig config = getConfigCacheValue(key);
        if (config == null) {
            throw new RequestParamException("未注册的SAP实例(" + desName + ")rfc(" + rfc + ")！");
        }
        if (!config.getSvcNo().equals(id)) {
            throw new RequestParamException("id(" + id+ ")与SAP实例(" + desName + ")rfc(" + rfc + ")不匹配！");
        }
        //控制RFC的刷新时间
        rfcRefreshConfigCache.get(key);
        LicenseCacheEntity l = getLicenseCacheValue("l");
        if (l == null || l.getLicenseTime() <= System.currentTimeMillis()) {
            throw new RequestParamException("license过期或错误!");
        }
        exchange.setProperty(CustomConstants.SVC_NO, config.getSvcNo());
    }

    private SapConfig getSapConfig(String key) throws IOException {
        String httpBase = customHttpBase + "/sapConfigInfo/a";
        String desName = destinationConfig;
        String rfc = key.substring(desName.length() + 1, key.length());

        //2.缓存不存在数据库数据
        HashMap<String, String> map = new HashMap<>();
        map.put("destination", desName);
        map.put("rfcName", rfc);

        String res = rsaUtil.decryptStr(HttpUtil.doPost(httpBase, map));
        List<SapConfig> sapConfigs = JSONArray.parseArray(res, SapConfig.class);
        if (sapConfigs == null || sapConfigs.isEmpty()) {
            return null;
        }

        SapConfig config = sapConfigs.get(0);
        config.setFailureTime(System.currentTimeMillis() + (cacheDb *
                1000 * 60));
        return config;
    }

    private LicenseCacheEntity getLicense() throws Exception{
        String url = customHttpBase + "/licenseInfo/c";
        String str = rsaUtil.decryptStr(HttpUtil.doGet(url, new HashMap<>()));
        RSA rsa = new RSA("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIA+twFFr1hOFUAJ3ckSLAeHblllRC0Ju9LYdoHGjtvIeMcucgCQeRkCW3xVLygqGrIrZvT2oMPZm+FZu/4tO+hWsJ3nmVzRDfll4QijrSK44M+Ff0mrdjBudUpfcoZnGiR5P6DSUojqllFfbtsgr9s89WTdIAPTyhelSrj5WQJ5AgMBAAECgYAEayS/BHmgH0CYLj7X+KpPsBjbN6P7sUQpZY/ftMmjROr0YeNHpbKma/Be/khbp+e3j8tCUWUEmnDGeOMDROe1cvIEzElqFhVoSajahOxIrE+xo9Ma3r/v07uqS0J8vo/9aTtDLgQsRWhxTd74zuE4vxu+yy3U9utq+ibC8Sy+KQJBANSOAyyGKXtCBKVaCgaJNXuuD79/9VcPMRHmByuH8T7hufFYKBk/hjB1I120yj+tJ+hxZ7tI3sGzA0YVALqHwecCQQCadSuWxWIaOBLJ6+C1T7wxl6h5C36/kDNTF/1CVvfmm0gnO4yIK3eM7PJWC9hvZ9+CQ2BX9muBJ2rP7xOh8UyfAkB9x+P87w+RDwosx1FzeLKbk+9hxVjrweOp0dOgYPvT2EPum9puxnakKk1ZYGjmsZMSLDnUTFT1jvd6+2bI+xk1AkAJgKvt2rbuZgTB54ErpnwtkOcMi2iA4J5HvnIWYsNdrLADueYreoEganN+V7w5Hmrh2MNUphR3HbW0lUDf9biBAkEA0UUDiZjnj4XfIzcXGyO46/dEY31bKf445WuscqfJnwUjt14GElmk5MSsyjXD5q9jwhzmQXe9Zh8izc5bX34JsQ==", null);
        String s = rsa.decryptStr(str, KeyType.PrivateKey);
        JSONObject jsonObject = JSONObject.parseObject(s);
        LicenseCacheEntity licenseCache = new LicenseCacheEntity();
        licenseCache.setLicenseTime(jsonObject.getLong("endDate"));
        licenseCache.setFailureTime(System.currentTimeMillis() + (cacheLicense *
                1000 * 60));
        return licenseCache;
    }

    /**
     * 这里做清除camel缓存操作
     * 1.内存缓存失效。
     * 2.db查询是否注册。
     * 3.检查RefreshMark字段，本机是否有更新。
     * 4.没有则删除一次camel缓存，在sap有字段更新情况，执行清除RefreshMark字段字段，字段刷新
     */
    private void removeCamelCache(CamelContext context, SapConfig config, String saveHttpPath) throws JCoException, IOException {
        String ip = IpUtil.getLocalIpOrFallback(null);
        if (ip == null) {
            log.error("Cannot resolve local IPv4 for refreshMark, skip cache refresh once. config={}",
                    config.getRfcName());
            return;
        }
        String refreshMark = config.getRefreshMark();
        Set<String> marked = refreshMark == null ? Collections.emptySet() : Arrays.stream(refreshMark.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        if (!marked.contains(ip)) {
            SapManage.removeCamelCache(context, config.getDestination(), config.getRfcName());
            HashMap<String, String> markMap = new HashMap<>();
            markMap.put("configId", config.getConfigId());
            markMap.put("ip", ip);
            HttpUtil.doPost(saveHttpPath, markMap);
        }
    }

    private boolean isEmptyHeader(String value) {
        return value == null || "".equals(value) || "null".equals(value);
    }
}
