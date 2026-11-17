package com.veertu.ankaMgmtSdk;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

import com.veertu.ankaMgmtSdk.exceptions.ClientException;

/**
 * Checks URLs before the OpenID client uses them for the next request.
 * The controller names the provider, and the provider names the token endpoint.
 * Both values arrive in HTTP response bodies, so a bad value can point this
 * server at a local metadata address.
 */
final class OutboundUrl {

    // Full-string match: http(s), a host, an optional port, then an optional path.
    // No user info and no whitespace. Callers use String.matches on this pattern
    // before the value becomes the next request URL.
    static final String URL_PATTERN =
            "(?i)https?://"
                    + "(?:"
                    + "(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)*[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
                    + "|\\[[0-9a-f:]+\\]"
                    + "|(?:\\d{1,3}\\.){3}\\d{1,3}"
                    + ")"
                    + "(?::\\d{1,5})?"
                    + "(?:[/?#][^\\s\\\\]*)?";

    private static final Set<String> BLOCKED_HOST_NAMES = Set.of(
            "metadata.google.internal",
            "metadata.internal");

    private static final InetAddress AWS_IPV6_METADATA = literalAddress("fd00:ec2::254");
    private static final InetAddress ALIBABA_METADATA = literalAddress("100.100.100.200");

    private OutboundUrl() {
    }

    static URI require(String url) throws ClientException {
        if (url == null || !url.matches(URL_PATTERN)) {
            throw new ClientException("Invalid URL");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new ClientException("Invalid URL");
        }
        if (uri.getUserInfo() != null || uri.getHost() == null) {
            throw new ClientException("Invalid URL");
        }
        int port = uri.getPort();
        if (port > 65535) {
            throw new ClientException("Invalid URL");
        }
        rejectBlockedHost(uri.getHost());
        return uri;
    }

    private static void rejectBlockedHost(String host) throws ClientException {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.chars().allMatch(Character::isDigit)
                || BLOCKED_HOST_NAMES.contains(normalizedHost)) {
            throw new ClientException("URL host is not allowed");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new ClientException("URL host is not allowed");
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new ClientException("URL host is not allowed");
            }
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        // Loopback and private addresses stay allowed. Controllers and IdPs
        // often live on the local network. Link-local includes 169.254.169.254.
        if (address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        return address.equals(AWS_IPV6_METADATA) || address.equals(ALIBABA_METADATA);
    }

    private static InetAddress literalAddress(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (UnknownHostException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
