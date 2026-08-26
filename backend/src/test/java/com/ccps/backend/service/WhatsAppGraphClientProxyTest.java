package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.junit.jupiter.api.Test;

class WhatsAppGraphClientProxyTest {

    @Test
    void createsHttpProxyFromHttpsProxyEnvironmentUrl() {
        Proxy proxy = WhatsAppGraphClient.proxy("http://127.0.0.1:7897");

        assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
        assertThat(proxy.address()).isEqualTo(new InetSocketAddress("127.0.0.1", 7897));
    }

    @Test
    void keepsDirectConnectionWhenProxyUrlIsBlank() {
        assertThat(WhatsAppGraphClient.proxy(" ")).isSameAs(Proxy.NO_PROXY);
    }
}
