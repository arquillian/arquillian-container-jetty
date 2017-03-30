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
package org.jboss.arquillian.container.jetty;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;

public class EnvUtil {
    public static boolean classExists(String className) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(className, false, cl);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void assertMinimumJettyVersion(String version, String minimum) throws LifecycleException {
        if (!VersionUtil.isGreaterThenOrEqual(version, minimum)) {
            throw new LifecycleException("Incompatible Jetty container version on the classpath: "
                + "[actual:" + version + "], "
                + "[minimum:" + minimum + "]");
        }
    }
}
