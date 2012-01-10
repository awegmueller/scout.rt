/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.form.fields.tablefield;

import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutlineTableForm;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ISmartFieldProposalForm;
import org.eclipse.scout.rt.client.ui.form.fields.tablefield.ITableField;
import org.eclipse.scout.rt.ui.rap.LogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.basic.table.IRwtScoutTable;
import org.eclipse.scout.rt.ui.rap.basic.table.RwtScoutTable;
import org.eclipse.scout.rt.ui.rap.core.LogicalGridData;
import org.eclipse.scout.rt.ui.rap.core.util.RwtLayoutUtility;
import org.eclipse.scout.rt.ui.rap.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.rap.extension.UiDecorationExtensionPoint;
import org.eclipse.scout.rt.ui.rap.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.rap.form.fields.RwtScoutFieldComposite;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class RwtScoutTableField extends RwtScoutFieldComposite<ITableField<? extends ITable>> implements IRwtScoutTableField {

  private IRwtScoutTable m_tableComposite;
  private IRwtTableStatus m_tableStatus;
  private Composite m_tableContainer;

  @Override
  protected void initializeUi(Composite parent) {
    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    int labelStyle = UiDecorationExtensionPoint.getLookAndFeel().getFormFieldLabelAlignment();
    StatusLabelEx label = new StatusLabelEx(container, labelStyle);
    getUiEnvironment().getFormToolkit().getFormToolkit().adapt(label, false, false);
    //
    setUiContainer(container);
    setUiLabel(label);

    // layout
    LogicalGridLayout containerLayout = new LogicalGridLayout(1, 0);
    container.setLayout(containerLayout);
  }

  /**
   * complete override
   */
  @Override
  protected void setFieldEnabled(Control field, boolean b) {
    if (m_tableComposite != null) {
      m_tableComposite.setEnabledFromScout(b);
    }
  }

  @Override
  public Control getUiField() {
    return super.getUiField();
  }

  @Override
  protected void attachScout() {
    setTableFromScout(getScoutObject().getTable());
    super.attachScout();
  }

  protected void setTableFromScout(ITable table) {
    if (m_tableComposite != null && !m_tableComposite.isUiDisposed()) {
      m_tableComposite.dispose();
    }
    if (m_tableStatus != null) {
      m_tableStatus.dispose();
    }
    m_tableComposite = null;
    m_tableStatus = null;
    if (table != null) {
      //table
      LogicalGridData tableGridData = LogicalGridDataBuilder.createField(getScoutObject().getGridData());
      if (getScoutObject().getForm() instanceof ISmartFieldProposalForm) {
        m_tableComposite = new RwtScoutTable(RwtUtility.VARIANT_PROPOSAL_FORM);
      }
      else {
        m_tableComposite = new RwtScoutTable();
      }

      IForm form = getScoutObject() == null ? null : getScoutObject().getForm();
      m_tableContainer = null;
      if (form == null
          || form instanceof ISmartFieldProposalForm
          || form instanceof IOutlineTableForm) {
        m_tableComposite.createUiField(getUiContainer(), getScoutObject().getTable(), getUiEnvironment());
        m_tableComposite.getUiField().setLayoutData(tableGridData);
      }
      //XXX somehow the tableContainer does absorb the mouse-clicks when used in the outline
      else {
        Composite tableContainer = new Composite(getUiContainer(), SWT.NONE);
        tableContainer.setData(WidgetUtil.CUSTOM_VARIANT, VARIANT_TABLE_CONTAINER);
        tableContainer.setLayout(new LogicalGridLayout(1, 0));
        m_tableComposite.createUiField(tableContainer, getScoutObject().getTable(), getUiEnvironment());
        tableContainer.setLayoutData(tableGridData);
        m_tableContainer = tableContainer;
      }
      //table status
      if (getScoutObject().isTableStatusVisible()) {
        m_tableStatus = createRwtTableStatus();
      }
      setUiField(m_tableComposite.getUiField());
      setTableStatusFromScout();
    }
    if (!getUiContainer().isDisposed()) {
      getUiContainer().layout(true, true);
    }
  }

  protected void setTableStatusFromScout() {
    if (m_tableStatus != null) {
      IProcessingStatus dataStatus = getScoutObject().getTablePopulateStatus();
      IProcessingStatus selectionStatus = getScoutObject().getTableSelectionStatus();
      m_tableStatus.setStatus(dataStatus, selectionStatus);
    }
  }

  protected IRwtTableStatus createRwtTableStatus() {
    return new RwtTableStatus(getUiContainer(), getUiEnvironment(), getScoutObject());
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    super.setEnabledFromScout(b);
    // Workaround, because ":disabled" state seems to be ignored by RAP
    if (m_tableContainer != null) {
      m_tableContainer.setData(WidgetUtil.CUSTOM_VARIANT, (b ? VARIANT_TABLE_CONTAINER : VARIANT_TABLE_CONTAINER_DISABLED));
    }
  }

  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    super.handleScoutPropertyChange(name, newValue);
    if (name.equals(ITableField.PROP_TABLE)) {
      setTableFromScout((ITable) newValue);
      if (isCreated()) {
        RwtLayoutUtility.invalidateLayout(getUiEnvironment(), getUiContainer());
      }
    }
    else if (name.equals(ITableField.PROP_TABLE_SELECTION_STATUS)) {
      setTableStatusFromScout();
    }
    else if (name.equals(ITableField.PROP_TABLE_POPULATE_STATUS)) {
      setTableStatusFromScout();
    }
  }
}