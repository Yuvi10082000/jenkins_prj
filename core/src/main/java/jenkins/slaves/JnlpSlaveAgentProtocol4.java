package jenkins.slaves;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Computer;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import jenkins.AgentProtocol;
import jenkins.model.identity.InstanceIdentityProvider;
import org.jenkinsci.remoting.engine.JnlpClientDatabase;
import org.jenkinsci.remoting.engine.JnlpProtocol4Handler;
import org.jenkinsci.remoting.protocol.cert.PublicKeyMatchingX509ExtendedTrustManager;

/**
 * Master-side implementation for JNLP4-connect protocol.
 *
 * <p>@see {@link org.jenkinsci.remoting.engine.JnlpProtocol4Handler} for more details.
 *
 * @since FIXME
 */
@Extension
public class JnlpSlaveAgentProtocol4 extends AgentProtocol {
    /**
     * Our logger.
     */
    private static final Logger LOGGER = Logger.getLogger(JnlpSlaveAgentProtocol4.class.getName());

    /**
     * Our keystore.
     */
    private final KeyStore keyStore;
    /**
     * Our trust manager.
     */
    private final TrustManager trustManager;

    private IOHubProvider hub;

    private JnlpProtocol4Handler handler;
    private SSLContext sslContext;

    public JnlpSlaveAgentProtocol4() throws KeyStoreException, KeyManagementException, IOException {
        // prepare our local identity and certificate
        X509Certificate identityCertificate = InstanceIdentityProvider.RSA.getCertificate();
        RSAPrivateKey privateKey = InstanceIdentityProvider.RSA.getPrivateKey();

        // prepare our keyStore so we can provide our authentication
        keyStore = KeyStore.getInstance("JKS");
        char[] password = "password".toCharArray();
        try {
            keyStore.load(null, password);
        } catch (IOException e) {
            throw new IllegalStateException("Specification says this should not happen as we are not doing I/O", e);
        } catch (NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalStateException("Specification says this should not happen as we are not loading keys", e);
        }
        keyStore.setKeyEntry("jenkins", privateKey, password,
                new X509Certificate[]{identityCertificate});

        // prepare our keyManagers to provide to the SSLContext
        KeyManagerFactory kmf;
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Specification says the default algorithm should exist", e);
        } catch (UnrecoverableKeyException e) {
            throw new IllegalStateException("The key was just inserted with this exact password", e);
        }

        // prepare our trustManagers
        trustManager = new PublicKeyMatchingX509ExtendedTrustManager(false, true);
        TrustManager[] trustManagers = {trustManager};

        // prepare our SSLContext
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Java runtime specification requires support for TLS algorithm", e);
        }
        sslContext.init(kmf.getKeyManagers(), trustManagers, null);
    }

    @Inject
    public void setHub(IOHubProvider hub) {
        this.hub = hub;
        handler = new JnlpProtocol4Handler(new JnlpClientDatabase() {
            @Override
            public boolean exists(String clientName) {
                return JnlpAgentReceiver.exists(clientName);
            }

            @Override
            public String getSecretOf(@Nonnull String clientName) {
                return JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(clientName);
            }
        }, Computer.threadPoolForRemoting, hub.getHub(), sslContext, false);
    }

    @Override
    public String getName() {
        return handler.getName();
    }

    @Override
    public void handle(Socket socket) throws IOException, InterruptedException {
        try {
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate("jenkins");
            if (certificate == null
                    || certificate.getNotAfter().getTime() < System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)) {
                LOGGER.log(Level.INFO, "Updating {0} TLS certificate to retain validity", getName());
                X509Certificate identityCertificate = InstanceIdentityProvider.RSA.getCertificate();
                RSAPrivateKey privateKey = InstanceIdentityProvider.RSA.getPrivateKey();
                char[] password = "password".toCharArray();
                keyStore.setKeyEntry("jenkins", privateKey, password, new X509Certificate[]{identityCertificate});
            }
        } catch (KeyStoreException e) {
            // ignore
        }
        HashMap<String, String> headers = new HashMap<>();
        // TODO populate headers
        try {
            handler.handle(socket, headers, ExtensionList.lookup(JnlpAgentReceiver.class)).get();
        } catch (ExecutionException e) {
            LOGGER.log(Level.WARNING, "Unexpected", e);
        }
    }

}
