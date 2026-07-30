package io.quarkiverse.quarkus.banner.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BannerResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/banner")
                .then()
                .statusCode(200)
                .body(is("Hello banner"));
    }
}
