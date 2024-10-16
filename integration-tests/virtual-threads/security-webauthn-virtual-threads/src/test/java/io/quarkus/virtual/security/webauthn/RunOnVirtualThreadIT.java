package io.quarkus.virtual.security.webauthn;

import static io.quarkus.virtual.security.webauthn.RunOnVirtualThreadTest.checkLoggedIn;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.vertx.core.json.JsonObject;

@QuarkusIntegrationTest
class RunOnVirtualThreadIT {

    @Test
    public void test() {

        RestAssured.get("/open").then().statusCode(200).body(Matchers.is("Hello"));
        RestAssured
                .given().redirects().follow(false)
                .get("/secure").then().statusCode(302);
        RestAssured
                .given().redirects().follow(false)
                .get("/admin").then().statusCode(302);
        RestAssured
                .given().redirects().follow(false)
                .get("/cheese").then().statusCode(302);

        CookieFilter cookieFilter = new CookieFilter();
        WebAuthnHardware hardwareKey = new WebAuthnHardware();
        String challenge = WebAuthnEndpointHelper.invokeRegistration("stef", cookieFilter);
        JsonObject registration = hardwareKey.makeRegistrationJson(challenge);

        // now finalise
        WebAuthnEndpointHelper.invokeCallback(registration, cookieFilter);

        // make sure our login cookie works
        checkLoggedIn(cookieFilter);

        // reset cookies for the login phase
        cookieFilter = new CookieFilter();
        // now try to log in
        challenge = WebAuthnEndpointHelper.invokeLogin("stef", cookieFilter);
        JsonObject login = hardwareKey.makeLoginJson(challenge);

        // now finalise
        WebAuthnEndpointHelper.invokeCallback(login, cookieFilter);

        // make sure our login cookie still works
        checkLoggedIn(cookieFilter);
    }
}
