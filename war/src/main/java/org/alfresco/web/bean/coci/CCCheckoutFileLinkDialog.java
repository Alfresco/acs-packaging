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
package org.alfresco.web.bean.coci;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CCCheckoutFileLinkDialog extends CheckinCheckoutDialog
{
    private static final long serialVersionUID = -4732775752517417543L;
    
    public static final String MSG_CHECKOUT_OF = "check_out_of";
    public static final String LBL_UNDO_CHECKOUT = "undo_checkout";
    private final static String MSG_LEFT_QUOTE = "left_qoute";
    private final static String MSG_RIGHT_QUOTE = "right_quote";

    private static Log logger = LogFactory.getLog(CCCheckoutFileLinkDialog.class);

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception
    {
        return checkoutFileOK(context, outcome);
    }

    @Override
    public boolean getFinishButtonDisabled()
    {
        return false;
    }

    @Override
    public String getContainerTitle()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return Application.getMessage(fc, MSG_CHECKOUT_OF) + " " + Application.getMessage(fc, MSG_LEFT_QUOTE)
            + property.getDocument().getName() + Application.getMessage(fc, MSG_RIGHT_QUOTE);
    }

    @Override
    public String getCancelButtonLabel()
    {
        return Application.getMessage(FacesContext.getCurrentInstance(), LBL_UNDO_CHECKOUT);
    }
    
    public String getFinishButtonLabel()
    {
       return Application.getMessage(FacesContext.getCurrentInstance(), "ok");
    }    

    @Override
    public String cancel()
    {
        undoCheckout();
        resetState();
        super.cancel();
        return "browse";

    }

    /**
     * Action called upon completion of the Check Out file Link download page
     */
    public String checkoutFileOK(FacesContext context, String outcome)
    {
        Node node = property.getWorkingDocument();
        if (node != null)
        {
            // reset the underlying node
            if (this.browseBean.getDocument() != null)
            {
                this.browseBean.getDocument().reset();
            }

            // clean up and clear action context
            resetState();
            property.setDocument(null);
            property.setWorkingDocument(null);
            // currentAction = Action.NONE;
            outcome = AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME + ":browse";
        }
        else
        {
            logger.warn("WARNING: checkoutFileOK called without a current WorkingDocument!");
        }
        return outcome;
    }
}
