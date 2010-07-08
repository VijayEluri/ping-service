package dmitrygusev.ping.services.security;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry5.services.Dispatcher;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;

import dmitrygusev.ping.services.GAEHelper;

public class AccessController implements Dispatcher {

    private GAEHelper helper;
    private RequestGlobals globals;
    
    public AccessController(GAEHelper helper, RequestGlobals globals) {
        this.helper = helper; 
        this.globals = globals;
    }
    
    public boolean dispatch(Request request, Response response) throws IOException {
        Principal principal = helper.getUserPrincipal();

        String thisURL = request.getPath();

        if (principal != null 
                || thisURL.startsWith("/job/analytics/1026/5002")   //  Share PingService analytics with everyone
                || thisURL.startsWith("/assets")
                || thisURL.startsWith("/cron/")
                || thisURL.startsWith("/job/run/")
                || thisURL.startsWith("/task/")
                || thisURL.startsWith("/welcome")
                || thisURL.startsWith("/help")
                || thisURL.startsWith("/feedback")) {
            return false;
        } else {
            if (thisURL.equals("/")) 
            {
                forwardRequest("/welcome", request, response);
            }
            else
            {
                response.sendRedirect(helper.createLoginURL());
            }
            return true;
        }
    }

    private void forwardRequest(String forwardTo, Request request, Response response)
            throws IOException {
        try {
            HttpServletRequest servletRequest = globals.getHTTPServletRequest();
            HttpServletResponse servletResponse = globals.getHTTPServletResponse();
            
            servletRequest.getRequestDispatcher(forwardTo).forward(servletRequest, servletResponse);
            
            globals.storeRequestResponse(request, response);
        } catch (ServletException e) {
            throw new RuntimeException("Error forwarding request", e);
        }
    }
}
