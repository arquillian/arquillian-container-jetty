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
package org.jboss.arquillian.container.jetty.embedded_11;

import java.sql.Connection;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * In-container test case for the Jetty Embedded 9 container
 *
 * @author Dan Allen
 *
 */
@ExtendWith(ArquillianExtension.class)
public class JettyEmbeddedInContainerTestCase {
    /**
     * Deployment for the test
     */
    @Deployment
    public static WebArchive getTestArchive() {
        return ShrinkWrap.create(WebArchive.class)
            .addClass(MyBean.class)
            // adding the configuration class silences the logged exception when building the configuration on the server-side, but shouldn't be necessary
            //.addClass(JettyEmbeddedConfiguration.class)
            .addAsLibraries(
                Maven.configureResolver()
                    .workOffline()
                    .loadPomFromFile("pom.xml")
                    .resolve("org.jboss.weld.servlet:weld-servlet-core")
                       .withTransitivity()
                    .as(GenericArchive.class))
            .addAsWebInfResource("jetty-env.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .setWebXML("in-container-web.xml");
    }

    // defined in jetty-env.xml, scoped to global
    @Resource(mappedName = "version") Integer version;

    // defined in web.xml, scoped to webapp (relative to java:comp/env)
    @Resource(name = "name") String name;

    // defined in jetty-env.xml, scoped to webapp (relative to java:comp/env)
    @Resource(name = "type") String containerType;

    @Resource(name = "jdbc/test") DataSource ds;

    @Inject MyBean testBean;

    @Test
    public void shouldBeAbleToInjectMembersIntoTestClass() throws Exception {
        assertThat(version, notNullValue());
        assertThat(version, is(6));
        assertThat(name, notNullValue());
        assertThat(name, is("Jetty"));
        assertThat(containerType, notNullValue());
        assertThat(containerType, is("Embedded"));
        assertThat(ds, notNullValue());

        try (Connection c = ds.getConnection()) {
            assertThat(c.getMetaData().getDatabaseProductName(), is("H2"));
        }
        // FIXME this
        //Assert.assertNotNull(testBean);
        //Assert.assertEquals("Jetty", testBean.getName());
    }
}
