/*
 * #%L
 * Alfresco Repository WAR Community
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.web.app.servlet;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.web.app.Application;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.URLDecoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Base servlet class containing useful constant values and common methods for Alfresco servlets.
 * 
 * @author Kevin Roast
 */
public abstract class BaseServlet extends HttpServlet
{
   private static final long serialVersionUID = -826295358696861789L;

   public static final String KEY_ROOT_PATH = "rootPath";
   
   /** an existing Ticket can be passed to most servlet for non-session based authentication */
   private static final String ARG_TICKET   = "ticket";
   
   /** forcing guess access is available on most servlets */
   private static final String ARG_GUEST    = "guest";

   protected static final String MSG_ERROR_CONTENT_MISSING = "error_content_missing";
   protected static final String MSG_ERROR_PERMISSIONS = "Forbidden";
   protected static final String MSG_ERROR_NOT_FOUND = "Not Found";
   protected static final String MSG_BAD_REQUEST = "Bad Request";
   
   private static Log logger = LogFactory.getLog(BaseServlet.class);

   /**
    * Return the ServiceRegistry helper instance
    * 
    * @param sc      ServletContext
    * 
    * @return ServiceRegistry
    */
   public static ServiceRegistry getServiceRegistry(ServletContext sc)
   {
      WebApplicationContext wc = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
      return (ServiceRegistry)wc.getBean(ServiceRegistry.SERVICE_REGISTRY);
   }
   
   /**
    * Perform an authentication for the servlet request URI. Processing any "ticket" or
    * "guest" URL arguments.
    * 
    * @return AuthenticationStatus
    * 
    * @throws IOException
    */
   public AuthenticationStatus servletAuthenticate(HttpServletRequest req, HttpServletResponse res) 
      throws IOException
   {
      return servletAuthenticate(req, res, true);
   }
   
   /**
    * Perform an authentication for the servlet request URI. Processing any "ticket" or
    * "guest" URL arguments.
    * 
    * @return AuthenticationStatus
    * 
    * @throws IOException
    */
   public AuthenticationStatus servletAuthenticate(HttpServletRequest req, HttpServletResponse res,
         boolean redirectToLoginPage) throws IOException
   {
      AuthenticationStatus status;
      
      // see if a ticket or a force Guest parameter has been supplied
      String ticket = req.getParameter(ARG_TICKET);
      if (ticket != null && ticket.length() != 0)
      {
         status = AuthenticationHelper.authenticate(getServletContext(), req, res, ticket);
      }
      else
      {
         boolean forceGuest = false;
         String guest = req.getParameter(ARG_GUEST);
         if (guest != null)
         {
            forceGuest = Boolean.parseBoolean(guest);
         }
         status = AuthenticationHelper.authenticate(getServletContext(), req, res, forceGuest);
      }
      if (status == AuthenticationStatus.Failure && redirectToLoginPage)
      {
         // authentication failed - now need to display the login page to the user, if asked to
         redirectToLoginPage(req, res, getServletContext());
      }
      
      return status;
   }
   
   /**
    * Check the user has the given permission on the given node. If they do not either force a log on if this is a guest
    * user or forward to an error page.
    * 
    * @param req
    *           the request
    * @param res
    *           the response
    * @param nodeRef
    *           the node in question
    * @param allowLogIn
    *           Indicates whether guest users without access to the node should be redirected to the log in page. If
    *           <code>false</code>, a status 403 forbidden page is displayed instead.
    * @return <code>true</code>, if the user has access
    * @throws IOException
    *            Signals that an I/O exception has occurred.
    * @throws ServletException
    *            On other errors
    */
   public boolean checkAccess(HttpServletRequest req, HttpServletResponse res, NodeRef nodeRef, String permission,
         boolean allowLogIn) throws IOException, ServletException
   {
      ServletContext sc = getServletContext();
      ServiceRegistry serviceRegistry = getServiceRegistry(sc);
      PermissionService permissionService = serviceRegistry.getPermissionService();

      // check that the user has the permission
      if (permissionService.hasPermission(nodeRef, permission) == AccessStatus.DENIED)
      {
         if (logger.isDebugEnabled())
            logger.debug("User does not have " + permission + " permission for NodeRef: " + nodeRef.toString());

         if (allowLogIn && serviceRegistry.getAuthorityService().hasGuestAuthority())
         {
            if (logger.isDebugEnabled())
               logger.debug("Redirecting to login page...");
            redirectToLoginPage(req, res, sc);
         }
         else
         {
            if (logger.isDebugEnabled())
               logger.debug("Forwarding to error page...");
            prettyPrintError(req, res, MSG_ERROR_PERMISSIONS, HttpServletResponse.SC_FORBIDDEN, "User does not ha");
//            Application
//                  .handleSystemError(sc, req, res, MSG_ERROR_PERMISSIONS, HttpServletResponse.SC_FORBIDDEN, logger);
         }
         return false;
      }
      return true;
   }
   
