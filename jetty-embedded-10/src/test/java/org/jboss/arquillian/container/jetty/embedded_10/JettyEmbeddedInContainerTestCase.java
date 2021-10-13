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
package org.jboss.arquillian.container.jetty.embedded_10;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * In-container test case for the Jetty Embedded 9 container
 *
 * @author Dan Allen
 *
 */
@RunWith(Arquillian.class)
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
    public void shouldBeAbleToInjectMembersIntoTestClass() {
        Assert.assertNotNull(version);
        Assert.assertEquals(Integer.valueOf(6), version);
        Assert.assertNotNull(name);
        Assert.assertEquals("Jetty", name);
        Assert.assertNotNull(containerType);
        Assert.assertEquals("Embedded", containerType);
        Assert.assertNotNull(ds);

        try (Connection c = ds.getConnection()) {
            Assert.assertEquals("H2", c.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(testBean);
        Assert.assertEquals("Jetty", testBean.getName());
    }
}
