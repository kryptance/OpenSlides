package org.openslides.keycloak.addons.emailtemplateprovider;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.openslides.keycloak.addons.emailtemplateprovider.util.DockerComposeParser;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.util.List;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openslides.keycloak.addons.emailtemplateprovider.util.ContainerFactory.createContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockTest
public class KeycloakProviderIntegrationTest {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(KeycloakProviderIntegrationTest.class);

    private GenericContainer<?> keycloak;
    private Keycloak adminClient;

    @BeforeAll
    public void startKeycloakAndConfigureRealm() throws Exception {
        final var network = Network.newNetwork();

        final var compose = DockerComposeParser.parseComposeFile(System.getProperty("docker-compose.configfile"));
        final var keycloak = createContainer("keycloak", compose.services.get("keycloak"))
                .withNetwork(network).withCopyFileToContainer(
                        MountableFile.forHostPath(System.getProperty("keycloak.addon.path")),
                        "/opt/keycloak/providers/openslides-email-template-provider.jar"
                );

        keycloak.start();
        this.keycloak = keycloak;

        final var keycloakInit = createContainer("keycloak-init", compose.services.get("keycloak-init"))
                .withNetwork(network);
        keycloakInit.start();
        Thread.sleep(3000);

        final var proxy = createContainer("proxy", compose.services.get("proxy"))
                .withNetwork(network);
        proxy.setEnv(List.of("KEYCLOAK_HOST=keycloak", "KEYCLOAK_PORT=8080"));
        proxy.start();

        String keycloakUrl = "https://" + proxy.getHost() + ":" + proxy.getFirstMappedPort() + "/idp/";

        Client client = ClientBuilder.newClient().register(new KeycloakErrorLoggingFilter(keycloak));

        adminClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password("admin")
                .resteasyClient(client)
                .build();
    }

    @AfterAll
    public void stopKeycloakContainer() {
        if (adminClient != null) {
            adminClient.close();
        }
        if (keycloak != null) {
            keycloak.stop();
        }
    }

    @Test
    public void checkIfAddonsInstalled() {
        assertThat(keycloak.getLogs()).contains("Initializing OpenSlidesEmailTemplateProviderFactory");
    }

    @Test
    public void testProviderFunctionality() {
        // Retrieve the realm "os"
        RealmResource realmResource = adminClient.realm("os");

        // Search for the user with username "admin"
        UserRepresentation user = realmResource
                .users()
                .searchByUsername("admin", true)
                .get(0);

        // Find os-ui client
        final var client = realmResource.clients().query("os-ui").get(0);

        // Trigger a reset password email for the user using the os-ui client
        realmResource.users().get(user.getId())
                .resetPasswordEmail(client.getClientId());
    }
}
