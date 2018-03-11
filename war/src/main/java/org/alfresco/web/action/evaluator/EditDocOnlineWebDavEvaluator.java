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
package org.alfresco.web.action.evaluator;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.coci.EditOfflineDialog;
import org.alfresco.web.bean.coci.EditOnlineDialog;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

/**
 * UI Action Evaluator - Edit document online via WebDav.
 */
public class EditDocOnlineWebDavEvaluator extends CheckoutDocEvaluator
{
   /**
    * @see org.alfresco.web.action.ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)
    */
   public boolean evaluate(Node node)
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      DictionaryService dd = Repository.getServiceRegistry(fc).getDictionaryService();
      
      boolean result = false;
      
      // if the node is inline editable, the inline online editing should always be used
      if (dd.isSubClass(node.getType(), ContentModel.TYPE_CONTENT))
      {
         Map<String, Object> props = node.getProperties();

         if ("webdav".equals(Application.getClientConfig(fc).getEditLinkType()) &&
             (!node.hasAspect(ApplicationModel.ASPECT_INLINEEDITABLE) ||
              props.get(ApplicationModel.PROP_EDITINLINE) == null ||
              !((Boolean)props.get(ApplicationModel.PROP_EDITINLINE)).booleanValue()))
         {
            if (node.hasAspect(ContentModel.ASPECT_WORKING_COPY))
            {
               result = (EditOnlineDialog.ONLINE_EDITING.equals(props.get(ContentModel.PROP_WORKING_COPY_MODE)));
            }
            else
            {
               result = super.evaluate(node);
            }
         }
      }
      
      return result;
   }
}