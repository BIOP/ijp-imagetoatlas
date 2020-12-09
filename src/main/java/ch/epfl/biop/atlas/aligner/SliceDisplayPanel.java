package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.DisplaySettingsCommand;
import org.scijava.command.CommandService;
import spimdata.util.Displaysettings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SliceDisplayPanel implements MultiSlicePositioner.SliceChangeListener, ListSelectionListener {

    final JPanel paneDisplay;

    final MultiSlicePositioner mp;

    final JTable table;

    final JTable tableSelectionControl;

    final SliceDisplayTableModel model;

    final SelectedSliceDisplayTableModel modelSelect;

    int maxChannels = 0;

    int nSlices = 0;

    boolean globalFlagVisible = true;

    List<Boolean> globalFlagPerChannel = new ArrayList<>();

    List<Displaysettings> globalDisplaySettingsPerChannel = new ArrayList<>();

    List<SliceSources> sortedSlices = new ArrayList<>();

    public SliceDisplayPanel(MultiSlicePositioner mp) {
        this.mp = mp;
        paneDisplay = new JPanel(new BorderLayout());

        JButton toggleDisplayMode = new JButton("Multi/Single Slice");
        toggleDisplayMode.addActionListener(e -> mp.changeSliceDisplayMode());

        mp.addSliceListener(this);

        model = new SliceDisplayTableModel();
        modelSelect = new SelectedSliceDisplayTableModel();
        table = new JTable(model);
        tableSelectionControl = new JTable();

        tableSelectionControl.setModel( modelSelect );

        tableSelectionControl.setShowGrid(false);
        table.setShowGrid( false );

        table.setModel( model );

        table.getSelectionModel().addListSelectionListener(this);

        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Displaysettings.class, new DisplaySettingsRenderer(true));
        table.setDefaultRenderer(Boolean.class, new VisibilityRenderer(true));

        tableSelectionControl.setFillsViewportHeight(true);
        tableSelectionControl.setShowHorizontalLines(true);
        tableSelectionControl.setDefaultRenderer(Displaysettings.class, new DisplaySettingsRenderer(true));
        tableSelectionControl.setDefaultRenderer(Boolean.class, new VisibilityRenderer(true));

        JScrollPane scPane = new JScrollPane(tableSelectionControl);
        Dimension d = new Dimension(tableSelectionControl.getPreferredSize());

        tableSelectionControl.setPreferredScrollableViewportSize(d);

        paneDisplay.add(scPane, BorderLayout.NORTH);
        paneDisplay.add(table, BorderLayout.CENTER);
        paneDisplay.add(toggleDisplayMode, BorderLayout.SOUTH);

        tableSelectionControl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = tableSelectionControl.rowAtPoint(evt.getPoint());
                int col = tableSelectionControl.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    if (row==0) {
                        if ((col>1)&&(col%2 == 1)) {
                            int iChannel = (col-3)/2;

                            SourceAndConverter[] sacs = getSelectedIndices().stream()
                                    .map(idx -> sortedSlices.get(idx))
                                    .filter(slice -> slice.nChannels>iChannel)
                                    .map(slice -> slice.getGUIState().getCurrentSources()[iChannel])
                                    .collect(Collectors.toList()).toArray(new SourceAndConverter[0]);

                            if (sacs.length>0) {
                                Runnable update = () -> {
                                    model.fireTableChanged(new TableModelEvent(model, 0, nSlices, col,
                                            TableModelEvent.UPDATE));
                                    modelSelect.fireTableCellUpdated(row, col);};
                                mp.scijavaCtx
                                        .getService(CommandService.class)
                                        .run(DisplaySettingsCommand.class, true, "sacs", sacs, "postrun", update);
                            } else {
                                mp.log.accept("Please select a slice with a valid channel in the tab.");
                            }
                        }
                    }
                }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {

                    SliceSources[] slices = getSelectedIndices().stream()
                            .map(idx -> sortedSlices.get(idx))
                            .collect(Collectors.toList()).toArray(new SliceSources[0]);
                    JPopupMenu popup = new SliceSourcesPopupMenu(mp, slices).getPopup();
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
                super.mouseReleased(e);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    if ((col>1)&&(col%2 == 1)) {
                        int iChannel = (col-3)/2;

                        SourceAndConverter[] sacs = new SourceAndConverter[1];

                        if (sortedSlices.get(row).nChannels>iChannel) {
                            sacs[0] = sortedSlices.get(row).getGUIState().getCurrentSources()[iChannel];

                            Runnable update = () -> model.fireTableCellUpdated(row, col);

                            mp.scijavaCtx
                                    .getService(CommandService.class)
                                    .run(DisplaySettingsCommand.class, true, "sacs", sacs, "postrun", update);
                        } else {
                            mp.log.accept("This slice has no channel indexed "+iChannel);
                        }
                    }
                }
            }
        });
    }

    public JPanel getPanel() {
        return paneDisplay;
    }

    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        nSlices--;
        sortSlices();
        List<SliceSources> slices = sortedSlices;
        int index = slices.indexOf(slice);
        slices.remove(slice);
        model.fireTableRowsDeleted(index, index);

        // What happened to the number of channels ?
        if (slice.nChannels==maxChannels) {

            // Maybe it's the last one with this number of channels...
            int newMaxChannels;
            if (slices.size()==0) { // special case : hangs forever if the last slice is removed
                newMaxChannels = 0;
            } else {
                newMaxChannels = slices.stream()
                        .mapToInt(s -> s.nChannels).max().getAsInt();
            }

            if (newMaxChannels < maxChannels) {
                // The number of channels diminished... full update
                maxChannels = newMaxChannels;
                model.fireTableStructureChanged();

                globalFlagPerChannel.remove(globalFlagPerChannel.size()-1);
                globalDisplaySettingsPerChannel.remove(globalDisplaySettingsPerChannel.size()-1);

                modelSelect.fireTableStructureChanged();
            }

        } // else no change of number of channels

    }

    @Override
    public synchronized void sliceCreated(SliceSources slice) {
        nSlices++;
        sortSlices();
        int index = sortedSlices.indexOf(slice);
        model.fireTableRowsInserted(index, index);
        if (slice.nChannels>maxChannels) {
            for (int i=maxChannels;i<slice.nChannels;i++) {
                globalFlagPerChannel.add(Boolean.TRUE);
                globalDisplaySettingsPerChannel.add(new Displaysettings(-1));
            }
            maxChannels = slice.nChannels;
            model.fireTableStructureChanged(); // All changed!
            modelSelect.fireTableStructureChanged();
        }
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        sortSlices();
        model.fireTableDataChanged();
    }

    @Override
    public void sliceVisibilityChanged(SliceSources slice) {
        int index = sortedSlices.indexOf(slice);
        model.fireTableRowsUpdated(index,index);
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        int idx = sortedSlices.indexOf(slice);
        table.getSelectionModel().addSelectionInterval(idx, idx);
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        int idx = sortedSlices.indexOf(slice);
        table.getSelectionModel().removeSelectionInterval(idx, idx);
    }

    int currentIndex = -1;

    @Override
    public void isCurrentSlice(SliceSources slice) {
        int oldIndex = currentIndex;
        currentIndex = -1;
        model.fireTableCellUpdated(oldIndex,0);
        int idx = sortedSlices.indexOf(slice);
        currentIndex = idx;
        model.fireTableCellUpdated(idx,0);
    }

    List<Integer> currentlySelectedIndices = new ArrayList<>();

    synchronized List<Integer> getSelectedIndices() {
        return new ArrayList<>(currentlySelectedIndices);
    }

    synchronized void setCurrentlySelectedIndices(List<Integer> selectedIndices) {
        currentlySelectedIndices = new ArrayList<>(selectedIndices);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsm = (ListSelectionModel)e.getSource();

        List<Integer> currentSelection = new ArrayList<>();

        if (!lsm.isSelectionEmpty()) {
            for (int i = 0; i < sortedSlices.size(); i++) {
                if (lsm.isSelectedIndex(i)) {
                    currentSelection.add(i);
                    sortedSlices.get(i).select();
                } else {
                    sortedSlices.get(i).deSelect();
                }
            }
        }

        mp.getBdvh().getViewerPanel().getDisplay().repaint(); // To update current selection state
        setCurrentlySelectedIndices(currentSelection);
    }

    public void sortSlices() {
        sortedSlices = mp.getSortedSlices();
    }

    class SliceDisplayTableModel extends AbstractTableModel {

        public String getColumnName(int columnIndex) {
            if ((columnIndex) == 0) {
                return "#";
            } else if ((columnIndex) == 1) {
                return "Vis.";
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                return "Ch_"+iChannel;
            } else {
                return "";
            }
        }

        @Override
        public int getRowCount() {
            return nSlices;
        }

        @Override
        public int getColumnCount() {
            return maxChannels*2+2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SliceSources slice =  sortedSlices.get(rowIndex);
            if ((columnIndex == 0)) {
                if (rowIndex == currentIndex) {
                    return "["+ rowIndex +"]";
                }
                return " "+ rowIndex;
            } else if ((columnIndex) == 1) {
                return slice.getGUIState().isSliceVisible();
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                if (slice.nChannels>iChannel) {
                    return slice.getGUIState().channelVisible[iChannel];
                } else {
                    return Boolean.FALSE;
                }
            } else {
                int iChannel = (columnIndex-3)/2;
                if (slice.nChannels>iChannel) {
                    return slice.getGUIState().getDisplaysettings()[iChannel];
                } else {
                    return new Displaysettings(-1,"-");
                }
            }
        }

        /**
         *
         *  @param  aValue   value to assign to cell
         *  @param  rowIndex   row of cell
         *  @param  columnIndex  column of cell
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            SliceSources slice =  sortedSlices.get(rowIndex);
            if (columnIndex != 0) { // column zero used for selecting
                if ((columnIndex) == 1) {
                    Boolean flag = (Boolean) aValue;
                    if (flag) {
                        if (!slice.getGUIState().isSliceVisible())
                            slice.getGUIState().setSliceVisible();
                    } else {
                        if (slice.getGUIState().isSliceVisible()) {
                            slice.getGUIState().setSliceInvisible();
                        }
                    }
                } else if (columnIndex%2 == 0) {
                    int iChannel = (columnIndex-2)/2;
                    if (slice.nChannels>iChannel) {
                        Boolean flag = (Boolean) aValue;
                        if (slice.getGUIState().isChannelVisible(iChannel)) {
                            if (!flag) {
                                slice.getGUIState().setChannelVisibility(iChannel, false);
                            }
                        } else {
                            if (flag) {
                                slice.getGUIState().setChannelVisibility(iChannel, true);
                            }
                        }
                    } // else Channel not available for this slice
                } // else -> dealt in the mouse adapter
            }
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return String.class;
            if (columnIndex == 1) return Boolean.class;
            if (columnIndex%2 == 0) {
                return Boolean.class;
            } else {
                return Displaysettings.class;
            }
        }

        public boolean isCellEditable(int row, int col) {
            return col>0;
        }

    }

    class SelectedSliceDisplayTableModel extends SliceDisplayTableModel {

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 0) { // Column zero is not used
                if ((columnIndex) == 1) {
                    Boolean flag = (Boolean) aValue;
                    globalFlagVisible = flag;
                    getSelectedIndices().forEach(idx -> {
                        SliceSources cSlice = sortedSlices.get(idx);
                        if (flag) {
                            if (!cSlice.getGUIState().isSliceVisible())
                                cSlice.getGUIState().setSliceVisible();
                        } else {
                            if (cSlice.getGUIState().isSliceVisible()) {
                                cSlice.getGUIState().setSliceInvisible();
                            }
                        }
                    });
                } else if (columnIndex%2 == 0) {
                    int iChannel = (columnIndex-2)/2;
                    Boolean flag = (Boolean) aValue;
                    globalFlagPerChannel.set(iChannel, flag);
                    getSelectedIndices().forEach(idx -> {
                        SliceSources cSlice = sortedSlices.get(idx);
                        if (cSlice.nChannels>iChannel) {
                            if (cSlice.getGUIState().isChannelVisible(iChannel)) {
                                if (!flag) {
                                    cSlice.getGUIState().setChannelVisibility(iChannel, false);
                                }
                            } else {
                                if (flag) {
                                    cSlice.getGUIState().setChannelVisibility(iChannel, true);
                                }
                            }
                        } //else Channel not available for this slice
                    });
                } // else dealt with in the mouse manager
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if ((columnIndex) == 0) {
                return ".";
            } else if ((columnIndex) == 1) {
                return globalFlagVisible;
            } if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                if (sortedSlices.size()>0) {
                    return globalFlagPerChannel.get(iChannel);
                } else {
                    return Boolean.FALSE;
                }
            } else {
                int iChannel = (columnIndex-3)/2;
                if (sortedSlices.size()>0) {
                    return globalDisplaySettingsPerChannel.get(iChannel);
                } else {
                    return new Displaysettings(-1,"-");
                }
            }
        }

        @Override
        public int getRowCount() {
            return 1;
        }
    }

    public static class DisplaySettingsRenderer extends JLabel
            implements TableCellRenderer {
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered;

        public DisplaySettingsRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            setOpaque(true); //MUST do this for background to show up.
        }

        public Component getTableCellRendererComponent(
                JTable table, Object displaysettings,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Displaysettings ds = (Displaysettings) displaysettings;

            if (!ds.getName().equals("-")) {
                Color newColor = new Color(ds.color[0], ds.color[1], ds.color[2]);
                setBackground(newColor);
                setForeground(new Color( (ds.color[0]+128) % 256, (ds.color[1]+128) % 256, (ds.color[2]+128)%256));
                setText((int) ds.min + ":" + (int) ds.max);
                if (isBordered) {
                    if (isSelected) {
                        if (selectedBorder == null) {
                            selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                    table.getSelectionBackground());
                        }
                        setBorder(selectedBorder);
                    } else {
                        if (unselectedBorder == null) {
                            unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                    table.getBackground());
                        }
                        setBorder(unselectedBorder);
                    }
                }

                setToolTipText("RGB value: " + newColor.getRed() + ", "
                        + newColor.getGreen() + ", "
                        + newColor.getBlue());
            }
            return this;
        }
    }

    public static class VisibilityRenderer extends JLabel implements TableCellRenderer {

        static ImageIcon visibleIcon;
        static ImageIcon invisibleIcon;

        static {
            URL iconURL;
            iconURL = SliceDisplayPanel.class.getResource("/graphics/Visible.png");

            visibleIcon = new ImageIcon(iconURL);
            Image image = visibleIcon.getImage(); // transform it
            Image newimg = image.getScaledInstance(15, 15,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
            visibleIcon = new ImageIcon(newimg);  // transform it back


            iconURL = SliceDisplayPanel.class.getResource("/graphics/InvisibleL.png");
            invisibleIcon = new ImageIcon(iconURL);
            image = invisibleIcon.getImage(); // transform it
            newimg = image.getScaledInstance(15, 15,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
            invisibleIcon = new ImageIcon(newimg);  // transform it back
        }

        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered;

        public VisibilityRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            setOpaque(true); //MUST do this for background to show up.
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object v,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Boolean visible = (Boolean) v;

            if (isBordered) {
                if (isSelected) {
                    if (selectedBorder == null) {
                        selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                table.getSelectionBackground());
                    }
                    setBorder(selectedBorder);
                } else {
                    if (unselectedBorder == null) {
                        unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                table.getBackground());
                    }
                    setBorder(unselectedBorder);
                }
            }

            if (visible) {
                setIcon(visibleIcon);
            } else {
                setIcon(invisibleIcon);
            }
            return this;
        }
    }


}
