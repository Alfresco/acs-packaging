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

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * Servlet responsible for streaming node content from the repo directly to the response stream.
 * The appropriate mimetype is calculated based on filename extension.
 * <p>
 * The URL to the servlet should be generated thus:
 * <pre>/alfresco/download/attach/workspace/SpacesStore/0000-0000-0000-0000/myfile.pdf</pre>
 * or
 * <pre>/alfresco/download/direct/workspace/SpacesStore/0000-0000-0000-0000/myfile.pdf</pre>
 * or
 * <pre>/alfresco/download/[direct|attach]?path=/Company%20Home/MyFolder/myfile.pdf</pre>
 * The protocol, followed by either the store and Id (NodeRef) or instead specify a name based
 * encoded Path to the content, note that the filename element is used for mimetype lookup and
 * as the returning filename for the response stream.
 * <p>
 * The 'attach' or 'direct' element is used to indicate whether to display the stream directly
 * in the browser or download it as a file attachment.
 * <p>
 * By default, the download assumes that the content is on the
 * {@link org.alfresco.model.ContentModel#PROP_CONTENT content property}.<br>
 * To retrieve the content of a specific model property, use a 'property' arg, providing the workspace,
 * node ID AND the qualified name of the property.
 * <p>
 * Like most Alfresco servlets, the URL may be followed by a valid 'ticket' argument for authentication:
 * ?ticket=1234567890
 * <p>
 * And/or also followed by the "?guest=true" argument to force guest access login for the URL. If the 
 * guest=true parameter is used the current session will be logged out and the guest user logged in. 
 * Therefore upon completion of this request the current user will be "guest".
 * <p>
 * If the user attempting the request is not authorised to access the requested node the login page
 * will be redirected to.
 * 
 * @author Kevin Roast
 * @author gavinc
 */
public class DownloadContentServlet extends HttpServlet
{
   private static final long serialVersionUID = -576405943603122206L;

   private static Log logger = LogFactory.getLog(DownloadContentServlet.class);

   private static final String URL_ATTACH = "a";
   private static final String URL_ATTACH_LONG = "attach";
   private static final String URL_DIRECT = "d";
   private static final String URL_DIRECT_LONG = "direct";

   /**
    * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
    */
   protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
      throws IOException
   {
      if (logger.isDebugEnabled())
      {
         String queryString = req.getQueryString();
         logger.debug("Authenticating (GET) request to URL: " + req.getRequestURI() +
               ((queryString != null && queryString.length() > 0) ? ("?" + queryString) : ""));
      }

      // remove request context.
      String requestURI = req.getRequestURI();
      requestURI = requestURI.substring(req.getContextPath().length());

      StringTokenizer t = new StringTokenizer(requestURI, "/");
      int tokenCount = t.countTokens();
      t.nextToken();     // skip servlet name

      // expect a minimum of 6 URL tokens.
      // /d/{attach|direct}/{storeType}/{storeId}/{nodeId}/{nodeName}
      if(tokenCount < 6)
      {
         throw new IllegalArgumentException("Download URL did not contain all required args: " + requestURI);
      }

      // attachment mode (either 'attach' or 'direct')
      String attachToken = t.nextToken();
      boolean isAttachment = URL_ATTACH.equals(attachToken) || URL_ATTACH_LONG.equals(attachToken);
      boolean isDirect = URL_DIRECT.equals(attachToken) || URL_DIRECT_LONG.equals(attachToken);
      if (!(isAttachment || isDirect))
      {
         throw new IllegalArgumentException("Attachment mode is not properly specified: " + requestURI);
      }

      // assume 'workspace' or other NodeRef based protocol for remaining URL elements
      StoreRef storeRef = new StoreRef(URLDecoder.decode(t.nextToken()), URLDecoder.decode(t.nextToken()));
      String id = URLDecoder.decode(t.nextToken());

      // build noderef from the appropriate URL elements
      NodeRef nodeRef = new NodeRef(storeRef, id);

      // build redirect URL to V1 GET /nodes/{nodeId}
      String redirectUrl = String
          .format("%s://%s:%s%s/api/-default-/public/alfresco/versions/1/nodes/%s/content?attachment=%b",
              req.getScheme(), req.getServerName(), req.getServerPort(), req.getContextPath(), nodeRef.getId(), isAttachment);

      if (logger.isDebugEnabled())
      {
         logger.debug("Request redirected to URL: " + redirectUrl);
      }
      res.sendRedirect(redirectUrl);
   }
}
