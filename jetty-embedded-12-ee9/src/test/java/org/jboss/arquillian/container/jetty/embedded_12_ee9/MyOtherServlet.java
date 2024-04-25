package org.jboss.arquillian.container.jetty.embedded_12_ee9;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class MyOtherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String URL_PATTERN = "Test2";

    public static final String MESSAGE = "hey there";

    @Inject
    MyOtherBean bean;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        assertThat(bean.ping(), is(MyOtherBean.class.getSimpleName()));
        response.getWriter().append(MESSAGE);
    }
}
