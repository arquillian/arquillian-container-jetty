package org.jboss.arquillian.container.jetty.embedded_12_ee11;

import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.jboss.shrinkwrap.api.Archive;

/**
 * used to customise the {@link WebAppContext} created for a given {@link Archive}
 * You need to register your implementations using the {@link org.jboss.arquillian.core.spi.LoadableExtension} mechanism
 */
public interface WebAppContextProcessor {

    /**
     *
     * @param webAppContext the created {@link WebAppContext} for the {@link Archive}
     * @param archive The user defined deployment archive
     */
    void process(WebAppContext webAppContext, Archive<?> archive);

}
