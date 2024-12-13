/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * @author shunninghuang@gmail.com
 */
public class AboutForm extends JFrame {

    public AboutForm() {
        initializeComponent();
    }

    private void btnOK_Click(ActionEvent e) {
        this.setVisible(false);
    }

    private static final String readme = """
            Detail: https://www.codeproject.com/Tips/646359/M-NET
            You should install Microsoft .NET Framework 3.5 or higher before running the program. You should download M1.NET ROM files in roms directory.
            Hotkey: F10 -- toggle global throttle.
            M1.NET ROM files: https://pan.baidu.com/s/14bR2wEzU2Qqx5hM7hJXMZA https://drive.google.com/folderview?id=11brIxHTJ_M4yOkm08wR3LOibZyEitcAw
            Email: shunninghuang@gmail.com
            """;

    final WindowListener aboutForm_Load = new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
            pictureBox1.setIcon(new ImageIcon(AboutForm.class.getResource("/1.png")));
            lbVersion.setText(Version.build_version);
            lbAuthor.setText("by " + Version.author);
            tbShow.setText(readme);
            scrollPane.setVisible(false);
        }
    };

    private void btnShow_Click(ActionEvent e) {
        if (btnShow.getText().equals("show")) {
            this.setSize(getWidth(), 512);
            scrollPane.setVisible(true);
            btnShow.setText("hide");
        } else if (btnShow.getText().equals("hide")) {
            this.setSize(getWidth(), 256);
            scrollPane.setVisible(false);
            btnShow.setText("show");
        }
    }

//#region Windows Form Designer generated code

    /**
     * Required method for Designer support - do not modify
     * the contents of this method with the code editor.
     */
    private void initializeComponent() {
        this.btnOK = new JButton();
        this.lbVersion = new JLabel();
        this.lbAuthor = new JLabel();
        this.pictureBox1 = new JLabel();
        this.btnShow = new JButton();
        this.tbShow = new JTextArea();

        //
        // btnOK
        //
        this.btnOK.setText("OK");
        this.btnOK.addActionListener(this::btnOK_Click);
        //
        // lbVersion
        //
        this.lbVersion.setText("...");
        //
        // lbAuthor
        //
        this.lbAuthor.setText("...");
        //
        // pictureBox1
        //
        //
        // btnShow
        //
        this.btnShow.setText("show");
        this.btnShow.addActionListener(this::btnShow_Click);
        //
        // tbShow
        //
        this.tbShow.setRows(5);
        this.tbShow.setColumns(32);
        //
        // AboutForm
        //
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.setPreferredSize(new Dimension(496, 218));

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(this.tbShow);

        panel.add(this.pictureBox1);
        panel.add(this.lbAuthor);
        panel.add(this.lbVersion);
        panel.add(this.btnShow);
        panel.add(this.btnOK);
        panel.add(scrollPane);

        this.setContentPane(panel);

        this.setTitle("About");
        this.addWindowListener(aboutForm_Load);

        this.pack();
    }

//#endregion

    JScrollPane scrollPane;
    private JButton btnOK;
    private JLabel lbVersion;
    private JLabel lbAuthor;
    private JLabel pictureBox1;
    private JButton btnShow;
    private JTextArea tbShow;
}
