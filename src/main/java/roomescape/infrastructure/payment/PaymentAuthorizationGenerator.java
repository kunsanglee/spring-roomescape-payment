package roomescape.infrastructure.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentAuthorizationGenerator {

    private static final String PAYMENT_AUTHORIZATION_PREFIX = "Basic ";
    private final String secretKey;

    public PaymentAuthorizationGenerator(@Value("${payment.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String createAuthorizations() {
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return PAYMENT_AUTHORIZATION_PREFIX + new String(encodedBytes);
    }
}
