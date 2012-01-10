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
package org.eclipse.scout.rt.testing.ui.rap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.eclipse.scout.rt.testing.shared.WaitCondition;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.basic.RwtScoutComposite;
import org.eclipse.scout.rt.ui.rap.ext.DropDownButton;
import org.eclipse.scout.rt.ui.rap.ext.custom.StyledText;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.scout.testing.client.IGuiMock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 *
 */
public class RapMock implements IGuiMock {
  static interface MockRunnable<T> extends WaitCondition<T> {
  }

  private final IClientSession m_session;
  private int m_sleepDelay = 40;

  public RapMock(IClientSession session) {
    m_session = session;
  }

  @Override
  public GuiStrategy getStrategy() {
    return GuiStrategy.Rap;
  }

  @Override
  public void waitForIdle() {
    if (getDisplay().getThread() == Thread.currentThread()) {
      return;
    }
    //
    for (int pass = 0; pass < 1; pass++) {
      //wait until gui queue is empty
      syncExec(new MockRunnable<Object>() {
        @Override
        public Object run() throws Throwable {
          return null;
        }
      });
      //wait until model queue is empty
      ClientSyncJob idleJob = new ClientSyncJob("Check for idle", m_session) {
        @Override
        protected void runVoid(IProgressMonitor m) throws Throwable {
        }
      };
      idleJob.setSystem(true);
      idleJob.schedule();
      try {
        idleJob.join();
      }
      catch (InterruptedException e) {
        throw new IllegalStateException("Interrupted");
      }
    }
  }

  @Override
  public void waitForActiveWindow(final String title) {
    waitUntil(new WaitCondition<Object>() {
      @Override
      public Object run() {
        if (isWindowActive(title)) return true;
        else return null;
      }
    });
    waitForIdle();
  }

  @Override
  public void waitForOpenWindow(final String title) {
    waitUntil(new WaitCondition<Object>() {
      @Override
      public Object run() {
        if (isWindowOpen(title)) return true;
        else return null;
      }
    });
    waitForIdle();
  }

  @Override
  public int getSleepDelay() {
    return m_sleepDelay;
  }

  @Override
  public void setSleepDelay(int sleepDelay) {
    m_sleepDelay = sleepDelay;
  }

  @Override
  public void sleep() {
    sleep(getSleepDelay());
  }