   /**
    * Redirect to the Login page - saving the current URL which can be redirected back later
    * once the user has successfully completed the authentication process.
    */
   public static void redirectToLoginPage(HttpServletRequest req, HttpServletResponse res, ServletContext sc)
         throws IOException
   {
      redirectToLoginPage(req, res, sc, AuthenticationHelper.getRemoteUserMapper(sc) == null);
   }
   
   /**
    * Redirect to the Login page - saving the current URL which can be redirected back later
    * once the user has successfully completed the authentication process.
    * @param sendRedirect allow a redirect status code to be set? If <code>false</code> redirect
    * will be via markup rather than status code (to allow the status code to be used for handshake
    * responses etc.
    */
   public static void redirectToLoginPage(HttpServletRequest req, HttpServletResponse res, ServletContext sc, boolean sendRedirect)
         throws IOException
   {
      // Alfresco Explorer was used as a default login page for servlets that extended BaseServlet
      // These page and Spring context was removed as part OF ACE-2091
      // If no ticket is provided then trow a login error.
      res.setContentType("text/html; charset=UTF-8");
      res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

      prettyPrintError(req, res, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED, "Servlet expects an authenticated user.");
   }

   /**
    * Method introduced in Alfresco 6.0 for MNT-21602
    * As part of ACE-2091 all error pages have been removed.
    * This method pretty prints an error page instead of showing the stacktrace.
    * @param req
    * @param res
    * @param message
    * @param status
    * @param actualCause
    * @throws IOException
    */
   public static void prettyPrintError(HttpServletRequest req, HttpServletResponse res, String message, int status, String actualCause) throws IOException
   {
      final PrintWriter out = res.getWriter();

      out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
      out.println("<html><head>");
      out.println("<link rel=\"stylesheet\" href=\"/alfresco/css/webscripts.css\" type=\"text/css\">");
      out.println("</head><body>");
      out.println("<table>\n" + "            <tbody><tr>\n"
          + "               <td><img src=\"/alfresco/images/logo/AlfrescoLogo32.png\" alt=\"Alfresco\"></td>\n"
          + "               <td><span class=\"title\">Servlet Status: " +  status +  " - " + message + "</span></td>\n"
          + "            </tr>\n" + "         </tbody></table>");
      out.println("<br>");
      out.println("<table>\n"
          + "            <tbody><tr><td>The Servlet <a href=\"" + req.getRequestURI() + "\">" + req.getRequestURI() + "</a> has responded with a status of " + status +" - " + message + ".</td></tr>\n"
          + "         </tbody></table>");
      out.println("<br>");
      out.println("<table>\n"
          + "            <tbody><tr><td>Cause: "+ actualCause + ".</td></tr>\n"
          + "         </tbody></table>");
      out.println("</body></html>");

      out.close();
   }

   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param context ServletContext context
    * @param args    The elements of the path to lookup
    * @param decode  True to decode the arg from UTF-8 format, false for no decoding
    */
   public static NodeRef resolveWebDAVPath(ServletContext context, String[] args, boolean decode)
   {
      WebApplicationContext wc = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
      return resolveWebDAVPath(wc, args, decode);
   }
   
   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param wc WebApplicationContext Context
    * @param args    The elements of the path to lookup
    * @param decode  True to decode the arg from UTF-8 format, false for no decoding
    */
   private static NodeRef resolveWebDAVPath(final WebApplicationContext wc, final String[] args, final boolean decode)
   {
      return AuthenticationUtil.runAs(new RunAsWork<NodeRef>()
      {
         public NodeRef doWork() throws Exception
         {
            NodeRef nodeRef = null;
            
            List<String> paths = new ArrayList<String>(args.length - 1);

            FileInfo file = null;
            try
            {
               // create a list of path elements (decode the URL as we go)
               for (int x = 1; x < args.length; x++)
               {
                  paths.add(decode ? URLDecoder.decode(args[x]) : args[x]);
               }
               
               if (logger.isDebugEnabled())
                  logger.debug("Attempting to resolve webdav path: " + paths);

               // get the company home node to start the search from
               nodeRef = new NodeRef(Application.getRepositoryStoreRef(wc), Application.getCompanyRootId());
               
               TenantService tenantService = (TenantService)wc.getBean("tenantService");         
               if (tenantService != null && tenantService.isEnabled())
               {
                  if (logger.isDebugEnabled())
                     logger.debug("MT is enabled.");
                  
                  NodeService nodeService = (NodeService) wc.getBean("NodeService");
                  SearchService searchService = (SearchService) wc.getBean("SearchService");
                  NamespaceService namespaceService = (NamespaceService) wc.getBean("NamespaceService");
                  
                  // TODO: since these constants are used more widely than just the WebDAVServlet, 
                  // they should be defined somewhere other than in that servlet
                  String rootPath = wc.getServletContext().getInitParameter(BaseServlet.KEY_ROOT_PATH);
                  
                  // note: rootNodeRef is required (for storeRef part)
                  nodeRef = tenantService.getRootNode(nodeService, searchService, namespaceService, rootPath, nodeRef);
               }
               
               if (paths.size() != 0)
               {
                  FileFolderService ffs = (FileFolderService)wc.getBean("FileFolderService");
                  file = ffs.resolveNamePath(nodeRef, paths);
                  nodeRef = file.getNodeRef();
               }
               
               if (logger.isDebugEnabled())
                  logger.debug("Resolved webdav path to NodeRef: " + nodeRef);
            }
            catch (FileNotFoundException fne)
            {
               if (logger.isWarnEnabled())
                  logger.warn("Failed to resolve webdav path", fne);
               
               nodeRef = null;
            }
            return nodeRef;
         }
      }, AuthenticationUtil.getSystemUserName());
   }
   
