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

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A serlet that provides direct access to the content stores via a content URL.
 * <p>
 * Requests can be of the form:<br/>
 * <pre>
 * /alfresco/downloadDirect?contentUrl=some-url?ticket=auth?infoOnly=value
 *    <b>contentUrl</b> is a ContentStore-specific URL 
 *    <b>ticket</b> is a valid authentication token for an admin user 
 *    <b>infoOnly</b> is set to true to leave off the binary stream and just send
 *                    the data about the content. 
 * </pre>
 * This serlet is intended to be accessed to retrieve the binary content from
 * the content stores.  If you wish to retrieve content from a client, use
 * the other content download servlets available.
 * <p>
 * The following responses are generated:
 * <ul>
 *   <li><b>Contet not found:</b> 204 NO CONTENT</li>
 *   <li><b>Access denied:</b>    403 FORBIDDEN</li>
 * </ul>
 * The following header values are set:
 * <ul>
 *   <li><b>alfresco.dr.size:</b> The content size</li>
 *   <li><b>alfresco.dr.lastModified:</b> The last modified date</li>
 *   <li><b>alfresco.dr.mimetype:</b> The content mimetype</li>
 *   <li><b>alfresco.dr.encoding:</b> The content encoding</li>
 *   <li><b>alfresco.dr.locale:</b> The content locale</li>
 * </ul>
 * Note that the mimetype, encoding and locale generally fallback to the default
 * as implemented by the {@linkplain ContentService#getRawReader(String) raw reader}.
 * 
 * @since 2.1
 * @author Derek Hulley
 */
public class DownloadRawContentServlet extends BaseServlet
{
   private static final long serialVersionUID = 2973080032044411358L;

   private static Log logger = LogFactory.getLog(DownloadRawContentServlet.class);
    
   private static final String DEFAULT_MIMETYPE = MimetypeMap.MIMETYPE_BINARY;
   private static final String DEFAULT_ENCODING = "utf-8";

   private static final String ARG_CONTENT_URL = "contentUrl";
   private static final String ARG_INFO_ONLY = "infoOnly";
   
   protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
   {
      if (logger.isDebugEnabled())
      {
         logger.debug("Authenticating downloadDirect request: " + req.getRequestURI());
      }
      
      AuthenticationStatus status = servletAuthenticate(req, res);
      if (status == AuthenticationStatus.Failure)
      {
         return;
      }
      
      setNoCacheHeaders(res);
      
      processRequest(req, res);
   }

   /**
    * Processes the request using the current context i.e. no
    * authentication checks are made, it is presumed they have already
    * been done.
    * 
    * @param req The HTTP request
    * @param res The HTTP response
    */
   private void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
   {
      String uri = req.getRequestURI();
      
      String contentUrl = req.getParameter(ARG_CONTENT_URL);
      if (contentUrl == null || contentUrl.length() == 0)
      {
         throw new IllegalArgumentException("Download URL did not contain parameter '" + ARG_CONTENT_URL + "':" + uri); 
      }
      String infoOnlyStr = req.getParameter(ARG_INFO_ONLY);
      boolean infoOnly = (infoOnlyStr == null) ? false : Boolean.parseBoolean(infoOnlyStr);

      ServiceRegistry serviceRegistry = getServiceRegistry(getServletContext());
      ContentService contentService = serviceRegistry.getContentService();
      
      // Attempt to get the reader
      ContentReader reader = null;
      try
      {
         reader = contentService.getRawReader(contentUrl);
         // If the content doesn't exist, generate an error
         if (!reader.exists())
         {
            if (logger.isDebugEnabled())
            {
               logger.debug("Returning 204 Not Found error...");
            }
            res.sendError(HttpServletResponse.SC_NO_CONTENT);
            return;
         }
      }
      catch (AccessDeniedException e)
      {
         if (logger.isDebugEnabled())
         {
            logger.debug("Returning 403 Forbidden error after exception: ", e);
         }
         res.sendError(HttpServletResponse.SC_FORBIDDEN);
         return;
      }
      
      long readerSize = reader.getSize();
      Date readerLastModified = new Date(reader.getLastModified());
      String readerMimetype = reader.getMimetype();
      String readerEncoding = reader.getEncoding();
      Locale readerLocale = reader.getLocale();
      // Set the content info
      res.setHeader("alfresco.dr.size", DefaultTypeConverter.INSTANCE.convert(String.class, readerSize));
      res.setHeader("alfresco.dr.lastModified", DefaultTypeConverter.INSTANCE.convert(String.class, readerLastModified));
      res.setHeader("alfresco.dr.mimetype", readerMimetype);
      res.setHeader("alfresco.dr.encoding", readerEncoding);
      res.setHeader("alfresco.dr.locale", DefaultTypeConverter.INSTANCE.convert(String.class, readerLocale));
      
      // Pass the stream to the response, unless only the content info was requested
      if (infoOnly)
      {
          // Fill response details
          res.setContentType(DEFAULT_MIMETYPE);
          res.setCharacterEncoding(DEFAULT_ENCODING);
      }
      else
      {
          // Fill response details
          res.setContentType(readerMimetype);
          res.setCharacterEncoding(readerEncoding);
          
         try
         {
            OutputStream clientOs = res.getOutputStream();
            reader.getContent( clientOs );                     // Streams closed for us
         }
         catch (SocketException e1)
         {
            // Not a problem
            if (logger.isDebugEnabled())
            {
               logger.debug(
                     "Client aborted stream read:\n" +
                     "   Content URL: " + contentUrl);
            }
         }
         catch (ContentIOException e2)
         {
            // Not a problem
            if (logger.isDebugEnabled())
            {
               logger.debug(
                     "Client aborted stream read:\n" +
                     "   Content URL: " + contentUrl);
            }
         }
      }
   }
}
