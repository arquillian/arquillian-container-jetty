package org.jboss.arquillian.container.jetty;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JettyEmbeddedConfigurationTest {

    private static final String MIME_DEFINITIONS = "js application/javascript " +
        "txt text/plain " +
        "html text/html";

    @Test
    public void shouldConfigureMimeTypes() {
        DummyJettyEmbeddedConfiguration dummyJettyEmbeddedConfiguration = new DummyJettyEmbeddedConfiguration();
        dummyJettyEmbeddedConfiguration.setMimeTypes(MIME_DEFINITIONS);
        Map<String, String> mimeTypes = dummyJettyEmbeddedConfiguration.getMimeTypes();
        assertThat(mimeTypes.get("js"), is("application/javascript"));
        assertThat(mimeTypes.get("txt"), is("text/plain"));
        assertThat(mimeTypes.get("html"), is("text/html"));
    }
}
