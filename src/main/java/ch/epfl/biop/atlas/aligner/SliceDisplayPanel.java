package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.DisplaySettingsCommand;
import org.scijava.command.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimdata.util.Displaysettings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SliceDisplayPanel implements MultiSlicePositioner.ModeListener, MultiSlicePositioner.SliceChangeListener, ListSelectionListener {

    protected static Logger logger = LoggerFactory.getLogger(SliceDisplayPanel.class);

    final MultiSlicePositioner mp;

    final JTable table;

    final SliceDisplayTableModel model;

    int maxChannels = 0;

    final JPanel paneDisplay;

    public SliceDisplayPanel(MultiSlicePositioner mp) {
        this.mp = mp;

        paneDisplay = new JPanel(new BorderLayout());

        mp.addSliceListener(this);
        mp.addModeListener(this);

        model = new SliceDisplayTableModel();
        table = new JTable(model);
        table.setShowGrid( false );

        table.setModel( model );

        table.getSelectionModel().addListSelectionListener(this);

        table.setFillsViewportHeight(false);
        table.setDefaultRenderer(Displaysettings.class, new DisplaySettingsRenderer(true));
        table.setDefaultRenderer(Boolean.class, new VisibilityRenderer(true));

        table.setComponentPopupMenu(SliceSourcesPopupMenu.createFinalPopupMenu(mp));

        paneDisplay.add(new JLabel("Click table header to modify selected slices"), BorderLayout.NORTH);
        paneDisplay.add(new JScrollPane(table), BorderLayout.CENTER);
        //paneDisplay.add(panelDisplayOptions, BorderLayout.SOUTH);

        // listener
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<SliceSources> sortedSlices = mp.getSortedSlices();
                int col = table.columnAtPoint(e.getPoint());
                String name = table.getColumnName(col);
                logger.debug("Column index selected " + col + " " + name);

                if (col==1) {
                    // Let's gather whether most are visible or invisible
                    long nVisible = getSelectedIndices().stream()
                            .map(idx -> sortedSlices.get(idx).getGUIState())
                            .filter(SliceSourcesGUIState::isSliceVisible)
                            .count();
                    long nInvisible = getSelectedIndices().size()-nVisible;

                    if (nVisible<nInvisible) {
                        getSelectedIndices().stream()
                                .map(idx -> sortedSlices.get(idx).getGUIState())
                                .forEach(SliceSourcesGUIState::setSliceVisible);
                    } else {
                        getSelectedIndices().stream()
                                .map(idx -> sortedSlices.get(idx).getGUIState())
                                .forEach(SliceSourcesGUIState::setSliceInvisible);
                    }
                }
                if ((col>1)&&(col%2 == 0)) {

                    int iChannel = (col-2)/2;

                    long nVisible = getSelectedIndices().stream()
                            .filter(idx -> sortedSlices.get(idx).nChannels > iChannel)
                            .map(idx -> sortedSlices.get(idx).getGUIState())
                            .filter(guiState -> guiState.isChannelVisible(iChannel))
                            .count();

                    long nInvisible = getSelectedIndices().stream()
                            .filter(idx -> sortedSlices.get(idx).nChannels > iChannel)
                            .map(idx -> sortedSlices.get(idx).getGUIState())
                            .filter(guiState -> !guiState.isChannelVisible(iChannel))
                            .count();

                    if (nVisible<nInvisible) {
                        getSelectedIndices().stream()
                                .filter(idx -> sortedSlices.get(idx).nChannels > iChannel)
                                .map(idx -> sortedSlices.get(idx).getGUIState())
                                .forEach(guiState -> guiState.setChannelVisibility(iChannel, true));
                    } else {
                        getSelectedIndices().stream()
                                .filter(idx -> sortedSlices.get(idx).nChannels > iChannel)
                                .map(idx -> sortedSlices.get(idx).getGUIState())
                                .forEach(guiState -> guiState.setChannelVisibility(iChannel, false));
                    }

                }

                if ((col>1)&&(col%2 == 1)) {
                    int iChannel = (col-3)/2;

                    SourceAndConverter<?>[] sacs = getSelectedIndices().stream()
                            .map(idx -> sortedSlices.get(idx))
                            .filter(slice -> slice.nChannels > iChannel)
                            .map(slice -> slice.getGUIState().getCurrentSources()[iChannel])
                            .toArray(SourceAndConverter<?>[]::new);

                    if (sacs.length>0) {
                        Runnable update = () -> {
                            model.fireTableChanged(new TableModelEvent(model, 0, sortedSlices.size(), col,
                                    TableModelEvent.UPDATE));
                            //modelSelect.fireTableCellUpdated(row, col);
                        };
                        // ---- Just to have the correct parameters displayed (dirty hack)
                        Displaysettings ds_in = new Displaysettings(-1);
                        Displaysettings.GetDisplaySettingsFromCurrentConverter(sacs[0], ds_in);
                        DisplaySettingsCommand.IniValue = ds_in;
                        mp.scijavaCtx
                                .getService(CommandService.class)
                                .run(DisplaySettingsCommand.class, true,
                                        "sacs", sacs,
                                        "postrun", update);
                    } else {
                        mp.log.accept("Please select a slice with a valid channel in the tab.");
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
        //sortSlices();
        List<SliceSources> slices = mp.getSortedSlices();
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
            }

        } // else no change of number of channels

    }

    @Override
    public synchronized void sliceCreated(SliceSources slice) {
        //sortSlices();
        int index = mp.getSortedSlices().indexOf(slice);
        model.fireTableRowsInserted(index, index);
        if (slice.nChannels>maxChannels) {
            maxChannels = slice.nChannels;
            model.fireTableStructureChanged(); // All changed!
        }
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        //sortSlices();
        model.fireTableDataChanged();
    }

    @Override
    public void sliceVisibilityChanged(SliceSources slice) {
        int index = mp.getSortedSlices().indexOf(slice);
        model.fireTableRowsUpdated(index,index);
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        List<SliceSources> sortedSlices = mp.getSortedSlices();
        int idx = sortedSlices.indexOf(slice);
        //if (!sortedSlices.get(idx).isSelected()) {
        if (!table.getSelectionModel().isSelectedIndex(idx)){//sortedSlices.get(idx).isSelected()) {
            table.getSelectionModel().addSelectionInterval(idx, idx);
            table.repaint();
        }
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        List<SliceSources> sortedSlices = mp.getSortedSlices();
        int idx = sortedSlices.indexOf(slice);
        if (table.getSelectionModel().isSelectedIndex(idx)){//sortedSlices.get(idx).isSelected()) {
            table.getSelectionModel().removeSelectionInterval(idx, idx);
            table.repaint();
        }
    }

    int currentIndex = -1;

    @Override
    public void isCurrentSlice(SliceSources slice) {
        int oldIndex = currentIndex;
        currentIndex = -1;
        model.fireTableCellUpdated(oldIndex,0);
        int idx = mp.getSortedSlices().indexOf(slice);
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
        List<SliceSources> sortedSlices = mp.getSortedSlices();
        if (!lsm.isSelectionEmpty()) {
            for (int i = 0; i < sortedSlices.size(); i++) {
                if (lsm.isSelectedIndex(i)) {
                    currentSelection.add(i);
                    SliceSources slice = sortedSlices.get(i);
                    if (!slice.isSelected()) slice.select();
                } else {
                    SliceSources slice = sortedSlices.get(i);
                    if (slice.isSelected()) slice.deSelect();
                }
            }
        }

        mp.getBdvh().getViewerPanel().getDisplay().repaint(); // To update current selection state
        setCurrentlySelectedIndices(currentSelection);
    }

    /*public void sortSlices() {
        sortedSlices = mp.getSortedSlices();
    }*/

    @Override
    public void modeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {
    }

    @Override
    public void sliceDisplayModeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {
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
            return mp.getSlices().size();
        }

        @Override
        public int getColumnCount() {
            return maxChannels*2+2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex>mp.getSlices().size()-1) {
                if ((columnIndex == 0)) {
                    return "null";
                } else if ((columnIndex) == 1) {
                    return false;
                } else if (columnIndex%2 == 0) {
                    return Boolean.FALSE;
                } else {
                    return new Displaysettings(-1,"-");
                }
            }
            SliceSources slice =  mp.getSortedSlices().get(rowIndex);
            if ((columnIndex == 0)) {
                if (rowIndex == currentIndex) {
                    return "["+ rowIndex +"] - "+slice.getName();
                }
                return " "+ rowIndex+"  - "+slice.getName();
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
            SliceSources slice =  mp.getSortedSlices().get(rowIndex);
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

    public static class DisplaySettingsRenderer implements TableCellRenderer {
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered;
        JLabel label = new JLabel();

        public DisplaySettingsRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            label.setOpaque(true); //MUST do this for background to show up.
        }

        public Component getTableCellRendererComponent(
                JTable table, Object displaysettings,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Displaysettings ds = (Displaysettings) displaysettings;

            if (!ds.getName().equals("-")) {
                Color newColor = new Color(ds.color[0], ds.color[1], ds.color[2]);
                label.setBackground(newColor);
                label.setForeground(new Color( (ds.color[0]+128) % 256, (ds.color[1]+128) % 256, (ds.color[2]+128)%256));
                label.setText((int) ds.min + ":" + (int) ds.max);
                if (isBordered) {
                    if (isSelected) {
                        if (selectedBorder == null) {
                            selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                    table.getSelectionBackground());
                        }
                        label.setBorder(selectedBorder);
                    } else {
                        if (unselectedBorder == null) {
                            unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                    table.getBackground());
                        }
                        label.setBorder(unselectedBorder);
                    }
                }

                label.setToolTipText("RGB value: " + newColor.getRed() + ", "
                        + newColor.getGreen() + ", "
                        + newColor.getBlue());
            }
            return label;
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
