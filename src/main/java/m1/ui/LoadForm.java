/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;

import m1.emu.Machine;
import m1.emu.Mame;
import m1.emu.RomInfo;
import vavi.swing.binding.table.TableModel;

import static java.lang.System.getLogger;


/**
 * @author shunninghuang@gmail.com
 */
public class LoadForm extends JFrame {

    private static final Logger logger = getLogger(LoadForm.class.getName());

    private final MainForm _myParentForm;
    private final int currentCol = -1;
    private boolean sort;
    public boolean bLoad;

//    static class ListViewItemComparer implements Comparator<String> {
//
//        public boolean sort_b;
//        public SortOrder order = SortOrder.Ascending;
//        private int col;
//
//        public ListViewItemComparer() {
//            col = 0;
//        }
//
//        public ListViewItemComparer(int column, boolean sort) {
//            col = column;
//            sort_b = sort;
//        }
//
//        @Override
//        public int compare(String x, String y) {
//            if (sort_b) {
//                return String.Compare(((ListViewItem) x).SubItems[col].Text, ((ListViewItem) y).SubItems[col].Text);
//            } else {
//                return String.Compare(((ListViewItem) y).SubItems[col].Text, ((ListViewItem) x).SubItems[col].Text);
//            }
//        }
//    }

    public LoadForm(MainForm form) {
        this._myParentForm = form;
        initializeComponent();
    }

    private void btnOK_Click(ActionEvent e) {
        try {
            ApplyRom();
        } catch (IOException ex) {
            logger.log(Level.DEBUG, ex.getMessage(), e);
        }
    }

    private void btnCancel_Click(ActionEvent e) {
        bLoad = false;
        this.setVisible(false);
    }

    private final MouseListener listView1_DoubleClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1) {
                try {
                    ApplyRom();
                } catch (IOException ex) {
                    logger.log(Level.DEBUG, ex.getMessage(), e);
                }
            }
        }
    };

    private void ApplyRom() throws IOException {
logger.log(Level.DEBUG, "selection count: " + table.getSelectionModel().getSelectedItemsCount());
        if (table.getSelectionModel().getSelectedItemsCount() > 0) {
            bLoad = true;
            this.setVisible(false);
            Mame.exit_pending = true;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            RomInfo.rom = RomInfo.getRomByName((String) model.getValueAt(row, 2));
logger.log(Level.DEBUG, "selected: " + RomInfo.rom);
            this._myParentForm.LoadRom();
            if (Machine.bRom) {
                Mame.exit_pending = false;
            }
        }
    }

//    private void listView1_ColumnClick(ListSelectionEvent e) {
//        if (e.getValueIsAdjusting()) {
//            return;
//        }
//        String Asc = "%2c".formatted((char) 0x25bc);
//        String Des = "%2c".formatted((char) 0x25b2);
//        if (!sort) { TODO
//            sort = true;
//            String oldStr = this.table.Columns[e.Column].Text.TrimEnd((char) 0x25bc, (char) 0x25b2, ' ');
//            this.table.Columns[e.Column].setText(oldStr + Des);
//        } else if (sort) {
//            sort = false;
//            String oldStr = this.table.Columns[e.Column].Text.TrimEnd((char) 0x25bc, (char) 0x25b2, ' ');
//            this.table.Columns[e.Column].setText(oldStr + Asc);
//        }
//        table.ListViewItemSorter = new ListViewItemComparer(e.Column, sort);
//        int rowCount = this.table.Items.size();
//        if (currentCol != -1) {
//            for (int i = 0; i < rowCount; i++) {
//                this.table.Items[i].UseItemStyleForSubItems = false;
//                this.table.Items[i].SubItems[currentCol].BackColor = Color.White;
//                if (e.Column != currentCol)
//                    this.table.Columns[currentCol].setText(this.table.Columns[currentCol].Text.TrimEnd((char) 0x25bc, (char) 0x25b2, ' '));
//            }
//        }
//        for (int i = 0; i < rowCount; i++) {
//            this.table.Items[i].UseItemStyleForSubItems = false;
//            this.table.Items[i].SubItems[e.Column].BackColor = Color.WhiteSmoke;
//            currentCol = e.Column;
//        }
//    }

    private final WindowListener loadForm_FormClosing = new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            if (bLoad) {
                _myParentForm.OnLoaded(true);
            } else {
                _myParentForm.OnLoaded(false);
            }
        }
    };

//#region Windows Form Designer generated code

    /**
     * Required method for Designer support - do not modify
     * the contents of this method with the code editor.
     */
    private void initializeComponent() {
        this.table = new JTable();
        this.btnOK = new JButton();
        this.btnCancel = new JButton();
        //
        // table
        //
        this.table.addMouseListener(this.listView1_DoubleClick);
//        this.table.getSelectionModel().addListSelectionListener(this::listView1_ColumnClick);
        this.table.setPreferredScrollableViewportSize(new Dimension(884, 635));
        this.table.setFillsViewportHeight(true);
        this.table.setAutoCreateRowSorter(true);
        //
        // btnOK
        //
        this.btnOK.setText("OK");
        this.btnOK.addActionListener(this::btnOK_Click);
        //
        // btnCancel
        //
        this.btnCancel.setText("cancel");
        this.btnCancel.addActionListener(this::btnCancel_Click);
        //
        // LoadForm
        //
        this.getContentPane().add(this.btnCancel);
        this.getContentPane().add(this.btnOK);
        this.getContentPane().add(this.table);
        this.setTitle("Load");
        this.addWindowListener(this.loadForm_FormClosing);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(table);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        this.setContentPane(panel);
        this.pack();
    }

//#endregion

    public TableModel<?> model;
    JTable table;
    private JButton btnOK;
    private JButton btnCancel;
}
