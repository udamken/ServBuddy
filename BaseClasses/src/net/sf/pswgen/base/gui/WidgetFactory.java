package net.sf.pswgen.base.gui;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Collections;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import net.sf.pswgen.base.util.ConstantsProvider;
import net.sf.pswgen.base.util.ConverterHelper;
import net.sf.pswgen.base.util.Services;

/**
 * <p>
 * Erzeugt Widgets, also GUI-Elemente, auf vereinfachte und für von mir geschriebene Anwendungen
 * standardisierte Weise.
 * </p>
 * <p>
 * (c) 2005-2012, by Uwe Damken
 * </p>
 */
public class WidgetFactory {

	/** Die eine und einzige Instanz dieser Klasse */
	private static WidgetFactory instance = null;

	/** Hashtable mit Informationen zu allen Widgets, die hier erzeugt werden können */
	private Hashtable<String, WidgetInfo> widgetInfos = new Hashtable<String, WidgetInfo>();

	/** Konstruktor ist nicht öffentlich zugreifbar => getInstance() nutzen */
	private WidgetFactory() {
		super();
	}

	/**
	 * Liefert die eine und einzige Instanz dieser Klasse.
	 */
	public static synchronized WidgetFactory getInstance() {
		if (instance == null) { // Noch nicht instantiiert und initialisiert?
			instance = new WidgetFactory();
			instance.initialize();
		}
		return instance;
	}

	/**
	 * Initialisiert die eine und einzige Instanz.
	 */
	private void initialize() {
		String pkg = Services.getInstance().getConstants().getApplicationPackageName();
		ResourceBundle bundle = ResourceBundle.getBundle(pkg + ".Widgets");
		for (String key : Collections.list(bundle.getKeys())) {
			String value = bundle.getString(key);
			Matcher matcher = Pattern.compile("\\s*([^,]*)(\\s*,\\s*(\\d+),\\s*(\\d+))?\\s*").matcher(value);
			if (!matcher.matches()) {
				Services.getInstance()
						.getLogger()
						.log(Level.WARNING, ConstantsProvider.MSG_INVALID_WIDGET_INFO,
								new Object[] { key, value });
			} else {
				String text = matcher.group(1);
				String preferredWidth = matcher.group(3);
				String preferredHeight = matcher.group(4);
				text = (text.length() == 0) ? null : text;
				if (preferredWidth == null || preferredHeight == null) {
					widgetInfos.put(key, new WidgetInfo(text));
				} else {
					widgetInfos.put(key, new WidgetInfo(text, ConverterHelper.toInt(preferredWidth),
							ConverterHelper.toInt(preferredHeight)));
				}
			}
		}
	}

	/**
	 * Liefert den GUI-Text eines Widgets.
	 */
	public String getWidgetText(String name) {
		return getWidgetInfo(name).getText();
	}

	/**
	 * Liefert Informationen zu dem Widget mit dem Namen name oder einen Dummy-Eintrag, wenn der Name nicht
	 * bekannt ist.
	 */
	private WidgetInfo getWidgetInfo(final String name) {
		WidgetInfo wi = widgetInfos.get(name);
		if (wi == null) {
			wi = new WidgetInfo("<" + name + ">");
			Services.getInstance().getLogger().log(Level.WARNING, ConstantsProvider.MSG_NO_WIDGET_INFO, name);
		}
		return wi;
	}

	/**
	 * Liefert einen Button mit einem damit verbundenen ActionListener der für den Event ActionPerformed im
	 * Controller pre eine Methode name für die auslösende View view aufruft.
	 */
	public JButton getButton(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JButton button = new JButton();
		button.setText(wi.getText());
		if (wi.getPreferredSize() != null) {
			button.setPreferredSize(wi.getPreferredSize());
		}
		return button;
	}

	/**
	 * Liefert ein Label.
	 */
	public JLabel getLabel(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JLabel label = new JLabel();
		label.setText(wi.getText());
		if (wi.getPreferredSize() != null) {
			label.setPreferredSize(wi.getPreferredSize());
		}
		return label;
	}

	/**
	 * Liefert eine CheckBox.
	 */
	public JCheckBox getCheckBox(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JCheckBox checkbox = new JCheckBox();
		checkbox.setText(wi.getText());
		if (wi.getPreferredSize() != null) {
			checkbox.setPreferredSize(wi.getPreferredSize());
		}
		return checkbox;
	}

	/**
	 * Liefert eine Tabelle.
	 */
	public JTable getTable(final String widgetName, AbstractTableModel model) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JTable table = new JTable(model);
		if (wi.getPreferredSize() != null) {
			table.setPreferredScrollableViewportSize(wi.getPreferredSize());
		}
		table.setFillsViewportHeight(true);
		return table;
	}

	/**
	 * Liefert ein Panel, das als ContentPane gestaltet ist.
	 */
	public JPanel getContentPane(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		if (wi.getPreferredSize() != null) {
			panel.setPreferredSize(wi.getPreferredSize());
		}
		return panel;
	}

	/**
	 * Liefert ein Panel.
	 */
	public JPanel getPanel(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED), wi.getText(),
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
		if (wi.getPreferredSize() != null) {
			panel.setPreferredSize(wi.getPreferredSize());
		}
		return panel;
	}

	/**
	 * Liefert ein FormattedTextField.
	 */
	public DbcIntegerField getIntegerField(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		DbcIntegerField field = new DbcIntegerField();
		if (wi.getPreferredSize() != null) {
			field.setPreferredSize(wi.getPreferredSize());
		}
		return field;
	}

	/**
	 * Liefert ein PasswordField.
	 */
	public JPasswordField getPasswordField(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JPasswordField field = new JPasswordField();
		if (wi.getPreferredSize() != null) {
			field.setPreferredSize(wi.getPreferredSize());
		}
		return field;
	}

	/**
	 * Liefert ein TextField.
	 */
	public JTextField getTextField(final String widgetName) {
		WidgetInfo wi = getWidgetInfo(widgetName);
		JTextField field = new JTextField();
		if (wi.getPreferredSize() != null) {
			field.setPreferredSize(wi.getPreferredSize());
		}
		return field;
	}

	/**
	 * Enthält Informationen für ein Widget, also ein GUI-Element.
	 * <p>
	 * (c) 2005-2012, by Uwe Damken
	 */
	private class WidgetInfo {

		/** Der für das Widget anzuzeigende Text */
		private String text;

		/** Die bevorzugte Größe des Widgets */
		private Dimension preferredSize;

		/**
		 * Konstruktiert ein Widget mit einem Text.
		 */
		public WidgetInfo(final String text) {
			this.text = text;
		}

		/**
		 * Konstruktiert ein Widget mit allen Informationen.
		 */
		public WidgetInfo(final String text, final int preferredWidth, final int preferredHeight) {
			this.text = text;
			preferredSize = new Dimension(preferredWidth, preferredHeight);
		}

		/**
		 * @return Returns the text.
		 */
		public String getText() {
			return text;
		}

		/**
		 * @return Returns the preferredWidth.
		 */
		public Dimension getPreferredSize() {
			return preferredSize;
		}

	}

}