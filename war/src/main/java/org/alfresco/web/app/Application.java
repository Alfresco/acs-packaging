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
package org.alfresco.web.app;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.bean.repository.User;
import org.apache.commons.logging.Log;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utilities class
 *
 * @author gavinc
 */
public class Application
{
   private static final String LOCALE = "locale";

   public static final String BEAN_IMPORTER_BOOTSTRAP = "spacesBootstrap";

   public static final String MESSAGE_BUNDLE = "alfresco.messages.webclient";

   private static ThreadLocal<Boolean> inPortalServer = new ThreadLocal<Boolean>();
   private static StoreRef repoStoreRef = null;
   private static String companyRootId;


   /**
    * Private constructor to prevent instantiation of this class
    */
   private Application()
   {
   }

   /**
    * Determines whether the server is running in a portal
    *
    * @return true if we are running inside a portal server
    */
   public static boolean inPortalServer()
   {
      Boolean result = inPortalServer.get();
      return result == null ? false : result;
   }

   /**
    * Handles error conditions detected by servlets.
    *
    * @param servletContext
    *           The servlet context
    * @param request
    *           The HTTP request
    * @param response
    *           The HTTP response
    * @param messageKey
    *           the resource bundle key for the error mesage
    * @param statusCode
    *           the status code to set on the response
    * @param logger
    *           The logger
    * @throws IOException
    *            Signals that an I/O exception has occurred.
    * @throws ServletException
    *            the servlet exception
    */
   public static void handleSystemError(ServletContext servletContext, HttpServletRequest request,
         HttpServletResponse response, String messageKey, int statusCode, Log logger)
         throws IOException, ServletException
   {
      // get the error bean from the session and set the error that occurred.
      HttpSession session = request.getSession();
      throw new ServletException(getMessage(session, messageKey));
   }

   /**
    * Retrieves the configured error page for the application
    *
    * @param servletContext The servlet context
    * @return The configured error page or null if the configuration is missing
    */
   public static String getErrorPage(ServletContext servletContext)
   {
      return getErrorPage(WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext));
   }

   /**
    * Retrieves the configured login page for the application
    *
    * @param servletContext The servlet context
    * @return The configured login page or null if the configuration is missing
    */
   public static String getLoginPage(ServletContext servletContext)
   {
      return getLoginPage(WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext));
   }

   /**
    * @return Returns the User object representing the currently logged in user
    */
   public static User getCurrentUser(HttpSession session)
   {
      return (User)session.getAttribute(AuthenticationHelper.AUTHENTICATION_USER);
   }

   /**
    * @return Returns id of the company root
    *
    * @deprecated Replace with user-context-specific getCompanyRootId (e.g. could be tenant-specific)
    */
   public static String getCompanyRootId()
   {
      return companyRootId;
   }

   /**
    * Get the specified I18N message string from the default message bundle for this user
    *
    * @param session        HttpSession
    * @param msg            Message ID
    *
    * @return String from message bundle or $$msg$$ if not found
    */
   public static String getMessage(HttpSession session, String msg)
   {
      return getBundle(session).getString(msg);
   }

   /**
    * Get the specified the default message bundle for this user
    *
    * @param session        HttpSession
    *
    * @return ResourceBundle for this user
    */
   public static ResourceBundle getBundle(HttpSession session)
   {
      ResourceBundle bundle = (ResourceBundle)session.getAttribute(MESSAGE_BUNDLE);
      if (bundle == null)
      {
         // get Locale from language selected by each user on login
         Locale locale = (Locale)session.getAttribute(LOCALE);
         if (locale == null)
         {
            bundle = ResourceBundleWrapper.getResourceBundle(MESSAGE_BUNDLE, I18NUtil.getLocale());
         }
         else
         {
            // Only cache the bundle if/when we have a session locale
            bundle = ResourceBundleWrapper.getResourceBundle(MESSAGE_BUNDLE, locale);
            session.setAttribute(MESSAGE_BUNDLE, bundle);
         }
      }

      return bundle;
   }

   /**
    * Returns the repository store URL
    *
    * @param context The spring context
    * @return The repository store URL to use
    */
   public static StoreRef getRepositoryStoreRef(WebApplicationContext context)
   {
      if (repoStoreRef == null)
      {
         ImporterBootstrap bootstrap = (ImporterBootstrap)context.getBean(BEAN_IMPORTER_BOOTSTRAP);
         repoStoreRef = bootstrap.getStoreRef();
      }

      return repoStoreRef;
   }

      public static StoreRef getStoreRef()
      {
         return repoStoreRef;
      }

   /**
    * Retrieves the configured error page for the application
    *
    * @param context The Spring context
    * @return The configured error page or null if the configuration is missing
    */
   public static String getErrorPage(ApplicationContext context)
   {
      return null;
   }

   /**
    * Retrieves the configured login page for the application
    *
    * @param context The Spring context
    * @return The configured login page or null if the configuration is missing
    */
   public static String getLoginPage(ApplicationContext context)
   {
      return null;
   }
}