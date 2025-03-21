import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
public class WebClientConfig {

    @Value("${webclient.ssl.bypass:false}")
    private boolean bypassSsl;

    @Value("${webclient.ssl.truststore.path:}")
    private String trustStorePath;

    @Value("${webclient.ssl.truststore.password:}")
    private String trustStorePassword;

    @Bean
    public WebClient.Builder webClientBuilder() throws Exception {
        HttpClient httpClient;

        if (bypassSsl) {
            // Bypass SSL verification (insecure, for local/dev)
            SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
            httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        } else if (!trustStorePath.isEmpty()) {
            // Use custom truststore (for prod)
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                trustStore.load(fis, trustStorePassword.toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(tmf)
                .build();
            httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        } else {
            // Default SSL (JVM's cacerts)
            httpClient = HttpClient.create();
        }

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
