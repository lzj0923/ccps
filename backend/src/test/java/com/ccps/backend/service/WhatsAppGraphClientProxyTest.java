package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

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

    @Test
    void createsAuthenticatedSocks5ProxySettings() {
        WhatsAppGraphClient.ProxySettings settings = WhatsAppGraphClient.proxySettings(
                "socks5://proxy-user:proxy-password@proxy.example:51033");

        assertThat(settings.proxy().type()).isEqualTo(Proxy.Type.SOCKS);
        assertThat(settings.proxy().address()).isEqualTo(new InetSocketAddress("proxy.example", 51033));
        assertThat(settings.username()).isEqualTo("proxy-user");
        assertThat(settings.password()).isEqualTo("proxy-password");
    }

    @Test
    void authenticatesAndSendsGraphHostnameToSocksProxy() throws Exception {
        Authenticator previousAuthenticator = Authenticator.getDefault();
        try (ServerSocket proxyServer = new ServerSocket(0)) {
            proxyServer.setSoTimeout((int) Duration.ofSeconds(5).toMillis());
            CompletableFuture<SocksHandshake> handshake = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = proxyServer.accept()) {
                    socket.setSoTimeout((int) Duration.ofSeconds(5).toMillis());
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                    assertThat(input.readUnsignedByte()).isEqualTo(5);
                    int methodCount = input.readUnsignedByte();
                    input.readNBytes(methodCount);
                    output.write(new byte[]{5, 2});
                    output.flush();

                    assertThat(input.readUnsignedByte()).isEqualTo(1);
                    int usernameLength = input.readUnsignedByte();
                    String username = new String(input.readNBytes(usernameLength), StandardCharsets.UTF_8);
                    int passwordLength = input.readUnsignedByte();
                    String password = new String(input.readNBytes(passwordLength), StandardCharsets.UTF_8);
                    output.write(new byte[]{1, 0});
                    output.flush();

                    assertThat(input.readUnsignedByte()).isEqualTo(5);
                    assertThat(input.readUnsignedByte()).isEqualTo(1);
                    input.readUnsignedByte();
                    int addressType = input.readUnsignedByte();
                    assertThat(addressType).isEqualTo(3);
                    int hostLength = input.readUnsignedByte();
                    String host = new String(input.readNBytes(hostLength), StandardCharsets.US_ASCII);
                    input.readUnsignedShort();

                    output.write(new byte[]{5, 5, 0, 1, 0, 0, 0, 0, 0, 0});
                    output.flush();
                    return new SocksHandshake(username, password, host);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            WhatsAppGraphClient client = new WhatsAppGraphClient(
                    RestClient.builder(), true, "v25.0", "123", "token",
                    "socks5h://proxy-user:proxy-password@127.0.0.1:" + proxyServer.getLocalPort());

            assertThatThrownBy(() -> client.sendTemplate("60123456789", "hello_world", "en_US", List.of()))
                    .isInstanceOf(WhatsAppGraphClient.WhatsAppGraphException.class);
            assertThat(handshake.get(5, TimeUnit.SECONDS)).isEqualTo(
                    new SocksHandshake("proxy-user", "proxy-password", "graph.facebook.com"));
        } finally {
            Authenticator.setDefault(previousAuthenticator);
        }
    }

    private record SocksHandshake(String username, String password, String host) {
    }

    @Test
    void rejectsUnsupportedProxySchemes() {
        assertThatThrownBy(
                        () -> WhatsAppGraphClient.proxySettings("ftp://proxy.example:21"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid WhatsApp proxy URL");
    }
}
