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
package org.alfresco.web.bean.actions.handlers;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.repo.action.executer.AddFeaturesActionExecuter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.actions.BaseActionWizard;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.wizard.IWizardBean;

/**
 * Action handler implementation for the "add-features" action.
 * 
 * @author gavinc
 */
public class AddFeaturesHandler extends BaseActionHandler
{
   private static final long serialVersionUID = -2062128886273060606L;
   
   protected static final String PROP_ASPECT = "aspect";
   
   public String getJSPPath()
   {
      return getJSPPath(AddFeaturesActionExecuter.NAME);
   }

   public void prepareForSave(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoParams)
   {
      QName aspect = Repository.resolveToQName((String)actionProps.get(PROP_ASPECT));
      repoParams.put(AddFeaturesActionExecuter.PARAM_ASPECT_NAME, aspect);
   }

   public void prepareForEdit(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoParams)
   {
      QName aspect = (QName)repoParams.get(AddFeaturesActionExecuter.PARAM_ASPECT_NAME);
      actionProps.put(PROP_ASPECT, aspect.toString());
   }

   public String generateSummary(FacesContext context, IWizardBean wizard,
         Map<String, Serializable> actionProps)
   {
      String label = null;
      String aspect = (String)actionProps.get(PROP_ASPECT);
         
      // find the label used by looking through the SelectItem list
      for (SelectItem item : ((BaseActionWizard)wizard).getAddableAspects())
      {
         if (item.getValue().equals(aspect))
         {
            label = item.getLabel();
            break;
         }
      }

      return MessageFormat.format(Application.getMessage(context, "action_add_features"), 
            new Object[] {label});
   }
}
