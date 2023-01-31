package io.quarkus.resteasy.test.security;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

public class ProactiveAuthCompletionExceptionMapperTest {

    private static final String AUTHENTICATION_COMPLETION_EX = "AuthenticationCompletionException";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.form.enabled=true\n"), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("a d m i n", "a d m i n", "a d m i n");
    }

    @Test
    public void testAuthCompletionExMapper() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured
                .given()
                .filter(new CookieFilter())
                .redirects().follow(false)
                .when()
                .formParam("j_username", "a d m i n")
                .formParam("j_password", "a d m i n")
                .cookie("quarkus-redirect-location", "https://quarkus.io/guides")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(401)
                .body(Matchers.equalTo(AUTHENTICATION_COMPLETION_EX));
    }

    @Path("/hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "Hello";
        }

    }

    @Priority(Priorities.USER)
    @Provider
    public static class CustomAuthCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

        @Override
        public Response toResponse(AuthenticationCompletionException e) {
            return Response.status(UNAUTHORIZED).entity(AUTHENTICATION_COMPLETION_EX).build();
        }
    }
}
