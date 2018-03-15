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
package org.alfresco.web.bean.rules.handlers;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.repo.action.evaluator.HasAspectEvaluator;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.rules.CreateRuleWizard;
import org.alfresco.web.bean.wizard.IWizardBean;

/**
 * Condition handler for the "has-aspect" condition.
 * 
 * @author gavinc
 */
public class HasAspectHandler extends BaseConditionHandler
{
   private static final long serialVersionUID = 7365950247553882237L;
   
   protected static final String PROP_ASPECT = "aspect";
   
   public String getJSPPath()
   {
      return getJSPPath(HasAspectEvaluator.NAME);
   }

   public void prepareForSave(Map<String, Serializable> conditionProps,
         Map<String, Serializable> repoProps)
   {
      QName aspect = QName.createQName((String)conditionProps.get(PROP_ASPECT));
      repoProps.put(HasAspectEvaluator.PARAM_ASPECT, aspect);
   }

   public void prepareForEdit(Map<String, Serializable> conditionProps,
         Map<String, Serializable> repoProps)
   {
      QName aspect = (QName)repoProps.get(HasAspectEvaluator.PARAM_ASPECT);
      conditionProps.put(PROP_ASPECT, aspect.toString());
   }

   public String generateSummary(FacesContext context, IWizardBean wizard,
         Map<String, Serializable> conditionProps)
   {
      Boolean not = (Boolean)conditionProps.get(PROP_CONDITION_NOT);
      String msgId = not.booleanValue() ? "condition_has_aspect_not" : "condition_has_aspect";
         
      String label = null;
      String aspectName = (String)conditionProps.get(PROP_ASPECT);
      for (SelectItem item : ((CreateRuleWizard)wizard).getTestableAspects())
      {
         if (item.getValue().equals(aspectName))
         {
            label = item.getLabel();
            break;
         }
      }
      
      return MessageFormat.format(Application.getMessage(context, msgId),
            new Object[] {label});
   }
}
