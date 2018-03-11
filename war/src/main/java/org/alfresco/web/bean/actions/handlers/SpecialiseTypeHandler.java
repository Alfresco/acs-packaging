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

import org.alfresco.repo.action.executer.SpecialiseTypeActionExecuter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.actions.BaseActionWizard;
import org.alfresco.web.bean.wizard.IWizardBean;

/**
 * Action handler for the "specialise-type" action.
 * 
 * @author gavinc
 */
public class SpecialiseTypeHandler extends BaseActionHandler
{
   private static final long serialVersionUID = 7404684895683515301L;
   
   public static final String PROP_OBJECT_TYPE = "objecttype";
   
   public String getJSPPath()
   {
      return getJSPPath(SpecialiseTypeActionExecuter.NAME);
   }

   public void prepareForSave(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoProps)
   {
      String objectType = (String)actionProps.get(PROP_OBJECT_TYPE);
      repoProps.put(SpecialiseTypeActionExecuter.PARAM_TYPE_NAME, 
            QName.createQName(objectType));
   }

   public void prepareForEdit(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoProps)
   {
      QName specialiseType = (QName)repoProps.get(SpecialiseTypeActionExecuter.PARAM_TYPE_NAME);
      actionProps.put(PROP_OBJECT_TYPE, specialiseType.toString());
   }

   public String generateSummary(FacesContext context, IWizardBean wizard,
         Map<String, Serializable> actionProps)
   {
      String label = null;
      String objectType = (String)actionProps.get(PROP_OBJECT_TYPE);
      for (SelectItem item  : ((BaseActionWizard)wizard).getObjectTypes())
      {
         if (item.getValue().equals(objectType) == true)
         {
            label = item.getLabel();
            break;
         }
      }
         
      return MessageFormat.format(Application.getMessage(context, "action_specialise_type"),
            new Object[] {label});
   }

}
