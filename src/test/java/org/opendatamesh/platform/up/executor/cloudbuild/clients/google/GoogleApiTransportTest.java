package org.opendatamesh.platform.up.executor.cloudbuild.clients.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.cloudbuild.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.cloudbuild.tasks.model.AccessToken;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.GoogleStub;
import org.opendatamesh.platform.up.executor.cloudbuild.testsupport.TestConfigs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HTTP mechanics of the Google clients against the stub, without the application. */
class GoogleApiTransportTest {

    private static final AccessToken TOKEN = new AccessToken("token");

    private GoogleStub stub;
    private GoogleApiTransport transport;

    @BeforeEach
    void setUp() throws Exception {
        stub = new GoogleStub();
        transport = new GoogleApiTransport(TestConfigs.cloudBuild(TestConfigs.valid(stub.baseUrl())), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void whenEmptyBodyOnSuccessThenBadGatewayNotAbsent() {
        stub.reply("GET", Fixtures.BUILD_PATH, 200, "");

        BadGatewayException failure = assertThrows(BadGatewayException.class,
                () -> transport.readJson(request(Fixtures.BUILD_PATH), TOKEN));

        assertTrue(failure.getMessage().contains("empty response"), failure.getMessage());
    }

    @Test
    void whenTruncatedInsideAMultiByteCharacterThenCutBeforeIt() {
        stub.replyText("GET", GoogleStub.LOG_OBJECT, 200, "abé-more");

        BoundedText text = transport.readText(request(GoogleStub.LOG_OBJECT), TOKEN, 3).orElseThrow();

        assertEquals("ab", text.text());
        assertTrue(text.truncated());
    }

    private GoogleRequest request(String path) {
        return new GoogleRequest("Test API", "read " + path, stub.baseUrl() + path);
    }
}
