package ch.epfl.biop.atlas.aligner.gui.bdv;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.command.DisplaySettingsCommand;
import ch.epfl.biop.atlas.aligner.gui.SliceSourcesPopupMenu;
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
import java.util.function.Consumer;

public class TableView implements MultiSlicePositioner.SliceChangeListener, ListSelectionListener {

    int maxChannels = 0;

    final JPanel paneDisplay;

    protected static Logger logger = LoggerFactory.getLogger(TableView.class);

    final JTable table;

    final SliceDisplayTableModel model;

    final BdvMultislicePositionerView view;

    final MultiSlicePositioner mp;

    List<SliceSources> listCopy = new ArrayList<>();
    final Object slicesModifyLock = new Object();

    public TableView(BdvMultislicePositionerView view) {
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
                int col = table.columnAtPoint(e.getPoint());
                String name = table.getColumnName(col);
                logger.debug("Column index selected " + col + " " + name);

                if (col==1) {
                    int[] selectedRows = table.getSelectedRows();

                    int nVisible = 0;
                    int nInvisible = 0;
                    for (int selectedRow : selectedRows) {
                        if ((Boolean) table.getValueAt(selectedRow, col)) {
                            nVisible++;
                        } else {
                            nInvisible++;
                        }
                    }

                    boolean newState = nVisible<nInvisible;
                    for (int selectedRow : selectedRows) {
                        view.guiState.runSlice(getSlices().get(selectedRow),
                                sliceGuiState -> sliceGuiState.setSliceVisibility(newState));
                    }
                }
                if ((col>1)&&(col%2 == 0)) {

                    int iChannel = (col-2)/2;
                    int[] selectedRows = table.getSelectedRows();

                    int nVisible = 0;
                    int nInvisible = 0;
                    for (int row : selectedRows) {
                        if ((Boolean) table.getValueAt(row, col)) {
                            nVisible++;
                        } else {
                            nInvisible++;
                        }
                    }

                    boolean newState = nVisible<nInvisible;
                    for (int selectedRow : selectedRows) {
                        view.guiState.runSlice(getSlices().get(selectedRow),
                                sliceGuiState -> sliceGuiState.setChannelVisibility(iChannel, newState));
                    }

                }
                if ((col>1)&&(col%2 == 1)) {
                    int iChannel = (col - 3) / 2;
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length > 0) {
                        int firstSelectedRow = table.getSelectedRows()[0];
                        Displaysettings ds_in = (Displaysettings) table.getValueAt(firstSelectedRow, col);
                        Consumer<Displaysettings> update = (displaySettings) -> {
                            for (int selectedRow: selectedRows) {
                                view.guiState.runSlice(getSlices().get(selectedRow),
                                        sliceGuiState -> sliceGuiState.setDisplaySettings(iChannel, displaySettings));
                            }
                            model.fireTableChanged(new TableModelEvent(model, 0, getSlices().size(), col,
                                    TableModelEvent.UPDATE));
                        };

                        // ---- Just to have the correct parameters displayed
                        DisplaySettingsCommand.IniValue = ds_in;
                        mp.getContext()
                                .getService(CommandService.class)
                                .run(DisplaySettingsCommand.class, true,
                                        "postrun", update);
                    } else {
                        mp.log.accept("Please select a slice with a valid channel in the tab.");
                    }
                }
            }
        });
    }

    int currentIndex = -1;

    @Override
    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (!lsm.isSelectionEmpty()) {
            for (SliceSources slice:getSlices()) {
                int i = slice.getIndex();
                if (lsm.isSelectedIndex(i)) {
                    if (!slice.isSelected()) slice.select();
                } else {
                    if (slice.isSelected()) slice.deSelect();
                }
            }
        }
    }

    public JComponent getPanel() {
        return paneDisplay;
    }

    public void cleanup() {
        listCopy.clear(); // Avoid memory leak... what a pain these swing components!
    }

    public void updateTable() {
        model.fireTableStructureChanged(); // All changed!
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
            return getSlices().size();
        }

        @Override
        public int getColumnCount() {
            return maxChannels*2+2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex>getSlices().size()-1) {
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
            SliceSources slice =  getSlices().get(rowIndex);
            if ((columnIndex == 0)) {
                if (rowIndex == currentIndex) {
                    // TODO : not implemented, currentIndex is always -1
                    if (slice.isKeySlice()) {
                        return "[" + rowIndex + "] " + slice.getName() + " (Key)";
                    } else {
                        return " " + rowIndex + "  " + slice.getName();
                    }
                }
                if (slice.isKeySlice()) {
                    return "["+ rowIndex+"] "+slice.getName()+" (Key)";
                } else {
                    return " "+ rowIndex+"  "+slice.getName();
                }
            } else if ((columnIndex) == 1) {
                return view.getSliceVisibility(slice);
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                return view.getChannelVisibility(slice, iChannel);
            } else {
                int iChannel = (columnIndex-3)/2;
                if (slice.nChannels>iChannel) {
                    return view.getDisplaySettings(slice, iChannel);
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
            SliceSources slice =  getSlices().get(rowIndex);
            if (columnIndex != 0) { // column zero used for selecting
                if ((columnIndex) == 1) {
                    view.guiState.runSlice(slice,
                            sliceGuiState -> sliceGuiState.setSliceVisibility(!sliceGuiState.getSliceVisibility()));
                } else if (columnIndex%2 == 0) {
                    int iChannel = (columnIndex-2)/2;
                    view.guiState.runSlice(slice,
                            sliceGuiState -> sliceGuiState.setChannelVisibility(iChannel, !sliceGuiState.getChannelVisibility(iChannel)));
                }
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

    synchronized List<SliceSources> getSlices() {
        return listCopy;
    }

    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        synchronized (slicesModifyLock) {
            listCopy = mp.getSlices();
            int index = slice.getIndex();
            model.fireTableRowsDeleted(index, index);

            // What happens to the number of channels ?

            List<SliceSources> slices = getSlices();
            if (slice.nChannels == maxChannels) {

                // Maybe it's the last one with this number of channels...
                int newMaxChannels;
                if (slices.size() == 0) { // special case : hangs forever if the last slice is removed
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
    }

    @Override
    public void sliceCreated(SliceSources slice) {
        synchronized (slicesModifyLock) {
            listCopy = mp.getSlices();
            int index = slice.getIndex();
            model.fireTableRowsInserted(index, index);
            if (slice.nChannels>maxChannels) {
                maxChannels = slice.nChannels;
                model.fireTableStructureChanged(); // All changed!
            }
        }
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        //model.fireTableStructureChanged(); // All changed! TODO : improve!!
        synchronized (slicesModifyLock) { // new order
            listCopy = mp.getSlices();
        }
        model.fireTableDataChanged();
        // TODO Need to update which rows are selected ...
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        int idx = slice.getIndex();
        if (!table.getSelectionModel().isSelectedIndex(idx)){
            table.getSelectionModel().addSelectionInterval(idx, idx);
            table.repaint();
        }
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        int idx = slice.getIndex();
        if (table.getSelectionModel().isSelectedIndex(idx)) {
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
        int idx = slice.getIndex();
        ((AbstractTableModel)table.getModel()).fireTableCellUpdated(idx, 0);
    }

    @Override
    public void sliceKeyOff(SliceSources slice) {
        int idx = slice.getIndex();
        ((AbstractTableModel)table.getModel()).fireTableCellUpdated(idx, 0);
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
            iconURL = TableView.class.getResource("/graphics/Visible.png");

            visibleIcon = new ImageIcon(iconURL);
            Image image = visibleIcon.getImage(); // transform it
            Image newimg = image.getScaledInstance(15, 15,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
            visibleIcon = new ImageIcon(newimg);  // transform it back


            iconURL = TableView.class.getResource("/graphics/InvisibleL.png");
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
