package org.openslides.keycloak.addons.emailtemplateprovider;

import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Event;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenSlidesEmailTemplateProvider implements EmailTemplateProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenSlidesEmailTemplateProvider.class);

    private final RealmModel realm;
    private final UserModel user;
    private final KeycloakSession session;

    public OpenSlidesEmailTemplateProvider(KeycloakSession session, RealmModel realm, UserModel user) {
        this.session = session;
        this.realm = realm;
        this.user = user;
    }

    public void sendEmailVerification(String link, String expirationInMinutes) {
        // Fetch template content from Realm attribute or other source
        String template = realm.getAttribute("emailVerificationTemplate");

        // Fetch dynamic subject from realm attribute or user attribute
        String subject = realm.getAttribute("emailVerificationSubject");
        if (subject == null) {
            subject = "Verify your email";
        }

        // You can also use user attributes to customize the subject
        subject = subject.replace("${username}", user.getUsername());

        // Prepare attributes for the email body
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", user.getUsername());
        attributes.put("link", link);
        attributes.put("expirationInMinutes", expirationInMinutes);

        // Generate the email content using a template engine (like FreeMarker)
        String emailBody = processTemplate(template, attributes);

        // Send the email with the custom subject
//        sendEmail(user.getEmail(), subject, emailBody);
    }

    @Override
    public void close() {
        // Clean-up resources if necessary
    }

    private String processTemplate(String template, Map<String, Object> attributes) {
        // Logic to process the template with the attributes (using FreeMarker or similar)
        return template;  // Simplified example, replace with actual templating logic
    }

    @Override
    public EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        return null;
    }

    @Override
    public EmailTemplateProvider setRealm(RealmModel realm) {
        return null;
    }

    @Override
    public EmailTemplateProvider setUser(UserModel user) {
        return null;
    }

    @Override
    public EmailTemplateProvider setAttribute(String name, Object value) {
        return null;
    }

    @Override
    public void sendEvent(Event event) throws EmailException {

    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {

    }

    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException {

    }

    @Override
    public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException {

    }

    @Override
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException {

    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {

    }

    @Override
    public void sendEmailUpdateConfirmation(String link, long expirationInMinutes, String address) throws EmailException {

    }

    @Override
    public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {

    }

    @Override
    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {

    }

    // Encodes and signs a token
//    public String encodeToken(KeycloakSession session, RealmModel realm, AccessToken token) {
//        // Retrieve the current active signing key for the realm
//        KeyWrapper activeKey = session.keys().getActiveKey(realm, KeyUse.SIG, Algorithm.RS256);
//
//        // Create JWT with necessary claims
//        JWSHeader jwsHeader = new JWSHeader(Algorithm.RS256);
//        SignedJWT signedJWT = new SignedJWT(jwsHeader, token.toJWTClaimsSet());
//
//        // Sign the JWT with the private key
//        JWSSigner signer = new RSASSASigner(activeKey.getPrivateKey());
//        signedJWT.sign(signer);
//
//        // Return the signed JWT in compact form
//        return signedJWT.serialize();
//    }

    private void sendBackchannelLogoutIfConfigured() {
        ClientModel client = getClientInContext();
        if (client == null) {
            log.warn("No client found in session context");
            return;
        }

        String backchannelLogoutUrl = client.getAttribute("backchannel.logout.url");
        if (backchannelLogoutUrl == null || backchannelLogoutUrl.isEmpty()) {
            log.warn("No backchannel logout URL configured for client: {}", client.getClientId());
            return;
        }

        log.info("Sending POST request to backchannel logout URL: {}", backchannelLogoutUrl);
        sendPostRequest(backchannelLogoutUrl);
    }

    private ClientModel getClientInContext() {
        return session.getContext().getClient();
    }

    private void sendPostRequest(String urlString) {
        try {
            HttpURLConnection connection = getHttpURLConnection(urlString);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                log.info("POST request to {} succeeded.", urlString);
            } else {
                log.warn("POST request to {} failed with response code: {}", urlString, responseCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            log.error("Error sending POST request to backchannel logout URL: " + urlString, e);
        }
    }

    private static HttpURLConnection getHttpURLConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Example payload, you might customize this as per the backchannel logout requirements
        String payload = "{}"; // Empty JSON object or replace with actual payload

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        return connection;
    }

}
