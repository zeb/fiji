
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.filter.RGBStackSplitter;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageStatistics;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Juanjo Vega
 */
public class DistanceMeter extends PlugInFrame {

    private final static String NEW_LINE = System.getProperty("line.separator");
    private final static Color roisColor = Color.YELLOW;//new Color(128, 255, 255);
    private final static Color labelColor = Color.BLUE;
    private final static Font largeFont = new Font("SansSerif", Font.PLAIN, 12);
    private final static Font smallFont = new Font("SansSerif", Font.PLAIN, 9);
    private DefaultListModel listModel = new DefaultListModel();
    private JList list = new JList(listModel);
    private DistanceListCellRenderer listCellRenderer = new DistanceListCellRenderer();
    private JButton jbProcessCell = new JButton(LABELS.PROCESS_CELL);
    private JButton jbRemove = new JButton(LABELS.REMOVE);
    private JButton jbClear = new JButton(LABELS.CLEAR);
    private JButton jbSave = new JButton(LABELS.SAVE);
    private JCheckBox jcbShowAll = new JCheckBox(LABELS.SHOW_ALL);
    private JLabel jlChannel = new JLabel(LABELS.CHANNEL);
    private ButtonGroup group = new ButtonGroup();
    private JRadioButton jrbR = new JRadioButton(LABELS.R);
    private JRadioButton jrbG = new JRadioButton(LABELS.G);
    private JRadioButton jrbB = new JRadioButton(LABELS.B);
    private JFileChooser fc = new JFileChooser();
    private String previousTool;

