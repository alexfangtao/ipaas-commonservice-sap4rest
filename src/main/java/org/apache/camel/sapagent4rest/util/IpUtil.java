package org.apache.camel.sapagent4rest.util;

import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.*;

/**
 * 获取本机IP 地址
 *
 * @author dingwen
 * 2021.04.28 11:49
 */
@Slf4j
public class IpUtil {
    /**
     * 默认放宽的网卡名前缀；可被系统属性 iputil.ifacePrefixes 覆盖（逗号分隔）。
     */
    private static final List<String> IFACE_PREFIXES = parsePrefixes(
            System.getProperty("iputil.ifacePrefixes",
                    "eth,ens,eno,enp,em,bond"));

    /**
     * 是否允许使用 UDP 探测公网 IP 兜底（默认关闭，避免合规风险）。
     */
    private static final boolean ALLOW_UDP_PROBE =
            Boolean.parseBoolean(System.getProperty("iputil.allowUdpProbe", "false"));

    private IpUtil() {
    }

    private static List<String> parsePrefixes(String csv) {
        List<String> list = new ArrayList<>();
        for (String p : csv.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 业务安全入口：拿不到时返回 fallback，不抛异常、不 NPE。
     */
    public static String getLocalIpOrFallback(String fallback) {
        try {
            return getLocalIp4Address().map(Inet4Address::getHostAddress).orElse(fallback);
        } catch (Exception e) {
            log.error("getLocalIpOrFallback failed, use fallback={}", fallback, e);
            return fallback;
        }
    }

    public static Optional<Inet4Address> getLocalIp4Address() throws SocketException {
        List<Inet4Address> all = collectIpv4Addresses();

        // 1. 优先按前缀挑
        Optional<Inet4Address> preferred = pickByPrefix(all);
        if (preferred.isPresent()) return preferred;

        // 2. 没匹配前缀就挑第一个有效地址（放宽兜底）
        if (!all.isEmpty()) return Optional.of(all.get(0));

        // 3. 显式允许时再尝试 UDP 探测
        if (ALLOW_UDP_PROBE) {
            return getIpBySocket();
        }
        return Optional.empty();
    }

    private static List<Inet4Address> collectIpv4Addresses() throws SocketException {
        List<Inet4Address> result = new ArrayList<>();
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        if (nis == null) return result;

        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            if (!isUsableInterface(ni)) continue;
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (isValidAddress(addr)) {
                    result.add((Inet4Address) addr);
                }
            }
        }
        return result;
    }

    private static Optional<Inet4Address> pickByPrefix(List<Inet4Address> addrs) throws SocketException {
        for (Inet4Address ip : addrs) {
            NetworkInterface ni = NetworkInterface.getByInetAddress(ip);
            if (ni == null) continue;
            String name = ni.getName();
            for (String p : IFACE_PREFIXES) {
                if (name.startsWith(p)) {
                    return Optional.of(ip);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 与原实现的差异：去掉强制 eth/ens 前缀，由 pickByPrefix 单独处理。
     */
    private static boolean isUsableInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual();
    }

    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address
                && address.isSiteLocalAddress()
                && !address.isLoopbackAddress();
    }

    /**
     * UDP 探测仅作为可选兜底，默认关闭。
     */
    private static Optional<Inet4Address> getIpBySocket() {
        try (DatagramSocket socket = new DatagramSocket()) {
            // 用 RFC 5737 文档地址，避免实际命中 8.8.8.8
            socket.connect(InetAddress.getByName("192.0.2.1"), 10002);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && !local.isAnyLocalAddress()) {
                return Optional.of((Inet4Address) local);
            }
        } catch (Exception e) {
            log.debug("UDP probe failed", e);
        }
        return Optional.empty();
    }
}