   /**
    * Resolve a name based into a NodeRef and Filename string
    *  
    * @param sc      ServletContext
    * @param path    'cm:name' based path using the '/' character as a separator
    *  
    * @return PathRefInfo structure containing the resolved NodeRef and filename
    * 
    * @throws IllegalArgumentException
    */
   public final static PathRefInfo resolveNamePath(ServletContext sc, String path)
   {
      StringTokenizer t = new StringTokenizer(path, "/");
      int tokenCount = t.countTokens();
      String[] elements = new String[tokenCount];
      for (int i=0; i<tokenCount; i++)
      {
         elements[i] = t.nextToken();
      }
      
      // process name based path tokens using the webdav path resolving helper 
      NodeRef nodeRef = resolveWebDAVPath(sc, elements, false);
      if (nodeRef == null)
      {
         // unable to resolve path - output helpful error to the user
         throw new IllegalArgumentException("Unable to resolve item Path: " + path);
      }
      
      return new PathRefInfo(nodeRef, elements[tokenCount - 1]);
   }
   
   /**
    * Simple structure class for returning both a NodeRef and Filename String
    * @author Kevin Roast
    */
   public static class PathRefInfo
   {
      PathRefInfo(NodeRef ref, String filename)
      {
         this.NodeRef = ref;
         this.Filename = filename;
      }
      public NodeRef NodeRef;
      public String Filename;
   }
}
