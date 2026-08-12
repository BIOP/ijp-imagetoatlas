package ch.epfl.biop.atlas.aligner.gui.bdv;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.command.DisplaySettingsCommand;
import ch.epfl.biop.atlas.aligner.gui.SliceSourcesPopupMenu;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.SliceInformationPanel;
import org.scijava.command.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimdata.util.Displaysettings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

    protected static final Logger logger = LoggerFactory.getLogger(TableView.class);

    final JTable table;

    final SliceDisplayTableModel model;

    final BdvMultislicePositionerView view;

    final MultiSlicePositioner mp;

    /**
     * Slices currently displayed in the table, in row order: this is the list of slices
     * of the {@link MultiSlicePositioner}, restricted to the ones matching {@link #nameFilter}.
     * Always reassigned to a fresh list, never modified in place, so that readers always
     * see a consistent snapshot.
     */
    volatile List<SliceSources> listCopy = new ArrayList<>();
    final Object slicesModifyLock = new Object();

    /** Lower case name filter, empty means 'show everything'. */
    volatile String nameFilter = "";

    /** Guards against the selection rebuild re-entering {@link #valueChanged(ListSelectionEvent)}. */
    boolean updatingSelection = false;

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

        JTextField filterField = new JTextField();
        filterField.setToolTipText("Only shows the slices whose name contains this text (case insensitive). Leave empty to show all slices.");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setNameFilter(filterField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setNameFilter(filterField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setNameFilter(filterField.getText());
            }
        });

        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.add(new JLabel("Filter by name "), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(new JLabel("Click table header to modify selected slices"), BorderLayout.NORTH);
        southPanel.add(filterPanel, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new SliceInformationPanel(view).getPanel(), BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        paneDisplay.add(panel, BorderLayout.NORTH);
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
                        mp.infoMessageForUser.accept("Table issue", "Please select a slice with a valid channel in the tab.");
                    }
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();
                int row = table.rowAtPoint(point);
                int col = table.columnAtPoint(point);
                if ((e.getClickCount()==2) && (col == 0)) {
                    // Is there a slice which is being double-clicked ?
                    if (row<getSlices().size()) {
                        SliceSources slice =  getSlices().get(row);
                        view.navigateSlice(slice);
                    }
                }
            }
        });
    }

    int currentIndex = -1;

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (updatingSelection) return; // the table selection is being rebuilt, not edited by the user
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (!lsm.isSelectionEmpty()) {
            // Only the filtered in slices are affected: a slice which is not in the table
            // can't be selected (see setNameFilter), and must not be deselected here either.
            List<SliceSources> slices = getSlices();
            for (int row = 0; row < slices.size(); row++) {
                SliceSources slice = slices.get(row);
                if (lsm.isSelectedIndex(row)) {
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
        listCopy = new ArrayList<>(); // Avoid memory leak... what a pain these swing components!
    }

    public void updateTable() {
        model.fireTableStructureChanged(); // All changed!
    }

    public void sliceDisplaySettingsChanged(SliceSources slice) {
        int idx = rowOf(slice);
        if (idx!=-1) {
            ((AbstractTableModel) table.getModel()).fireTableRowsUpdated(idx, idx);
        }
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
                // The slice index is displayed rather than the row index: they differ
                // as soon as a name filter hides some of the slices
                int sliceIndex = slice.getIndex();
                if (rowIndex == currentIndex) {
                    // TODO : not implemented, currentIndex is always -1
                    if (slice.isKeySlice()) {
                        return "[" + sliceIndex + "] " + slice.getName() + " (Key)";
                    } else {
                        return " " + sliceIndex + "  " + slice.getName();
                    }
                }
                if (slice.isKeySlice()) {
                    return "["+ sliceIndex+"] "+slice.getName()+" (Key)";
                } else {
                    return " "+ sliceIndex+"  "+slice.getName();
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

    /**
     * @return the slices displayed in the table, in row order. Because of the name filter,
     * this can be a subset of the slices of the {@link MultiSlicePositioner}.
     */
    List<SliceSources> getSlices() {
        return listCopy;
    }

    /**
     * @return the row displaying this slice, or -1 if the slice is currently filtered out.
     * This is NOT {@link SliceSources#getIndex()}, which ignores the filter.
     */
    int rowOf(SliceSources slice) {
        return getSlices().indexOf(slice);
    }

    /**
     * @return the slices matching the current name filter, keeping their original order
     */
    private List<SliceSources> applyFilter(List<SliceSources> slices) {
        String filter = nameFilter; // single read of the volatile field
        if (filter.isEmpty()) return slices;
        List<SliceSources> filtered = new ArrayList<>();
        for (SliceSources slice : slices) {
            String name = slice.getName();
            if ((name != null) && name.toLowerCase().contains(filter)) {
                filtered.add(slice);
            }
        }
        return filtered;
    }

    /**
     * Called on the EDT when the user edits the filter field.
     */
    private void setNameFilter(String text) {
        String newFilter = (text == null) ? "" : text.trim().toLowerCase();
        if (newFilter.equals(nameFilter)) return;
        nameFilter = newFilter;

        List<SliceSources> hidden;
        synchronized (slicesModifyLock) {
            List<SliceSources> allSlices = mp.getSlices();
            listCopy = applyFilter(allSlices);
            hidden = new ArrayList<>(allSlices);
            hidden.removeAll(getSlices());
        }

        // A slice which is not displayed anymore must not stay selected: otherwise an action
        // performed on the selected slices would silently affect slices invisible to the user.
        for (SliceSources slice : hidden) {
            if (slice.isSelected()) slice.deSelect();
        }

        model.fireTableDataChanged(); // this clears the table selection
        restoreSelection();
    }

    /**
     * Re-syncs the table selection with the selection state of the slices. Needed because
     * {@link AbstractTableModel#fireTableDataChanged()} clears the table selection.
     */
    private void restoreSelection() {
        updatingSelection = true;
        try {
            ListSelectionModel lsm = table.getSelectionModel();
            lsm.setValueIsAdjusting(true);
            lsm.clearSelection();
            List<SliceSources> slices = getSlices();
            for (int row = 0; row < slices.size(); row++) {
                if (slices.get(row).isSelected()) {
                    lsm.addSelectionInterval(row, row);
                }
            }
            lsm.setValueIsAdjusting(false);
        } finally {
            updatingSelection = false;
        }
        table.repaint();
    }

    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        synchronized (slicesModifyLock) {
            // The row has to be looked up before the list is updated, since the slice is
            // already gone from the MultiSlicePositioner when this listener is called
            int index = rowOf(slice);
            List<SliceSources> slices = mp.getSlices();
            listCopy = applyFilter(slices);
            if (index!=-1) {
                model.fireTableRowsDeleted(index, index);
            }

            // What happens to the number of channels ?
            // Note : the filter is ignored here, the columns should not depend on it

            if (slice.nChannels == maxChannels) {

                // Maybe it's the last one with this number of channels...
                int newMaxChannels;
                if (slices.isEmpty()) { // special case : hangs forever if the last slice is removed
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
            listCopy = applyFilter(mp.getSlices());
            int index = rowOf(slice); // -1 if the new slice does not match the filter
            if (index!=-1) {
                model.fireTableRowsInserted(index, index);
            }
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
            listCopy = applyFilter(mp.getSlices());
        }
        model.fireTableDataChanged();
        restoreSelection(); // fireTableDataChanged cleared the selection, put it back
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        int idx = rowOf(slice);
        if (idx!=-1) {
            if (!table.getSelectionModel().isSelectedIndex(idx)) {
                table.getSelectionModel().addSelectionInterval(idx, idx);
                table.repaint();
            }
        }
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        int idx = rowOf(slice);
        if (idx!=-1) {
            if (table.getSelectionModel().isSelectedIndex(idx)) {
                table.getSelectionModel().removeSelectionInterval(idx, idx);
                table.repaint();
            }
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
        int idx = rowOf(slice);
        if (idx!=-1) {
            ((AbstractTableModel) table.getModel()).fireTableCellUpdated(idx, 0);
        }
    }

    @Override
    public void sliceKeyOff(SliceSources slice) {
        int idx = rowOf(slice);
        if (idx!=-1) {
            ((AbstractTableModel) table.getModel()).fireTableCellUpdated(idx, 0);
        }
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

    @Override
    public void converterChanged(SliceSources slice) {
        int idx = rowOf(slice);
        if (idx!=-1)
            ((AbstractTableModel)table.getModel()).fireTableCellUpdated(idx, 0);
    }

    public static class DisplaySettingsRenderer implements TableCellRenderer {
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered;
        final JLabel label = new JLabel();

        public DisplaySettingsRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            label.setOpaque(true); //MUST do this for background to show up.
            label.setHorizontalAlignment(SwingConstants.CENTER);
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
            this.setHorizontalAlignment(SwingConstants.CENTER);
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
