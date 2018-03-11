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

import org.alfresco.repo.action.executer.ScriptActionExecuter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.wizard.IWizardBean;

/**
 * Action handler for the "script" action.
 * 
 * @author gavinc
 */
public class ScriptHandler extends BaseActionHandler
{
   private static final long serialVersionUID = -8006002591602401584L;
   
   protected static final String PROP_SCRIPT = "script";
   
   public String getJSPPath()
   {
      return getJSPPath(ScriptActionExecuter.NAME);
   }

   public void prepareForSave(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoProps)
   {
      // add the selected script noderef to the action properties
      String id = (String)actionProps.get(PROP_SCRIPT);
      NodeRef scriptRef = new NodeRef(Repository.getStoreRef(), id);
      repoProps.put(ScriptActionExecuter.PARAM_SCRIPTREF, scriptRef);
   }

   public void prepareForEdit(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoProps)
   {
      NodeRef scriptRef = (NodeRef)repoProps.get(ScriptActionExecuter.PARAM_SCRIPTREF);
      actionProps.put(PROP_SCRIPT, scriptRef.getId());
   }

   public String generateSummary(FacesContext context, IWizardBean wizard,
         Map<String, Serializable> actionProps)
   {
      String id = (String)actionProps.get(PROP_SCRIPT);
      NodeRef scriptRef = new NodeRef(Repository.getStoreRef(), id);
      String scriptName = Repository.getNameForNode(
            Repository.getServiceRegistry(context).getNodeService(), scriptRef);
      
      return MessageFormat.format(Application.getMessage(context, "action_script"),
            new Object[] {scriptName});
   }
}
