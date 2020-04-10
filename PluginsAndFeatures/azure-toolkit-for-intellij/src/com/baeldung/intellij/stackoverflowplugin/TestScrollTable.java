package com.baeldung.intellij.stackoverflowplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.intellij.helpers.base.BaseEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class TestScrollTable extends BaseEditor {
    private JPanel mainPanel;
    private com.intellij.ui.table.JBTable table1;
    private JTextField textField1;

    public TestScrollTable() {
        String[][] data = {
        };

        // Column Names
        String[] columnNames = { "Name", "Roll Number", "Department" };
        DefaultTableModel model  = new DefaultTableModel(data, columnNames);
        table1.setModel(model);
        for (int i = 0; i< 100; i++) {
            model.addRow(new Object[] {"" + (i+1), "Andy"  + i, "1312" + i});
        }
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @NotNull
    @Override
    public String getName() {
        return "xgf";
    }

    @Override
    public void dispose() {

    }
}