    public DistanceMeter() {
        super("Distance meter");

        setLayout(new BorderLayout());

        jbProcessCell.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                int channel = jrbR.isSelected() ? 0 : (jrbG.isSelected() ? 1 : 2);

                processCell(channel);
            }
        });

        jbRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                Object selected[] = list.getSelectedValues();
                for (int i = 0; i < selected.length; i++) {
                    listModel.removeElement(selected[i]);
                }
                updateButtons();
                drawROIs();
            }
        });

        jbClear.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                boolean choice = IJ.showMessageWithCancel("Confirm clear.", "You are about to clear list. Do you want to proceed?");
                if (choice) {
                    listModel.removeAllElements();
                }
                updateButtons();
                drawROIs();
            }
        });

        jbSave.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                exportROIs(listModel.toArray());
            }
        });

        jcbShowAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                drawROIs();
            }
        });

        //Group radio buttons.
        group.add(jrbR);
        group.add(jrbG);
        group.add(jrbB);

        jrbR.setSelected(true);

        SpringLayout layout = new SpringLayout();
        Panel panel = new Panel(layout);
        panel.add(Box.createVerticalGlue());
        panel.add(jbRemove);
        panel.add(jbClear);
        panel.add(jbSave);
        panel.add(jlChannel);
        panel.add(jrbR);
        panel.add(jrbG);
        panel.add(jrbB);
        panel.add(jcbShowAll);

        // Sets vertical spring layout.
        SpringUtilities.makeCompactGrid(panel, panel.getComponentCount(), 1, 1, 0, 5, 5);

        list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent lse) {
                if (!lse.getValueIsAdjusting()) {
                    updateButtons();
                    drawROIs();
                }
            }
        });

        list.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
                drawROIs();
            }

            @Override
            public void mousePressed(MouseEvent me) {
            }

            @Override
            public void mouseReleased(MouseEvent me) {
            }

            @Override
            public void mouseEntered(MouseEvent me) {
            }

            @Override
            public void mouseExited(MouseEvent me) {
            }
        });

        list.setCellRenderer(listCellRenderer);
        JScrollPane scrollPane = new JScrollPane(list);

        add(scrollPane, BorderLayout.CENTER);
        add(jbProcessCell, BorderLayout.NORTH);
        add(panel, BorderLayout.EAST);

        setLocationRelativeTo(null);

        updateButtons();

        pack();
    }

    @Override
    public void run(String arg) {
        setVisible(true);
    }

    private void updateButtons() {
        int nitems = listModel.getSize();
        int nselected = list.getSelectedIndices().length;

        jbRemove.setEnabled(nselected > 0);
        jbClear.setEnabled(nitems > 0);
        jbSave.setEnabled(nitems > 0);
    }

    private void processCell(int channelA) {
        final ImagePlus imp = WindowManager.getCurrentImage();

        if (imp != null) {
            final Roi roi = imp.getRoi();
            if (roi != null) {
                if (roi.isArea()) {
                    // Gets CenterOfMass from image ROI
                    final ImagePlus subImp = new ImagePlus("selected region", imp.getProcessor().crop());
                    subImp.copyScale(imp);  // Copies calibration info: pixels size, etc.

                    // Sets ROI to focus Center of Mass calculus.
                    Roi subRoi = (Roi) roi.clone();
                    subRoi.setLocation(0, 0);
                    subImp.setRoi(subRoi);

                    final Point CM = getCenterOfMass(subImp, channelA);
                    subImp.killRoi();   // Removes ROI to hide it.

                    // Adds a button to store points.
                    Button bOk = new Button(LABELS.SAVE_POINT);
                    bOk.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            if (storePoint(subImp, roi, CM)) {
                                subImp.close(); // Closes sub-image.
                                updateButtons();
                                drawROIs(imp);
                            }

                            IJ.setTool(previousTool);   // Restores previous tool.
                        }
                    });

                    // Creates a new window as it will be null until "show()",
                    // and if needs the butto nto be added and CM overlay to be drawn.
                    ImageWindow window = new ImageWindow(subImp);
                    window.add(bOk);
                    window.pack();

                    // Marks center of Mass.
                    Polygon cmPoint = new Polygon();
                    cmPoint.addPoint(CM.x, CM.y);
                    subImp.setOverlay(new PointRoi(cmPoint), roisColor, 3, Color.RED);

                    previousTool = IJ.getToolName();    // Stores current tool and...
                    IJ.setTool(Toolbar.CROSSHAIR);  // ...selects point tool.

                    subImp.show();
                } else {
                    IJ.error("ROI is not an area.");
                }
            } else {
                IJ.error("No ROI selected.");
            }
        } else {
            IJ.error("There are no images open.");
        }
    }

    private Point getCenterOfMass(ImagePlus imp, int channel) {
        RGBStackSplitter splitter = new RGBStackSplitter();
        splitter.split(imp.getStack(), true);

        ImageStack stack = null;
        switch (channel) {
            case 0:
                stack = splitter.red;
                break;
            case 1:
                stack = splitter.green;
                break;
            case 2:
                stack = splitter.blue;
                break;
        }

        ImagePlus aux = new ImagePlus("CM", stack);
//        aux.setCalibration(imp.getCalibration());
        aux.setRoi(imp.getRoi());
        ImageStatistics is = aux.getStatistics(ImageStatistics.CENTER_OF_MASS);

        aux.copyScale(imp); // Setting this beforehand causes worng center of mass placement.

        Point CM = new Point();
        CM.setLocation(is.xCenterOfMass, is.yCenterOfMass);

        return CM;
    }

    private boolean storePoint(ImagePlus imp, Roi roi, Point CM) {
        Roi point = imp.getRoi();

        if (point != null) {
            if (point instanceof PointRoi) {
                PointRoi pr = (PointRoi) point;
                Point p = new Point();
                p.setLocation(pr.getBounds().x, pr.getBounds().y);

                // Adds a new point.
                DistanceItem item = new DistanceItem(imp, new Roi(roi.getBounds()), CM, p);
                listModel.addElement(item);

                return true;
            } else {
                IJ.error("ROI is not a point.");
            }
        } else {
            IJ.error("No ROI selected.");
        }

        return false;
    }

    private void exportROIs(Object items[]) {
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file != null) {
                if (save(items, file.getAbsolutePath())) {
                    IJ.showMessage("Save", "File saved sucesfully.");
                }
            }
        }
    }

    public static boolean save(Object items[], String path) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path));

            for (int i = 0; i < items.length; i++) {
                DistanceItem item = (DistanceItem) items[i];

                out.write(item.getDistance() + NEW_LINE);
            }

            out.close();

            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
            IJ.error(ex.getMessage());
        }

        return false;
    }

    private void drawROIs() {
        drawROIs(WindowManager.getCurrentImage());
    }

    private void drawROIs(ImagePlus imp) {
        Object rois[] = jcbShowAll.isSelected() ? listModel.toArray() : list.getSelectedValues();
        int indexes[] = jcbShowAll.isSelected() ? null : list.getSelectedIndices();

        drawROIs(imp, rois, indexes);
    }

    private static void drawROIs(ImagePlus imp, Object items[], int indexes[]) {
        ImageCanvas canvas = imp.getCanvas();
        Graphics g = canvas.getGraphics();

        canvas.update(g);

        g.setColor(roisColor);

        for (int i = 0; i < items.length; i++) {
            String label = "[" + (indexes != null ? indexes[i] : i) + "]";
            Roi roi = ((DistanceItem) items[i]).getRoi();

            if (roi.getType() == Roi.COMPOSITE) {
                roi.setImage(imp);
                Color c = Roi.getColor();
                Roi.setColor(roisColor);
                roi.draw(g);
                Roi.setColor(c);
            } else {
                Polygon p = roi.getPolygon();
                int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                for (int j = 0; j < p.npoints; j++) {
                    x2 = canvas.screenX(p.xpoints[j]);
                    y2 = canvas.screenY(p.ypoints[j]);
                    if (j > 0) {
                        g.drawLine(x1, y1, x2, y2);
                    }
                    x1 = x2;
                    y1 = y2;
                }
                if (roi.isArea() && p.npoints > 0) {
                    int x0 = canvas.screenX(p.xpoints[0]);
                    int y0 = canvas.screenY(p.ypoints[0]);
                    g.drawLine(x1, y1, x0, y0);
                }
            }
            drawRoiLabel(label, canvas, roi.getBounds());
        }
    }

    private static void drawRoiLabel(String label, ImageCanvas canvas, Rectangle r) {
        Graphics g = canvas.getGraphics();

        int x = canvas.screenX(r.x);
        int y = canvas.screenY(r.y);
        double mag = canvas.getMagnification();
        int width = (int) (r.width * mag);
        int height = (int) (r.height * mag);
        int size = width > 40 && height > 40 ? 12 : 9;
        if (size == 12) {
            g.setFont(largeFont);
        } else {
            g.setFont(smallFont);
        }

        FontMetrics metrics = g.getFontMetrics();
        int labelW = metrics.stringWidth(label);
        int labelH = metrics.getHeight();

        g.setColor(roisColor);

        g.fillRect(x, y, labelW, labelH);

        y += labelH - 3;

        g.setColor(labelColor);
        g.drawString(label, x, y);
    }
}
