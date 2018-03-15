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

import org.alfresco.web.bean.groups.GroupsDialog;


/**
 * Evaluator that determines whether the add group user action should
 * be visible - only visible when the dialog is not showing the root
 * group.
 * 
 * @author Gavin Cornwell
 */
public class GroupActionEvaluator extends BaseActionEvaluator
{
   @Override
   public boolean evaluate(Object obj)
   {
      boolean result = true;
      
      if (obj instanceof GroupsDialog)
      {
         // if the object is the GroupsDialog check whether the group is null,
         // if it is it represents the root group so disallow the action
         result = (((GroupsDialog)obj).getGroup() != null);
      }
      else if (obj instanceof Map)
      {
         // if the object is a Map retrieve the group and check for null,
         // if it is it represents the root group so disallow the action
         Object group = ((Map)obj).get(GroupsDialog.PARAM_GROUP);
         result = (group != null);
      }
      else
      {
         result = super.evaluate(obj);
      }
      
      return result;
   }
}
