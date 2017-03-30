/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.jetty.embedded_7;

import org.jboss.arquillian.container.jetty.AbstractJettyEmbeddedConfiguration;

/**
 * A {@link org.jboss.arquillian.spi.client.container.ContainerConfiguration} implementation for the Jetty 7.x Embedded
 * containers
 *
 * @author Dan Allen
 * @author Ales Justin
 * @version $Revision: $
 */
public class JettyEmbeddedConfiguration extends AbstractJettyEmbeddedConfiguration {
    private boolean jettyPlus = true;
    private boolean jettyAnnotations = true;

    public boolean isJettyPlus() {
        return jettyPlus;
    }

    public void setJettyPlus(boolean jettyPlus) {
        this.jettyPlus = jettyPlus;
    }

    public boolean isJettyAnnotations() {
        return jettyAnnotations;
    }

    public void setJettyAnnotations(boolean jettyAnnotations) {
        this.jettyAnnotations = jettyAnnotations;
    }
}
