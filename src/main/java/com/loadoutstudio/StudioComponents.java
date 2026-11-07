/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Component-local presentation; never changes global RuneLite defaults. */
final class StudioComponents
{
	private static final Color GOLD = StudioTheme.ACCENT, BG = StudioTheme.BACKGROUND, CONTROL = StudioTheme.CONTROL,
			OUTLINE = StudioTheme.BORDER, TEXT = StudioTheme.TEXT, MUTED = StudioTheme.MUTED;
	private static final Font BODY_FONT = StudioTheme.BODY, ACTION_FONT = StudioTheme.ACTION;

	private StudioComponents()
	{
	}

	static void themeMenu(javax.swing.JPopupMenu menu)
	{
		menu.setBackground(CONTROL);
		menu.setBorder(BorderFactory.createLineBorder(OUTLINE));
		for (Component child : menu.getComponents())
		{
			child.setFont(BODY_FONT);
			child.setForeground(TEXT);
			child.setBackground(CONTROL);
			if (child instanceof javax.swing.JMenuItem)
			{
				javax.swing.JMenuItem item = (javax.swing.JMenuItem) child;
				item.putClientProperty("html.disable", true);
				item.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
			}
			if (child instanceof javax.swing.JMenu)
				themeMenu(((javax.swing.JMenu) child).getPopupMenu());
		}
	}

