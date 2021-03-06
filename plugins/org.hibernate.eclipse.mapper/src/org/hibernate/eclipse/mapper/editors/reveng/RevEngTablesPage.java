/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.eclipse.mapper.editors.reveng;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.hibernate.eclipse.mapper.MapperMessages;
import org.hibernate.eclipse.mapper.editors.ReverseEngineeringEditor;

public class RevEngTablesPage extends RevEngFormEditorPart {

	public final static String PART_ID = "tables"; //$NON-NLS-1$

	public RevEngTablesPage(ReverseEngineeringEditor reditor) {
		super(reditor, PART_ID, MapperMessages.RevEngTablesPage_tables_and_columns);
	}

	protected void createFormContent(IManagedForm managedForm) {

		ScrolledForm form = managedForm.getForm();

		GridLayout layout = new GridLayout();

		form.getBody().setLayout(layout);

		createTablesSection();

		getManagedForm().setInput(getReverseEngineeringEditor().getReverseEngineeringDefinition());

	}


	private void createTablesSection() {
		TablePropertiesBlock block = new TablePropertiesBlock(getReverseEngineeringEditor());
		//getManagedForm().getForm().setText("Tables & Columns");
		block.createContent(getManagedForm());
	}

}
