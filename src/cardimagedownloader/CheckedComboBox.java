/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cardimagedownloader;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboPopup;

class CheckedComboBox<E extends CheckableItem> extends JComboBox<E> {
  private boolean keepOpen;
  private transient ActionListener listener;

  protected CheckedComboBox() {
    super();
  }
  protected CheckedComboBox(ComboBoxModel<E> aModel) {
    super(aModel);
  }
  protected CheckedComboBox(E[] m) {
    super(m);
  }
  
  @Override 
  public Dimension getPreferredSize() {
    return new Dimension(200, 20);
  }
  
  @Override 
  public void updateUI() {
    setRenderer(null);
    removeActionListener(listener);
    super.updateUI();
    listener = e -> {
      if ((e.getModifiers() & InputEvent.MOUSE_EVENT_MASK) != 0) {
        updateItem(getSelectedIndex());
        keepOpen = true;
      }
    };
    setRenderer(new CheckBoxCellRenderer<>());
    addActionListener(listener);
    getInputMap(JComponent.WHEN_FOCUSED).put(
        KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "checkbox-select");
    getActionMap().put("checkbox-select", new AbstractAction() {
      @Override 
      public void actionPerformed(ActionEvent e) {
        Accessible a = getAccessibleContext().getAccessibleChild(0);
        if (a instanceof BasicComboPopup) {
          BasicComboPopup pop = (BasicComboPopup) a;
          updateItem(pop.getList().getSelectedIndex());
        }
      }
    });
  }
  private void updateItem(int index) {
    if (isPopupVisible()) {
      E item = getItemAt(index);
      item.selected ^= true;
      removeItemAt(index);
      insertItemAt(item, index);
      setSelectedItem(item);
      if(item.toString().contains("*.*") && item.selected){
          for(int i = 1; i < getItemCount(); i ++){
              if(getItemAt(i).selected)
                  getItemAt(i).selected = false;
          }
      } else
          getItemAt(0).selected = false;
    }
  }
  @Override public void setPopupVisible(boolean v) {
    if (keepOpen) {
      keepOpen = false;
    } else {
      super.setPopupVisible(v);
    }
  }
}

class CheckBoxCellRenderer<E extends CheckableItem> implements ListCellRenderer<E> {
  private final JLabel label = new JLabel(" ");
  private final JCheckBox check = new JCheckBox(" ");

  @Override 
  public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
    if (index < 0) {
      String txt = getCheckedItemString(list.getModel());
      label.setText(txt.isEmpty() ? " " : txt);
      return label;
    } else {
      check.setText(Objects.toString(value, ""));
      check.setSelected(value.isSelected());
      if (isSelected) {
        check.setBackground(list.getSelectionBackground());
        check.setForeground(list.getSelectionForeground());
      } else {
        check.setBackground(list.getBackground());
        check.setForeground(list.getForeground());
      }
      return check;
    }
  }

  private static <E extends CheckableItem> String getCheckedItemString(ListModel<E> model) {
    return IntStream.range(0, model.getSize())
      .mapToObj(model::getElementAt)
      .filter(CheckableItem::isSelected)
      .map(Objects::toString)
      .sorted()
      .collect(Collectors.joining(", "));
  }
}

class CheckableItem {
  public final String text;
  boolean selected;

  protected CheckableItem(String text, boolean selected) {
    this.text = text;
    this.selected = selected;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  @Override 
  public String toString() {
    return text;
  }
}