
import ij.IJ;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SpringLayout;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Juanjo Vega
 */
public class DistanceListCellRenderer extends JPanel implements ListCellRenderer {

    private JLabel jlIndex = new JLabel();
    private JLabel jlThumbnail = new JLabel();
    private JLabel jlInfo = new JLabel();

    public DistanceListCellRenderer() {
        super();

        jlIndex.setHorizontalTextPosition(JLabel.LEFT);
        jlInfo.setHorizontalTextPosition(JLabel.LEFT);

        jlIndex.setVerticalTextPosition(JLabel.CENTER);
        jlInfo.setVerticalTextPosition(JLabel.CENTER);

        SpringLayout layout = new SpringLayout();
        setLayout(layout);

        add(jlIndex);
        add(jlThumbnail);
        add(jlInfo);

        // Horizontal layout.
        layout.putConstraint(SpringLayout.WEST, jlIndex, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.WEST, jlThumbnail, 5, SpringLayout.EAST, jlIndex);
        layout.putConstraint(SpringLayout.WEST, jlInfo, 5, SpringLayout.EAST, jlThumbnail);

        layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, jlThumbnail);

        // Vertical centering according to icon.
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, jlIndex, 0, SpringLayout.VERTICAL_CENTER, jlThumbnail);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, jlInfo, 0, SpringLayout.VERTICAL_CENTER, jlThumbnail);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        DistanceItem item = (DistanceItem) value;

        jlIndex.setText("<html><b>" + index/*nformatter.format(index)*/ + ": </b></html>");
        jlThumbnail.setIcon(new ImageIcon(item.getThumbnail()));
        jlInfo.setText("<html>" + IJ.d2s(item.getDistance(), 2) + " " + item.getUnit() + "</html>");

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}
