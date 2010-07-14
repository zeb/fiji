package ij3d;

import java.awt.Panel;
import java.awt.Checkbox;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Scrollbar;
import java.awt.Label;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Button;
import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.media.j3d.View;

import javax.vecmath.Color3f;

import util.BorderPanel;

public class AttributesPanel extends Panel
			implements UniverseListener, AdjustmentListener {

	private Image3DUniverse univ;
	private Component[] panelsToDisable;
	private Content content;

	private Checkbox redBox, greenBox, blueBox;
	private Checkbox showBox;
	private Checkbox applyToAllBox;
	private ColorButton colorButton;
	private Label nothingSelected;
	private Scrollbar transparencySlider;

	public AttributesPanel(final Image3DUniverse univ) {
		super();
		this.univ = univ;
		Color3f bg = new Color3f();
		((ImageCanvas3D)univ.getCanvas()).getBG().getColor(bg);
		this.setBackground(bg.get());
		this.setForeground(Color.WHITE);
		this.setLayout(new GridBagLayout());

		univ.addUniverseListener(this);

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 0.5;

		// channels
		ItemListener channelListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(content == null)
					return;
				boolean[] ch = new boolean[] {
					redBox.getState(),
					greenBox.getState(),
					blueBox.getState() };
				if(applyToAll())
					content.setChannels(ch);
				else
					content.getCurrent().setChannels(ch);
			}
		};
		BorderPanel bp1 = new BorderPanel("Channels");
		bp1.setLayout(gridbag);
		c.gridx = 0; c.gridy = 0;
		redBox = new Checkbox("Red");
		redBox.addItemListener(channelListener);
		bp1.add(redBox, c);
		c.gridx++;
		greenBox = new Checkbox("Green");
		greenBox.addItemListener(channelListener);
		bp1.add(greenBox, c);
		c.gridx++;
		blueBox = new Checkbox("Blue");
		blueBox.addItemListener(channelListener);
		bp1.add(blueBox, c);

		// colors
		BorderPanel bp2 = new BorderPanel("Color");
		bp2.setLayout(gridbag);
		c.gridx = 0; c.gridy = 0;
		bp2.add(new Label("Change color"), c);
		c.gridx++;
		colorButton = new ColorButton("   ");
		colorButton.setFG(Color.RED);
		colorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(content != null)
					univ.getExecuter().changeColor(content);
			}
		});
		bp2.add(colorButton, c);

		// transparency
		BorderPanel bp3 = new BorderPanel("Transparency");
		bp3.setLayout(gridbag);
		c.gridx = 0; c.gridy = 0;
		c.gridwidth = 2;
		final Label transparency = new Label("0    ");
		transparencySlider = addSlider(
			bp3, transparency, 0, 100, 0, c);

		// Hide/Show
		BorderPanel bp4 = new BorderPanel("Hide/Show");
		bp4.setLayout(gridbag);
		c.gridx = 0; c.gridy = 0;
		showBox = new Checkbox("Show object");
		showBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(content == null)
					return;
				boolean v = showBox.getState();
				if(applyToAll())
					content.setVisible(v);
				else
					content.getCurrent().setVisible(v);
			}
		});
		bp4.add(showBox, c);

		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;

		c.gridx = 0; c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHEAST;
		this.add(new CloseButton(), c);

		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		c.gridy++;
		this.add(bp1, c);

		c.gridy++;
		this.add(bp2, c);

		c.gridy++;
		this.add(bp3, c);

		c.gridy++;
		this.add(bp4, c);

		c.gridy++;
		nothingSelected = new Label("Nothing selected");
		this.add(nothingSelected, c);

		// add an empty panel with weighty specified. This
		// causes the components to be positioned at the top
		c.gridy++;
		c.weighty = 1.0;
		this.add(new Panel(), c);

		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.SOUTHWEST;
		applyToAllBox = new Checkbox("Apply to all timepoints", true);
		this.add(applyToAllBox, c);

		panelsToDisable = new Component[] {
			bp1, bp2, bp3, bp4, applyToAllBox};
		contentSelected(univ.getSelected());
	}

	public Dimension getPreferredSize() {
		return new Dimension(200, 500);
	}

	private boolean applyToAll() {
		return applyToAllBox.getState();
	}

	private Scrollbar addSlider(Panel p, final Label valueL,
			int min, int max, int value, GridBagConstraints c){

		c.gridx = 0;
		final Scrollbar s;
		final Label label = new Label("255");
		s = new Scrollbar(Scrollbar.HORIZONTAL, value, 1, min, max + 1);
		s.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				valueL.setText(Integer.toString(s.getValue()));
			}
		});
		s.addAdjustmentListener(this);
		s.setUnitIncrement(1);
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(s, c);
		c.fill = GridBagConstraints.NONE;
		c.gridx++;
		p.add(valueL, c);
		return s;
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		if(e.getSource() == transparencySlider) {
			if(content == null)
				return;
			float t = transparencySlider.getValue() / 100f;
			if(applyToAll())
				content.setTransparency(t);
			else
				content.getCurrent().setTransparency(t);
		}
	}

	public void transformationStarted(View view) {}
	public void transformationUpdated(View view) {}
	public void transformationFinished(View view) {}
	public void contentAdded(Content c) {}
	public void contentRemoved(Content c) {}
	public void canvasResized() {}
	public void universeClosed() {}

	public void contentChanged(Content c) {
		if(c == this.content)
			updateFields();
	}

	public void contentSelected(Content c) {
		this.content = c;
		updateFields();
		validate();
	}

	public void updateFields() {
		Content c = this.content;
		boolean vis = c != null;
		for(int i = 0; i < panelsToDisable.length; i++)
			panelsToDisable[i].setVisible(vis);
		nothingSelected.setVisible(c == null);
		if(c == null)
			return;
		boolean[] ch = c.getChannels();
		redBox.setState(ch[0]);
		greenBox.setState(ch[1]);
		blueBox.setState(ch[2]);

		Color3f col = c.getColor();
		colorButton.setFG(col == null ?
			Color.WHITE : col.get());
		transparencySlider.setValue(
			(int)(c.getTransparency() * 100 + 0.5));
		showBox.setState(c.isVisible());
	}

	private class ColorButton extends Button {

		private Color fg;

		public ColorButton(String label) {
			super(label);
		}

		public void setFG(Color fg) {
			this.fg = fg;
			repaint();
		}

		@Override
		public void update(Graphics g) {
			super.update(g);
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			g.setColor(fg);
			g.fillRect(5, 5, getWidth() - 10, getHeight() - 10);
			g.dispose();
		}
	}

	private class CloseButton extends Component implements MouseListener {

		private final Color PRESSED = new Color(139, 5, 0);
		private final Color NORMAL = Color.RED;

		private Color color = NORMAL;

		public CloseButton() {
			super();
			Dimension dim = new Dimension(20, 20);
			setPreferredSize(dim);
			setMinimumSize(dim);
			setMaximumSize(dim);
			addMouseListener(this);
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g) {
			super.paint(g);
			g.setColor(color);
			g.fillRoundRect(5, 5, 10, 10, 2, 2);
			g.setColor(Color.WHITE);
			g.drawRoundRect(5, 5, 10, 10, 2, 2);
			g.drawLine(6, 6, 14, 14);
			g.drawLine(14, 6, 6, 14);
		}

		public void mousePressed(MouseEvent e) {
			color = PRESSED;
			repaint();
		}

		public void mouseReleased(MouseEvent e) {
			color = NORMAL;
			repaint();
		}

		public void mouseClicked(MouseEvent e) {
			univ.detachAttributesPanel();
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
	}
}

