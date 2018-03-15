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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * Servlet responsible for streaming content directly into the repository from the PUT request.
 * The appropriate mimetype is calculated based on filename extension.
 * <p>
 * The URL to the servlet should be generated thus:
 * <pre>/alfresco/upload/workspace/SpacesStore/0000-0000-0000-0000/myfile.pdf</pre>
 * or
 * <pre>/alfresco/upload/myfile.pdf</pre>
 * <p>
 * If the store and node id are specified in the URL then the content provided will be streamed onto the node
 * using an updating writer, updating the content property value accordingly.
 * <p>
 * If only the file name is specified the content will be streamed into the content store and the content data
 * will be returned in the reposonse.  This can then be used to update the value of a content property manually.
 * Any used content will be cleared up in the usual manner.
 * <p>
 * By default, the download assumes that the content is on the
 * {@link org.alfresco.model.ContentModel#PROP_CONTENT content property}.<br>
 * To set the content of a specific model property, use a 'property' arg, providing the qualified name of the property.
 * <p>
 * Like most Alfresco servlets, the URL may be followed by a valid 'ticket' argument for authentication:
 * ?ticket=1234567890
 * <p>
 * Guest access is currently disabled for this servlet.
 * 
 * @author Roy Wetherall
 */
public class UploadContentServlet extends BaseServlet
{
    /** Serial version UID */
    private static final long serialVersionUID = 1055960980867420355L;

    /** Logger */
    private static Log logger = LogFactory.getLog(UploadContentServlet.class);

    /** Default mime type */
    protected static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";

    /** Argument properties */
    protected static final String ARG_PROPERTY = "property";
    protected static final String ARG_MIMETYPE = "mimetype";
    protected static final String ARG_ENCODING = "encoding";
    protected static final String ARG_LOCALE = "locale";

    /**
     * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        if (logger.isDebugEnabled() == true)
        {
            String queryString = req.getQueryString();
            logger.debug("Authenticating request to URL: " + req.getRequestURI()
                    + ((queryString != null && queryString.length() > 0) ? ("?" + queryString) : ""));
        }

        AuthenticationStatus status = servletAuthenticate(req, res, false);
        if (status == AuthenticationStatus.Failure || status == AuthenticationStatus.Guest)
        {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Tokenise the URI
        String uri = req.getRequestURI();
        uri = uri.substring(req.getContextPath().length());
        StringTokenizer t = new StringTokenizer(uri, "/");
        int tokenCount = t.countTokens();

        t.nextToken(); // skip servlet name

        // get or calculate the noderef and filename to download as
        NodeRef nodeRef = null;
        String filename = null;
        QName propertyQName = null;

        if (tokenCount == 2)
        {
            // filename is the only token
            filename = t.nextToken();
        }
        else if (tokenCount == 4 || tokenCount == 5)
        {
            // assume 'workspace' or other NodeRef based protocol for remaining URL
            // elements
            StoreRef storeRef = new StoreRef(t.nextToken(), t.nextToken());
            String id = t.nextToken();
            // build noderef from the appropriate URL elements
            nodeRef = new NodeRef(storeRef, id);
    
            if (tokenCount == 5)
            {
                // filename is last remaining token
                filename = t.nextToken();
            }
            
            // get qualified of the property to get content from - default to
            // ContentModel.PROP_CONTENT
            propertyQName = ContentModel.PROP_CONTENT;
            String property = req.getParameter(ARG_PROPERTY);
            if (property != null && property.length() != 0)
            {
                propertyQName = QName.createQName(property);
            }
        }
        else
        {
            logger.debug("Upload URL did not contain all required args: " + uri);
            res.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
            return;
        }

        // get the services we need to retrieve the content
        ServiceRegistry serviceRegistry = getServiceRegistry(getServletContext());
        ContentService contentService = serviceRegistry.getContentService();
        PermissionService permissionService = serviceRegistry.getPermissionService();
        MimetypeService mimetypeService = serviceRegistry.getMimetypeService();
        NodeService nodeService = serviceRegistry.getNodeService();
        
        InputStream is = req.getInputStream();
        BufferedInputStream inputStream = new BufferedInputStream(is);

        // Sort out the mimetype
        String mimetype = req.getParameter(ARG_MIMETYPE);
        if (mimetype == null || mimetype.length() == 0)
        {
            mimetype = MIMETYPE_OCTET_STREAM;
            if (filename != null)
            {
                MimetypeService mimetypeMap = serviceRegistry.getMimetypeService();
                int extIndex = filename.lastIndexOf('.');
                if (extIndex != -1)
                {
                    String ext = filename.substring(extIndex + 1);
                    mimetype = mimetypeService.getMimetype(ext);
                }
            }
        }

        // Get the encoding
        String encoding = req.getParameter(ARG_ENCODING);
        if (encoding == null || encoding.length() == 0)
        {
           // Get the encoding
           ContentCharsetFinder charsetFinder = mimetypeService.getContentCharsetFinder();
           Charset charset = charsetFinder.getCharset(inputStream, mimetype);
           encoding = charset.name();
        }

        // Get the locale
        Locale locale = I18NUtil.parseLocale(req.getParameter(ARG_LOCALE));
        if (locale == null)
        {
            locale = I18NUtil.getContentLocale();
            if (nodeRef != null)
            {
                ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, propertyQName);
                if (contentData != null)
                {
                    locale = contentData.getLocale();
                }
            }
        }

        if (logger.isDebugEnabled())
        {
            if (nodeRef != null) {logger.debug("Found NodeRef: " + nodeRef.toString());}
            logger.debug("For property: " + propertyQName);
            logger.debug("File name: " + filename);
            logger.debug("Mimetype: " + mimetype);
            logger.debug("Encoding: " + encoding);
            logger.debug("Locale: " + locale);
        }

        // Check that the user has the permissions to write the content
        if (permissionService.hasPermission(nodeRef, PermissionService.WRITE_CONTENT) == AccessStatus.DENIED)
        {
            if (logger.isDebugEnabled() == true)
            {
                logger.debug("User does not have permissions to wrtie content for NodeRef: " + nodeRef.toString());
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Returning 403 Forbidden error...");
            }

            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        // Try and get the content writer
        ContentWriter writer = contentService.getWriter(nodeRef, propertyQName, true);
        if (writer == null)
        {
            if (logger.isDebugEnabled() == true)
            {
                logger.debug("Content writer cannot be obtained for NodeRef: " + nodeRef.toString());
            }
            res.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
            return;
        }
        
        // Set the mimetype, encoding and locale
        writer.setMimetype(mimetype);
        writer.setEncoding(encoding);
        if (locale != null)
        {
            writer.setLocale(locale);
        }
        
        // Stream the content into the repository
        writer.putContent(inputStream);
        
        if (logger.isDebugEnabled() == true)
        {
            logger.debug("Content details: " + writer.getContentData().toString());
        }

        // Set return status
        res.getWriter().write(writer.getContentData().toString());       
        res.flushBuffer();
        
        if (logger.isDebugEnabled() == true)
        {
            logger.debug("UploadContentServlet done");
        }
    }
}
