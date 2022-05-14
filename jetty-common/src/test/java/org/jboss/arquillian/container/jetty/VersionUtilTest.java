/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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

import org.jboss.arquillian.container.jetty.VersionUtil.Version;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * VersionUtilTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 */
public class VersionUtilTest {

    @Test
    public void shouldBeAbleToExtract() {
        Version version = VersionUtil.extract("1.2");
        assertThat(version.getMajor(), is(1));
        assertThat(version.getMinor(), is(2));
    }

    @Test
    public void shouldBeAbleToExtractWithMultipleDigits() {
        Version version = VersionUtil.extract("10.300");
        assertThat(version.getMajor(), is(10));
        assertThat(version.getMinor(), is(300));
    }

    @Test
    public void shouldBeAbleToExtractWithBuild() {
        Version version = VersionUtil.extract("1.2.50.A");
        assertThat(version.getMajor(), is(1));
        assertThat(version.getMinor(), is(2));
    }

    @Test
    public void shouldReturnZeroVersionOnNull() {
        Version version = VersionUtil.extract(null);
        assertThat(version.getMajor(), is(0));
        assertThat(version.getMinor(), is(0));
    }

    @Test
    public void shouldReturnZeroVersionOnNullUnMatched() {
        Version version = VersionUtil.extract("243223.A");
        assertThat(version.getMajor(), is(0));
        assertThat(version.getMinor(), is(0));
    }

    @Test
    public void shouldBeGreaterEqual() {
        Version greater = VersionUtil.extract("7.1");
        Version then = VersionUtil.extract("7.1");

        assertThat(VersionUtil.isGreaterThenOrEqual(greater, then), is(true));
    }

    @Test
    public void shouldBeGreaterThen() {
        Version greater = VersionUtil.extract("7.2");
        Version then = VersionUtil.extract("7.1");

        assertThat(VersionUtil.isGreaterThenOrEqual(greater, then), is(true));
    }

    @Test
    public void shouldBeLessEqual() {
        Version less = VersionUtil.extract("7.1");
        Version then = VersionUtil.extract("7.1");

        assertThat(VersionUtil.isLessThenOrEqual(less, then), is(true));
    }

    @Test
    public void shouldBeLessThen() {
        Version less = VersionUtil.extract("7.1");
        Version then = VersionUtil.extract("7.2");

        assertThat(VersionUtil.isLessThenOrEqual(less, then), is(true));
    }
}
