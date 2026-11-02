package org.jboss.arquillian.container.jetty.embedded_13;

import jakarta.enterprise.context.Dependent;

@Dependent
public class MyOtherBean {

    public String ping() {
        return MyOtherBean.class.getSimpleName();
    }
}
