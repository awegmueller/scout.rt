/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.swing.window.desktop.toolbar.internal;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.eclipse.scout.rt.ui.swing.ISwingEnvironment;
import org.eclipse.scout.rt.ui.swing.SwingIcons;
import org.eclipse.scout.rt.ui.swing.basic.IconGroup;
import org.eclipse.scout.rt.ui.swing.basic.IconGroup.IconState;
import org.eclipse.scout.rt.ui.swing.window.desktop.toolbar.AbstractJNavigationWidget;

public class JNavigationWidget extends AbstractJNavigationWidget {

  private static final long serialVersionUID = 1L;

  private JToolBar m_swingToolBar;

  private JButton m_backButton;
  private JButton m_forwardButton;
  private JButton m_stopRefreshButton;
  private JButton m_historyButton;

  public JNavigationWidget(ISwingEnvironment env) {
    super(env);

    env.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (ISwingEnvironment.PROP_BUSY.equals(evt.getPropertyName())) {
          Boolean busy = (Boolean) evt.getNewValue();
          if (busy) {
            m_stopRefreshButton.setAction(getStopAction());
            installButtonIcons(m_stopRefreshButton, SwingIcons.NavigationStop, UIManager.getString("Navigation.cancel"));
          }
          else {
            m_stopRefreshButton.setAction(getRefreshAction());
            installButtonIcons(m_stopRefreshButton, SwingIcons.NavigationRefresh, UIManager.getString("Navigation.refresh"));
          }
        }
      }
    });
  }

  @Override
  public void rebuild() {
    removeAll();

    m_swingToolBar = new JToolBar(JToolBar.HORIZONTAL);
    m_swingToolBar.setFloatable(false);
    m_swingToolBar.setBorder(new EmptyBorder(0, 0, 0, 0));
    m_swingToolBar.setLayout(new GridBagLayout());

    // history button
    m_historyButton = new JButton(getHistoryAction());
    m_historyButton.setEnabled(getBackAction().isEnabled() || getForwardAction().isEnabled());
    installButtonIcons(m_historyButton, SwingIcons.NavigationHistory, UIManager.getString("Navigation.history"));
    if (m_historyButton.getIcon() == null) {
      m_historyButton.setIcon(createArrowDownIcon());
    }
    addButton(m_swingToolBar, m_historyButton, new Insets(0, 0, 0, 0));

    // back button
    m_backButton = new JButton(getBackAction());
    installButtonIcons(m_backButton, SwingIcons.NavigationBack, UIManager.getString("Navigation.back"));
    addButton(m_swingToolBar, m_backButton, new Insets(0, 0, 0, 3));

    // forward button
    m_forwardButton = new JButton(getForwardAction());
    installButtonIcons(m_forwardButton, SwingIcons.NavigationForward, UIManager.getString("Navigation.forward"));
    addButton(m_swingToolBar, m_forwardButton, new Insets(0, 0, 0, 3));

    // stop / refresh button
    m_stopRefreshButton = new JButton(getRefreshAction());
    installButtonIcons(m_stopRefreshButton, SwingIcons.NavigationRefresh, UIManager.getString("Navigation.refresh"));
    addButton(m_swingToolBar, m_stopRefreshButton, new Insets(0, 0, 0, 0));

    add(m_swingToolBar);
  }

  private void addButton(JToolBar toolBar, JButton button, Insets insets) {
    // layout tool button
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.insets = insets;
    toolBar.add(button, gbc);
  }

  @Override
  public void setBackAction(AbstractAction backAction) {
    super.setBackAction(backAction);
    if (m_backButton != null) {
      m_backButton.setAction(backAction);
    }
  }

  @Override
  public void setForwardAction(AbstractAction forwardAction) {
    super.setForwardAction(forwardAction);
    if (m_forwardButton != null) {
      m_forwardButton.setAction(forwardAction);
    }
  }

  @Override
  public void setHistoryAction(AbstractAction historyAction) {
    super.setHistoryAction(historyAction);
    if (m_historyButton != null) {
      m_historyButton.setAction(historyAction);
    }
  }

  @Override
  public Point getHistoryMenuLocation() {
    Point location = m_historyButton.getLocation();
    location.y = location.y + m_historyButton.getHeight() + 5;
    location.x = location.x + 5;
    return location;
  }

  private void installButtonIcons(JButton button, String iconId, String tooltip) {
    IconGroup iconGroup = new IconGroup(iconId);
    button.setIcon(iconGroup.getIcon(IconState.NORMAL));
    if (iconGroup.hasIcon(IconState.DISABLED)) {
      button.setDisabledIcon(iconGroup.getIcon(IconState.DISABLED));
    }
    if (iconGroup.hasIcon(IconState.SELECTED)) {
      button.setPressedIcon(iconGroup.getIcon(IconState.SELECTED));
    }
    if (iconGroup.hasIcon(IconState.ROLLOVER)) {
      button.setRolloverIcon(iconGroup.getIcon(IconState.ROLLOVER));
    }
    button.setToolTipText(tooltip);
  }

  private Icon createArrowDownIcon() {
    int arrowWidth = 8;
    int arrowHeight = arrowWidth / 2;
    BufferedImage img = new BufferedImage(arrowWidth, arrowHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics g = img.createGraphics();
    g.setColor(UIManager.getColor("controlDkShadow"));
    g.fillPolygon(new int[]{0, arrowWidth / 2, arrowWidth},
                  new int[]{0, arrowHeight, 0},
                  3);

    return new ImageIcon(img);
  }
}