	static void applyTheme(Component component)
	{
		if (component instanceof JButton && "itemSlot".equals(component.getName()))
			return;
		component.setFont(BODY_FONT);
		component.setForeground(TEXT);
		component.setBackground(BG);
		if ("exportCode".equals(component.getName()))
		{
			component.setFont(StudioTheme.CODE);
			component.setBackground(CONTROL);
		}
		if (component instanceof javax.swing.AbstractButton)
		{
			javax.swing.AbstractButton b = (javax.swing.AbstractButton) component;
			b.setBackground(CONTROL);
			if (!(b instanceof JCheckBox))
			{
				b.setFont(ACTION_FONT);
				b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE),
						BorderFactory.createEmptyBorder(5, 6, 5, 6)));
				b.setFocusPainted(true);
			}
		}
		if (component instanceof JButton && component.getName() != null && component.getName().startsWith("library"))
		{
			((JButton) component).setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE),
					BorderFactory.createEmptyBorder(2, 3, 2, 3)));
			if (component.getName().startsWith("libraryOpen:"))
				component.setFont(BODY_FONT);
		}
		if (component instanceof JButton && "supportCoffee".equals(component.getName()))
		{
			component.setFont(StudioTheme.SMALL);
			JButton coffee = (JButton) component;
			coffee.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
			int labelWidth = coffee.getFontMetrics(coffee.getFont()).stringWidth(coffee.getText())
					+ coffee.getInsets().left + coffee.getInsets().right + coffee.getIcon().getIconWidth()
					+ coffee.getIconTextGap();
			coffee.setPreferredSize(new Dimension(Math.max(94, labelWidth + 4), 28));
		}
		if (component instanceof JButton && ((JButton) component).getClientProperty("disclosure") != null)
		{
			JButton b = (JButton) component;
			b.setBackground(StudioTheme.SURFACE);
			b.setForeground(GOLD);
			b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, OUTLINE),
					BorderFactory.createEmptyBorder(6, 5, 6, 5)));
		}
		if (component instanceof JButton && component.getName() != null
				&& component.getName().startsWith("wikiPreset:"))
		{
			JButton b = (JButton) component;
			b.setBackground(StudioTheme.SELECTED);
			b.setForeground(GOLD);
			b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, GOLD),
					BorderFactory.createEmptyBorder(7, 8, 7, 8)));
		}
		if (component instanceof JButton && component.getName() != null
				&& component.getName().startsWith("setupToggle:"))
		{
			JButton b = (JButton) component;
			boolean selected = Boolean.TRUE.equals(b.getClientProperty("selected"));
			b.setBackground(selected ? StudioTheme.SELECTED : CONTROL);
			b.setForeground(selected ? GOLD : TEXT);
			b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(OUTLINE),
					BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		}
		if (component instanceof JCheckBox)
		{
			JCheckBox b = (JCheckBox) component;
			b.setIcon(StudioTheme.check(false));
			b.setSelectedIcon(StudioTheme.check(true));
			b.setDisabledIcon(StudioTheme.check(false));
			b.setDisabledSelectedIcon(StudioTheme.check(true));
			b.setBackground(BG);
		}
		if (component instanceof JTextField || component instanceof JComboBox || component instanceof JList)
			component.setBackground(CONTROL);
		if (component instanceof JTextField)
			((JTextField) component).setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(OUTLINE), BorderFactory.createEmptyBorder(4, 5, 4, 5)));
		if (component instanceof JScrollPane)
			((JScrollPane) component).setBorder(BorderFactory.createLineBorder(OUTLINE));
		if (component instanceof JComboBox
				&& !Boolean.TRUE.equals(((JComboBox<?>) component).getClientProperty("preserveRenderer")))
			((JComboBox<?>) component).setRenderer(plainNames());
		if (component instanceof JList)
		{
			((JList<?>) component).setCellRenderer(plainNames());
			((JList<?>) component).setSelectionBackground(StudioTheme.SELECTED);
			((JList<?>) component).setSelectionForeground(GOLD);
		}
		if (component instanceof JTextArea)
		{
			JTextArea a = (JTextArea) component;
			a.setForeground(a.isEditable() ? TEXT : MUTED);
			a.setBackground(a.isEditable() ? CONTROL : BG);
		}
		if ("exportCode".equals(component.getName()))
			component.setBackground(CONTROL);
		if (component instanceof JLabel && "sectionTitle".equals(component.getName()))
		{
			component.setForeground(GOLD);
			component.setFont(ACTION_FONT);
		}
		if (component instanceof JLabel && "studioTitle".equals(component.getName()))
		{
			component.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
			component.setForeground(TEXT);
		}
		if (component instanceof JLabel && "studioMark".equals(component.getName()))
			component.setForeground(GOLD);
		if (component instanceof JLabel && "studioSubtitle".equals(component.getName()))
		{
			component.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
			component.setForeground(GOLD);
		}
		if (component instanceof JLabel && "loadoutSectionTitle".equals(component.getName()))
		{
			component.setFont(StudioTheme.SECTION);
			component.setForeground(GOLD);
		}
		if (component instanceof JLabel && "gearReadiness".equals(component.getName()))
		{
			((JLabel) component).setOpaque(true);
			component.setBackground(StudioTheme.SURFACE);
			((JLabel) component).setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
		}
		if (component instanceof JButton && component.getName() != null
				&& component.getName().startsWith("libraryGroup:"))
		{
			JButton b = (JButton) component;
			b.setHorizontalAlignment(JButton.LEFT);
			b.setBackground(BG);
			b.setForeground(MUTED);
			b.setFont(StudioTheme.SECTION);
			b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, OUTLINE),
					BorderFactory.createEmptyBorder(8, 4, 6, 4)));
		}
		if (component instanceof JPanel && component.getName() != null && component.getName().startsWith("libraryRow:"))
		{
			component.setBackground(StudioTheme.SURFACE);
			((JPanel) component).setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 2, 0, 0, GOLD), BorderFactory.createEmptyBorder(2, 4, 2, 0)));
		}
		if (component instanceof JButton && component.getName() != null
				&& component.getName().startsWith("libraryOpen:"))
		{
			((JButton) component).setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 3));
			component.setBackground(StudioTheme.SURFACE);
		}
		if (component instanceof JButton && ("Save".equals(((JButton) component).getText())
				|| "Copy code".equals(((JButton) component).getText())))
		{
			component.setBackground(StudioTheme.SELECTED);
			component.setForeground(GOLD);
		}
		if (component instanceof JTextArea && "studioStatus".equals(component.getName()))
		{
			component.setFont(StudioTheme.SMALL);
			((JTextArea) component).setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(1, 0, 0, 0, OUTLINE), BorderFactory.createEmptyBorder(7, 2, 0, 2)));
		}
		if (component instanceof Container)
			for (Component child : ((Container) component).getComponents())
				applyTheme(child);
	}

	static javax.swing.DefaultListCellRenderer plainNames()
	{
		return new javax.swing.DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
					boolean focus)
			{
				putClientProperty("html.disable", true);
				Component rendered = super.getListCellRendererComponent(list, value, index, selected, focus);
				rendered.setFont(BODY_FONT);
				rendered.setBackground(selected ? StudioTheme.SELECTED : CONTROL);
				rendered.setForeground(selected ? GOLD : TEXT);
				return rendered;
			}
		};
	}

}
