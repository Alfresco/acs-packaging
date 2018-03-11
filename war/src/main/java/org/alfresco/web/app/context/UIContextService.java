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
package org.alfresco.web.app.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

/**
 * Beans supporting the IContextListener interface are registered against this class. Then Beans
 * which wish to indicate that the UI should refresh itself i.e. dump all cached data and settings,
 * call the UIContextService.notifyBeans() to inform all registered instances of the change.
 * <p>
 * Registered beans will also be informed of changes in location, for example when the current
 * space changes or when the user has changed area i.e. from company home to my home.
 * 
 * @author Kevin Roast
 */
public final class UIContextService implements Serializable
{
   private static final long serialVersionUID = -866750823190499704L;

   /**
    * Private constructor
    */
   private UIContextService()
   {
   }
   
   /**
    * Returns a Session local instance of the UIContextService
    * 
    * @return UIContextService for this Thread
    */
   @SuppressWarnings("unchecked")
   public static UIContextService getInstance(FacesContext fc)
   {
      Map session = fc.getExternalContext().getSessionMap();
      UIContextService service = (UIContextService)session.get(CONTEXT_KEY);
      if (service == null)
      {
         service = new UIContextService();
         session.put(CONTEXT_KEY, service);
      }
      
      return service;
   }
   
   /**
    * Register a bean to be informed of context events
    * 
    * @param bean    Conforming to the IContextListener interface
    */
   public void registerBean(IContextListener bean)
   {
      if (bean == null)
      {
         throw new IllegalArgumentException("Bean reference specified cannot be null!");
      }
      
      this.registeredBeans.put(bean.getClass(), bean);
   }
   
   /**
    * Remove a bean reference from those notified of changes
    * 
    * @param bean    Conforming to the IContextListener interface
    */
   public void unregisterBean(IContextListener bean)
   {
      if (bean == null)
      {
         throw new IllegalArgumentException("Bean reference specified cannot be null!");
      }
      
      this.registeredBeans.remove(bean);
   }
   
   /**
    * Returns a registered bean or null
    * 
    * @param className (fully qualified name)
    * 
    * @return IContextListener
    */
   public IContextListener getRegisteredBean(String className)
   {
      IContextListener bean = null;
      for (Class clazz : this.registeredBeans.keySet())
      {
         if (clazz.getName().equals(className))
         {
            bean = this.registeredBeans.get(clazz);
            break;
         }
      }
      return bean;
   }
   
   /**
    * Call to notify all register beans that the UI context has changed and they should
    * refresh themselves as appropriate.
    */
   public void notifyBeans()
   {
      for (IContextListener listener: this.registeredBeans.values())
      {
         listener.contextUpdated();
      }
   }
   
   /**
    * Call to notify all register beans that the current space has changed and they should
    * refresh themselves as appropriate.
    */
   public void spaceChanged()
   {
      for (IContextListener listener: this.registeredBeans.values())
      {
         listener.spaceChanged();
      }
   }
   
   /**
    * Call to notify all register beans that the area i.e. my home, has changed and they should
    * refresh themselves as appropriate.
    */
   public void areaChanged()
   {
      for (IContextListener listener: this.registeredBeans.values())
      {
         listener.areaChanged();
      }
   }
   
   /** key for the UI context service in the session */
   private final static String CONTEXT_KEY = "__uiContextService";
   
   /** Map of bean registered against the context service */
   private Map<Class, IContextListener> registeredBeans = new HashMap<Class, IContextListener>(7, 1.0f);
}
