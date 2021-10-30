package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.CancelableAction;
import ch.epfl.biop.atlas.aligner.command.DisplaySettingsCommand;
import ch.epfl.biop.atlas.aligner.gui.SliceSourcesPopupMenu;
import org.apache.commons.lang.ArrayUtils;
import org.scijava.command.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimdata.util.Displaysettings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class JTableView implements MultiSlicePositioner.SliceChangeListener, ListSelectionListener {

    int maxChannels = 0;

    final JPanel paneDisplay;

    protected static Logger logger = LoggerFactory.getLogger(JTableView.class);

    final JTable table;

    final SliceDisplayTableModel model;

    final BdvMultislicePositionerView view;

    final MultiSlicePositioner mp;

    public JTableView(BdvMultislicePositionerView view) {
        paneDisplay = new JPanel(new BorderLayout());
        this.view = view;
        this.mp = view.msp;
        model = new SliceDisplayTableModel();
        table = new JTable(model);
        table.setShowGrid( false );

        table.setModel( model );

        table.getSelectionModel().addListSelectionListener(this);

        table.setFillsViewportHeight(false);
        table.setDefaultRenderer(Displaysettings.class, new DisplaySettingsRenderer(true));
        table.setDefaultRenderer(Boolean.class, new VisibilityRenderer(true));

        table.setComponentPopupMenu(SliceSourcesPopupMenu.createFinalPopupMenu(view.msp, view));

        paneDisplay.add(new JLabel("Click table header to modify selected slices"), BorderLayout.NORTH);
        paneDisplay.add(new JScrollPane(table), BorderLayout.CENTER);
        //paneDisplay.add(panelDisplayOptions, BorderLayout.SOUTH);

        // listener
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<SliceSources> sortedSlices = mp.getSlices();
                int col = table.columnAtPoint(e.getPoint());
                String name = table.getColumnName(col);
                logger.debug("Column index selected " + col + " " + name);

                if (col==1) {
                    // Let's gather whether most are visible or invisible
                    /*long nVisible = getSelectedIndices().stream()
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
                    }*/
                }
                if ((col>1)&&(col%2 == 0)) {

                    int iChannel = (col-2)/2;

                    /*long nVisible = getSelectedIndices().stream() TODO
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
                    }*/

                }

                if ((col>1)&&(col%2 == 1)) {
                    int iChannel = (col-3)/2;

                    /*SourceAndConverter<?>[] sacs_gui = getSelectedIndices().stream() TODO
                            .map(sortedSlices::get)
                            .filter(slice -> slice.nChannels > iChannel)
                            .map(slice -> slice.getGUIState().getCurrentSources()[iChannel])
                            .toArray(SourceAndConverter<?>[]::new);

                    SourceAndConverter<?>[] sacs_original = getSelectedIndices().stream()
                            .map(sortedSlices::get)
                            .filter(slice -> slice.nChannels > iChannel)
                            .map(slice -> slice.getRegisteredSources()[iChannel])
                            .toArray(SourceAndConverter<?>[]::new);

                    SourceAndConverter<?>[] sacs = (SourceAndConverter<?>[]) ArrayUtils.addAll(sacs_gui,sacs_original);

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
                        mp.getContext()
                                .getService(CommandService.class)
                                .run(DisplaySettingsCommand.class, true,
                                        "sacs", sacs,
                                        "postrun", update);
                    } else {
                        mp.log.accept("Please select a slice with a valid channel in the tab.");
                    }*/
                }
            }
        });
    }

    int currentIndex = -1;

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
        List<SliceSources> sortedSlices = mp.getSlices();
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

        //mp.getBdvh().getViewerPanel().getDisplay().repaint(); // To update current selection state
        setCurrentlySelectedIndices(currentSelection);
    }

    public JComponent getPanel() {
        return paneDisplay;
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
            SliceSources slice =  mp.getSlices().get(rowIndex);
            if ((columnIndex == 0)) {
                if (rowIndex == currentIndex) {
                    return "["+ rowIndex +"] - "+slice.getName();
                }
                return " "+ rowIndex+"  - "+slice.getName();
            } else if ((columnIndex) == 1) {
                return true;//view.slice.getGUIState().isSliceVisible();
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                if (slice.nChannels>iChannel) {
                    //assert slice.getGUIState().getChannelsVisibility() != null;
                    return view.getChannelVisibility(slice, iChannel);//slice.getGUIState().getChannelsVisibility()[iChannel];
                } else {
                    return Boolean.FALSE;
                }
            } else {
                int iChannel = (columnIndex-3)/2;
                if (slice.nChannels>iChannel) {
                    return view.getDisplaySettings(slice, iChannel);//slice.getGUIState().getDisplaysettings()[iChannel];
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
            SliceSources slice =  mp.getSlices().get(rowIndex);
            if (columnIndex != 0) { // column zero used for selecting
                if ((columnIndex) == 1) {
                    Boolean flag = (Boolean) aValue;
                    /*if (flag) { TODO
                        if (!slice.getGUIState().isSliceVisible())
                            slice.getGUIState().setSliceVisible();
                    } else {
                        if (slice.getGUIState().isSliceVisible()) {
                            slice.getGUIState().setSliceInvisible();
                        }
                    }*/
                } else if (columnIndex%2 == 0) {
                    int iChannel = (columnIndex-2)/2;
                    if (slice.nChannels>iChannel) {
                        /* Boolean flag = (Boolean) aValue; TODO
                        if (slice.getGUIState().isChannelVisible(iChannel)) {
                            if (!flag) {
                                slice.getGUIState().setChannelVisibility(iChannel, false);
                            }
                        } else {
                            if (flag) {
                                slice.getGUIState().setChannelVisibility(iChannel, true);
                            }
                        }*/
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


    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        //sortSlices();
        List<SliceSources> slices = mp.getSlices();
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
    public void sliceCreated(SliceSources slice) {
        //sortSlices();
        int index = mp.getSlices().indexOf(slice);
        model.fireTableRowsInserted(index, index);
        if (slice.nChannels>maxChannels) {
            maxChannels = slice.nChannels;
            model.fireTableStructureChanged(); // All changed!
        }
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        model.fireTableDataChanged();
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        List<SliceSources> sortedSlices = mp.getSlices();
        int idx = sortedSlices.indexOf(slice);
        //if (!sortedSlices.get(idx).isSelected()) {
        if (!table.getSelectionModel().isSelectedIndex(idx)){//sortedSlices.get(idx).isSelected()) {
            table.getSelectionModel().addSelectionInterval(idx, idx);
            table.repaint();
        }
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        List<SliceSources> sortedSlices = mp.getSlices();
        int idx = sortedSlices.indexOf(slice);
        if (table.getSelectionModel().isSelectedIndex(idx)){//sortedSlices.get(idx).isSelected()) {
            table.getSelectionModel().removeSelectionInterval(idx, idx);
            table.repaint();
        }

    }

    @Override
    public void sliceSourcesChanged(SliceSources slice) {

    }

    @Override
    public void slicePretransformChanged(SliceSources slice) {

    }

    @Override
    public void sliceKeyOn(SliceSources slice) {

    }

    @Override
    public void sliceKeyOff(SliceSources slice) {

    }

    @Override
    public void roiChanged() {

    }

    @Override
    public void actionEnqueue(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {

    }

    @Override
    public void actionCancelEnqueue(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionCancelStarted(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result) {

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
            iconURL = JTableView.class.getResource("/graphics/Visible.png");

            visibleIcon = new ImageIcon(iconURL);
            Image image = visibleIcon.getImage(); // transform it
            Image newimg = image.getScaledInstance(15, 15,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
            visibleIcon = new ImageIcon(newimg);  // transform it back


            iconURL = JTableView.class.getResource("/graphics/InvisibleL.png");
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