  @Override
  public void sleep(int millis) {
    //only sleep when NOT in gui thread
    if (getDisplay().getThread() == Thread.currentThread()) {
      return;
    }
    //
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException e) {
      //nop
    }
    waitForIdle();
  }

  @Override
  public boolean isWindowActive(final String title) {
    return syncExec(new MockRunnable<Boolean>() {
      @Override
      public Boolean run() throws Throwable {
        CTabItem view = findWorkbenchView(title);
        if (view != null && view.getParent().getSelection() == view) return true;
        Shell shell = findShell(title);
        if (shell != null && shell == getActiveShell()) return true;
        return false;
      }
    });
  }

  @Override
  public boolean isWindowOpen(final String title) {
    return syncExec(new MockRunnable<Boolean>() {
      @Override
      public Boolean run() throws Throwable {
        CTabItem view = findWorkbenchView(title);
        if (view != null) return true;
        Shell shell = findShell(title);
        if (shell != null) return true;
        return false;
      }
    });
  }

  @Override
  public void activateWindow(final String title) {
    waitForOpenWindow(title);
    syncExec(new MockRunnable<Object>() {
      @Override
      public Object run() throws Throwable {
        CTabItem view = findWorkbenchView(title);
        if (view != null) {
          view.getParent().setSelection(view);
          return null;
        }
        Shell shell = findShell(title);
        if (shell != null) {
          shell.setActive();
          return null;
        }
        throw new IllegalStateException("There is no view with title " + title);
      }
    });
    waitForIdle();
  }

  @Override
  public FieldState getFieldState(FieldType type, int index) {
    final Control c = waitForIndexedField(type, index);
    return syncExec(new MockRunnable<FieldState>() {
      @Override
      public FieldState run() throws Throwable {
        return getFieldStateInternal(c);
      }
    });
  }

  @Override
  public FieldState getScoutFieldState(String name) {
    final Control c = waitForScoutField(name);
    return syncExec(new MockRunnable<FieldState>() {
      @Override
      public FieldState run() throws Throwable {
        return getFieldStateInternal(c);
      }
    });
  }

  @Override
  public List<FieldState> getFieldStates(final FieldType type) {
    return syncExec(new MockRunnable<List<FieldState>>() {
      @Override
      public List<FieldState> run() throws Throwable {
        List<FieldState> list = new ArrayList<FieldState>();
        for (Control c : RwtUtility.findChildComponents(getActiveShell(), Control.class)) {
          if (type == null && getFieldTypeOf(c) != null) {
            list.add(getFieldStateInternal(c));
          }
          else if (type != null && getFieldTypeOf(c) == type) {
            list.add(getFieldStateInternal(c));
          }
        }
        return list;
      }
    });
  }

  @Override
  public FieldState getFocusFieldState() {
    return syncExec(new MockRunnable<FieldState>() {
      @Override
      public FieldState run() throws Throwable {
        Control c = getDisplay().getFocusControl();
        if (c == null) {
          throw new IllegalStateException("There is no focus owner");
        }
        return getFieldStateInternal(c);
      }
    });
  }

  @Override
  public void clickOnPushButton(String text) {
    final Control c = waitForPushButtonWithLabel(text);
    syncExec(new MockRunnable<Object>() {
      @Override
      public Object run() throws Throwable {
        Point p = c.toDisplay(5, 5);
        gotoPoint(p.x, p.y);
        clickLeft();
        return null;
      }
    });
    waitForIdle();
  }

  @Override
  public void gotoField(FieldType type, int index) {
    final Control c = waitForIndexedField(type, index);
    syncExec(new MockRunnable<Object>() {
      @Override
      public Object run() throws Throwable {
        Point p = c.toDisplay(c.getSize().x / 2, c.getSize().y / 2);
        gotoPoint(p.x, p.y);
        return null;
      }
    });
  }

  @Override
  public void gotoScoutField(String name) {
    final Control c = waitForScoutField(name);
    syncExec(new MockRunnable<Object>() {
      @Override
      public Object run() throws Throwable {
        Point p = c.toDisplay(c.getSize().x / 2, c.getSize().y / 2);
        gotoPoint(p.x, p.y);
        return null;
      }
    });
  }

  @Override
  public void gotoTable(int tableIndex, final int rowIndex, final int columnIndex) {
    final Table table = (Table) waitForIndexedField(FieldType.Table, tableIndex);
    syncExec(new MockRunnable<Object>() {
      @Override
      public Object run() throws Throwable {
        TableItem item = table.getItem(rowIndex);
        //first column is dummy column
        Rectangle cellBounds = item.getBounds(columnIndex + 1);
        Point p = table.toDisplay(cellBounds.x + (cellBounds.width / 2), cellBounds.y + (cellBounds.height / 2));
        gotoPoint(p.x, p.y);
        return null;
      }
    });
  }

  @Override
  public void gotoTableHeader(int tableIndex, final int columnIndex) {
    final Table table = (Table) waitForIndexedField(FieldType.Table, tableIndex);
    syncExec(new MockRunnable<Object>() {
      @SuppressWarnings("null")
      @Override
      public Object run() throws Throwable {
        int curIndex = -1;
        int accumulatedWidth = 0;
        Rectangle cellBounds = null;
        for (int i : table.getColumnOrder()) {
          TableColumn col = table.getColumn(i);
          //first column is dummy column
          if (i > 0) {
            curIndex++;
            if (curIndex == columnIndex) {
              cellBounds = new Rectangle(accumulatedWidth, 0, col.getWidth(), table.getHeaderHeight());
              break;
            }
          }
          accumulatedWidth += col.getWidth();
        }
        cellBounds.x -= table.getHorizontalBar().getSelection();
        Point p = table.toDisplay(cellBounds.x + (cellBounds.width / 2), cellBounds.y + (cellBounds.height / 2));
        gotoPoint(p.x, p.y);
        return null;
      }
    });
  }

  @Override
  public void gotoTree(int treeIndex, final String nodeText) {
    final Tree tree = (Tree) waitForIndexedField(FieldType.Tree, treeIndex);
    syncExec(new MockRunnable<Object>() {
      @Override
      public Object run() throws Throwable {
        TreeItem item = findTreeItemRec(tree.getItems(), nodeText);
        if (item == null) {
          throw new IllegalStateException("Cannot find tree item '" + nodeText + "'");
        }
        Rectangle cellBounds = item.getBounds(0);
        Point p = tree.toDisplay(cellBounds.x + (cellBounds.width / 2), cellBounds.y + (cellBounds.height / 2));
        gotoPoint(p.x, p.y);
        return null;
      }
    });
  }

  @Override
  public void contextMenu(final String... names) {
    //move to menu
    for (int i = 0; i < names.length; i++) {
      String label = names[i];
      final boolean lastItem = i == names.length - 1;
      final MenuItem m = waitForMenuItem(label);
      syncExec(new MockRunnable<Boolean>() {
        @Override
        public Boolean run() throws Throwable {
          //toggle
          if ((m.getStyle() & (SWT.CHECK | SWT.RADIO)) != 0) {
            m.setSelection(!m.getSelection());
          }
          //fire selection
          Event event = new Event();
          event.display = getDisplay();
          event.time = (int) System.currentTimeMillis();
          event.type = SWT.Selection;
          event.widget = m;
          m.notifyListeners(event.type, event);
          if (lastItem) {
            //nop
          }
          return null;
        }
      });
      waitForIdle();
    }
  }

  @Override
  public List<String> getTableCells(int tableIndex, final int columnIndex) {
    final Table table = (Table) waitForIndexedField(FieldType.Table, tableIndex);
    return syncExec(new MockRunnable<List<String>>() {
      @Override
      public List<String> run() throws Throwable {
        ArrayList<String> list = new ArrayList<String>();
        for (TableItem row : table.getItems()) {
          //first column is dummy column
          list.add(row.getText(columnIndex + 1));
        }
        return list;
      }
    });
  }

  @Override
  public List<String> getTreeNodes(final int treeIndex) {
    final Tree tree = (Tree) waitForIndexedField(FieldType.Tree, treeIndex);
    return syncExec(new MockRunnable<List<String>>() {
      @Override
      public List<String> run() throws Throwable {
        ArrayList<String> list = new ArrayList<String>();
        addTreeItemsRec(tree.getItems(), list);
        return list;
      }
    });
  }

  @Override
  public Set<String> getSelectedTableCells(int tableIndex, final int columnIndex) {
    final Table table = (Table) waitForIndexedField(FieldType.Table, tableIndex);
    return syncExec(new MockRunnable<Set<String>>() {
      @Override
      public Set<String> run() throws Throwable {
        TreeSet<String> set = new TreeSet<String>();
        TableItem[] sel = table.getSelection();
        if (sel != null) {
          for (TableItem row : sel) {
            //first column is dummy column
            set.add(row.getText(columnIndex + 1));
          }
        }
        return set;
      }
    });
  }

  @Override
  public Set<String> getSelectedTreeNodes(int treeIndex) {
    final Tree tree = (Tree) waitForIndexedField(FieldType.Tree, treeIndex);
    return syncExec(new MockRunnable<Set<String>>() {
      @Override
      public Set<String> run() throws Throwable {
        TreeSet<String> set = new TreeSet<String>();
        TreeItem[] sel = tree.getSelection();
        if (sel != null) {
          for (TreeItem row : sel) {
            set.add(row.getText(0));
          }
        }
        return set;
      }
    });
  }

  @Override
  public Set<String> getCheckedTableCells(int tableIndex, final int columnIndex) {
    final Table table = (Table) waitForIndexedField(FieldType.Table, tableIndex);
    return syncExec(new MockRunnable<Set<String>>() {
      @Override
      public Set<String> run() throws Throwable {
        TreeSet<String> check = new TreeSet<String>();
        for (int i = 0; i < table.getItemCount(); i++) {
          TableItem item = table.getItem(i);
          if (item.getData() instanceof ITableRow) {
            ITableRow row = (ITableRow) item.getData();
            if (row.isChecked()) {
              check.add(item.getText(columnIndex + 1));
            }
          }
        }
        return check;
      }
    });
  }

  @Override
  public void gotoPoint(int x, int y) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void move(int deltaX, int deltaY) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void clickLeft() {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    m_bot.clickLeft();
    waitForIdle();
     */
  }

  @Override
  public void clickRight() {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    m_bot.clickRight();
    waitForIdle();
     */
  }

  @Override
  public void drag(int x1, int y1, int x2, int y2) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    gotoPoint(x1, y1);
    m_bot.pressLeft();
    gotoPoint(x2, y2);
    m_bot.releaseLeft();
    waitForIdle();
     */
  }

  @Override
  public void typeText(final String text) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    m_bot.typeText(text);
    waitForIdle();
     */
  }

  @Override
  public void paste(String text) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    //press paste (ctrl-V)
    m_bot.pressKey(Key.Control);
    m_bot.typeText("v");
    m_bot.releaseKey(Key.Control);
     */
  }

  @Override
  public void pressKey(Key key) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    m_bot.pressKey(key);
    waitForIdle();
     */
  }

  @Override
  public void releaseKey(Key key) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    m_bot.releaseKey(key);
    waitForIdle();
     */
  }

  @Override
  public void typeKey(Key key) {
    //XXX RAP
    throw new UnsupportedOperationException("not implemented");
    /*
    m_bot.typeKey(key);
    waitForIdle();
     */
  }

  @Override
  public WindowState getWindowState(final String title) {
    return syncExec(new MockRunnable<WindowState>() {
      @Override
      public WindowState run() throws Throwable {
        checkActiveShell();
        CTabItem view = findWorkbenchView(title);
        if (view != null) {
          WindowState state = new WindowState();
          Point p = view.getParent().toDisplay(0, 0);
          Point s = view.getParent().getSize();
          state.x = p.x;
          state.y = p.y;
          state.width = s.x;
          state.height = s.y;
          return state;
        }
        Shell shell = findShell(title);
        if (shell != null) {
          Rectangle r = shell.getBounds();
          WindowState state = new WindowState();
          state.x = r.x;
          state.y = r.y;
          state.width = r.width;
          state.height = r.height;
          return state;
        }
        throw new IllegalStateException("Window " + title + " not found");
      }
    });
  }

  @Override
  public String getClipboardText() {
    waitForIdle();
    return syncExec(new MockRunnable<String>() {
      @Override
      public String run() throws Throwable {
        //XXX RAP
//        Clipboard b = new Clipboard(getDisplay());
//        return (String) b.getContents(TextTransfer.getInstance());
        return "";
      }
    });
  }

  @Override
  public Object internal0(final Object o) {
    return syncExec(new MockRunnable<String>() {
      @Override
      public String run() throws Throwable {
        return null;
      }
    });
  }

  protected void checkActiveShell() {
    if (getActiveShell() == null) {
      throw new IllegalStateException("There is no active shell");
    }
  }

  protected FieldState getFieldStateInternal(Control c) {
    FieldState state = new FieldState();
    //type
    state.type = getFieldTypeOf(c);
    //scout name
    IPropertyObserver scoutObject = RwtScoutComposite.getScoutModelOnWidget(c);
    state.scoutName = (scoutObject != null ? scoutObject.getClass().getName() : null);
    //focus
    state.focus = (c == getDisplay().getFocusControl());
    //bounds
    Point p = c.toDisplay(0, 0);
    state.x = p.x;
    state.y = p.y;
    state.width = c.getBounds().width;
    state.height = c.getBounds().height;
    //text
    if (c instanceof Label) {
      state.text = ((Label) c).getText();
    }
    if (c instanceof Text) {
      state.text = ((Text) c).getText();
    }
    if (c instanceof StyledText) {
      state.text = ((StyledText) c).getText();
    }
    if (c instanceof Button) {
      state.text = ((Button) c).getText();
    }
    return state;
  }

  protected FieldType getFieldTypeOf(Control c) {
    if (c.isDisposed()) return null;
    if (!c.isVisible()) return null;
    //
    if (c instanceof Label) return FieldType.Label;
    if (c instanceof Text) return FieldType.Text;
    if (c instanceof StyledText) return FieldType.Text;
    if (c instanceof Table) return FieldType.Table;
    if (c instanceof Tree) return FieldType.Tree;
    if (c instanceof DropDownButton) return FieldType.DropdownButton;
    if (c instanceof Button) {
      int style = c.getStyle();
      if ((style & SWT.CHECK) != 0) return FieldType.Checkbox;
      else if ((style & SWT.RADIO) != 0) return FieldType.RadioButton;
      else if (c.getParent() instanceof Scrollable) return FieldType.ScrollButton;
      else return FieldType.PushButton;
    }
    return null;
  }

  protected String getScoutNameOf(Control c) {
    IPropertyObserver scoutObject = RwtScoutComposite.getScoutModelOnWidget(c);
    if (scoutObject != null) {
      return scoutObject.getClass().getName();
    }
    return null;
  }

  protected Display getDisplay() {
    IRwtEnvironment env = (IRwtEnvironment) m_session.getData(IRwtEnvironment.ENVIRONMENT_KEY);
    return env.getDisplay();
  }

  protected Shell getActiveShell() {
    return getDisplay().getActiveShell();
  }

  protected String cleanButtonLabel(String s) {
    return StringUtility.removeMnemonic(s);
  }

  protected TreeItem findTreeItemRec(TreeItem[] items, String nodeText) {
    if (items == null) return null;
    //
    for (TreeItem item : items) {
      if (nodeText.equals(item.getText())) {
        return item;
      }
      TreeItem found = findTreeItemRec(item.getItems(), nodeText);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  protected void addTreeItemsRec(TreeItem[] items, List<String> list) {
    if (items == null) return;
    //
    for (TreeItem item : items) {
      list.add(item.getText(0));
      addTreeItemsRec(item.getItems(), list);
    }
  }

  protected List<Composite> enumerateParentContainers() {
    return syncExec(new MockRunnable<ArrayList<Composite>>() {
      @Override
      public ArrayList<Composite> run() throws Throwable {
        ArrayList<Composite> list = new ArrayList<Composite>();
        for (Shell shell : getDisplay().getShells()) {
          if (shell.isVisible()) {
            list.add(shell);
          }
        }
        return list;
      }
    });
  }

  protected Shell findShell(final String title) {
    for (Shell shell : getDisplay().getShells()) {
      if (title.equals(shell.getText())) {
        return shell;
      }
    }
    return null;
  }

  protected CTabItem findWorkbenchView(final String title) {
    //XXX RAP
//    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();XXX RAP
//    if (shell != null) {
//      for (CTabFolder f : SwtUtility.findChildComponents(shell, CTabFolder.class)) {
//        if (f.getItemCount() > 0) {
//          for (CTabItem item : f.getItems()) {
//            if (item.isShowing()) {
//              if (title.equals(cleanButtonLabel(item.getText()))) {
//                return item;
//              }
//            }
//          }
//        }
//      }
//    }
    return null;
  }

  protected Control waitForPushButtonWithLabel(final String label) {
    return waitUntil(new WaitCondition<Control>() {
      @Override
      public Control run() {
        return syncExec(new MockRunnable<Control>() {
          @Override
          public Control run() throws Throwable {
            for (Shell shell : getDisplay().getShells()) {
              Composite parent = shell;
              for (Control o : RwtUtility.findChildComponents(parent, Control.class)) {
                if (o instanceof Button) {
                  if (cleanButtonLabel(label).equals(cleanButtonLabel(((Button) o).getText()))) return o;
                }
              }
            }
            return null;
          }
        });
      }
    });
  }

  protected Control waitForScoutField(final String name) {
    return waitUntil(new WaitCondition<Control>() {
      @Override
      public Control run() {
        return syncExec(new MockRunnable<Control>() {
          @Override
          public Control run() throws Throwable {
            Control lastSecondaryCandidate = null;
            for (Composite parent : enumerateParentContainers()) {
              for (Control c : RwtUtility.findChildComponents(parent, Control.class)) {
                String s = getScoutNameOf(c);
                if (s != null && ("." + s).endsWith("." + name)) {
                  lastSecondaryCandidate = c;
                  if (getFieldTypeOf(c) != null) {
                    //primary match
                    return c;
                  }
                }
              }
            }
            return lastSecondaryCandidate;
          }
        });
      }
    });
  }

  protected Control waitForIndexedField(final FieldType type, final int fieldIndex) {
    return waitUntil(new WaitCondition<Control>() {
      @Override
      public Control run() {
        return syncExec(new MockRunnable<Control>() {
          @Override
          public Control run() throws Throwable {
            List<Composite> parents = enumerateParentContainers();
            for (Composite parent : parents) {
              int index = 0;
              for (Control c : RwtUtility.findChildComponents(parent, Control.class)) {
                if (getFieldTypeOf(c) == type) {
                  if (index == fieldIndex) {
                    return c;
                  }
                  index++;
                }
              }
            }
            return null;
          }
        });
      }
    });
  }

  protected MenuItem waitForMenuItem(final String name) {
    return waitUntil(new WaitCondition<MenuItem>() {
      @Override
      public MenuItem run() {
        return syncExec(new MockRunnable<MenuItem>() {
          @Override
          public MenuItem run() throws Throwable {
            String label = cleanButtonLabel(name);
            //focus control
            Control focusControl = getDisplay().getFocusControl();
            if (focusControl != null) {
              Menu m = focusControl.getMenu();
              if (m != null) {
                for (MenuItem item : m.getItems()) {
                  if (label.equals(cleanButtonLabel(item.getText()))) {
                    return item;
                  }
                }
              }
            }
            //other controls
            for (Composite parent : enumerateParentContainers()) {
              for (Control c : RwtUtility.findChildComponents(parent, Control.class)) {
                Menu m = c.getMenu();
                if (m != null) {
                  for (MenuItem item : m.getItems()) {
                    if (label.equals(cleanButtonLabel(item.getText()))) {
                      return item;
                    }
                  }
                }
              }
            }
            //main menu
            for (Shell shell : getDisplay().getShells()) {
              Menu m = shell.getMenuBar();
              if (m != null) {
                for (MenuItem item : m.getItems()) {
                  if (label.equals(cleanButtonLabel(item.getText()))) {
                    return item;
                  }
                }
              }
            }
            return null;
          }
        });
      }
    });

  }

  protected <T> T syncExec(final MockRunnable<T> r) {
    if (getDisplay().getThread() != Thread.currentThread()) {
      final AtomicReference<T> ret = new AtomicReference<T>();
      final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
      try {
        getDisplay().syncExec(new Runnable() {
          @Override
          public void run() {
            try {
              ret.set(syncExec(r));
            }
            catch (Throwable t) {
              ex.set(t);
            }
          }
        });
        if (ex.get() != null) {
          throw ex.get();
        }
        return ret.get();
      }
      catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
    //
    try {
      return r.run();
    }
    catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  protected <T> T waitUntil(final WaitCondition<T> w) {
    try {
      return TestingUtility.waitUntil(WAIT_TIMEOUT, w);
    }
    catch (Throwable t) {
      throw new RuntimeException(t);
    }
    finally {
      waitForIdle();
    }
  }

  @Override
  public void pressLeft() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void releaseLeft() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void gotoTreeExpandIcon(int treeIndex, String nodeText) {
    throw new UnsupportedOperationException("not implemented");
  }
}