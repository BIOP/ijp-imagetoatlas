package ch.epfl.biop.atlas.aligner;

import spimdata.util.Displaysettings;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DisplayPanel implements MultiSlicePositioner.SliceChangeListener, ListSelectionListener {

    final JPanel paneDisplay;

    final MultiSlicePositioner mp;

    final JTable table;

    final JTable tableSelectionControl;

    final SliceDisplayTableModel model;

    final SelectedSliceDisplayTableModel modelSelect;

    Consumer<String> log = (str) -> System.out.println(DisplayPanel.class+":"+str);

    int maxChannels = 0;

    int nSlices = 0;

    int offsetX = 1, offsetY = 1;

    boolean globalFlagVisible = true;

    List<Boolean> globalFlagPerChannel = new ArrayList<>();

    List<Displaysettings> globalDisplaySettingsPerChannel = new ArrayList<>();

    public DisplayPanel(MultiSlicePositioner mp) {
        this.mp = mp;
        paneDisplay = new JPanel(new BorderLayout());

        JButton toggleDisplayMode = new JButton("Multi/Single Slice");
        toggleDisplayMode.addActionListener(e -> {
            mp.changeSliceDisplayMode();
        });

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
        //JPanel innerPanel = new JPanel();
        //innerPanel.add(tableSelectionControl);
        //JScrollPane innerScrollPane = new JScrollPane(table);
        //innerScrollPane.setColumnHeaderView(tableSelectionControl);
        //innerPanel.add(innerScrollPane);

        //JScrollPane scrollpane = new JScrollPane(innerScrollPane);//innerPanel);
        //scrollpane.setWheelScrollingEnabled(true);
        table.setFillsViewportHeight(true);
        tableSelectionControl.setFillsViewportHeight(true);
        tableSelectionControl.setShowHorizontalLines(true);
        JScrollPane scPane = new JScrollPane(tableSelectionControl);
        Dimension d = new Dimension(tableSelectionControl.getPreferredSize());

        //d.height*=2;
        tableSelectionControl.setPreferredScrollableViewportSize(d);//new Dimension(400,500));
        //tableSelectionControl.setFillsViewportHeight(true);

        paneDisplay.add(scPane, BorderLayout.NORTH);
        paneDisplay.add(table, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return paneDisplay;
    }

    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        //log.accept(slice+" deleted");
        nSlices--;
        sortSlices();
        List<SliceSources> slices = sortedSlices;//mp.getSortedSlices();
        int index = slices.indexOf(slice);
        slices.remove(slice);
        model.fireTableRowsDeleted(index, index); // Which thread ?

        // What happened to the number of channels ?
        if (slice.nChannels==maxChannels) {
            // Maybe it's the last one with this number of channels...
            int newMaxChannels = slices.stream()
                    .map(s -> s.nChannels)
                    .reduce(Math::max).get();
            if (newMaxChannels < maxChannels) {
                // The number of channels diminished... full update
                maxChannels = newMaxChannels;
                model.fireTableStructureChanged();

                globalFlagPerChannel.remove(globalFlagPerChannel.size()-1);
                globalDisplaySettingsPerChannel.remove(globalDisplaySettingsPerChannel.size()-1);
                modelSelect.fireTableStructureChanged();
            }

        } else {
            // No need to worry
        }
    }

    @Override
    public synchronized void sliceCreated(SliceSources slice) {
        //log.accept(slice+" created");
        nSlices++;
        sortSlices();
        int index = sortedSlices.indexOf(slice);
        model.fireTableRowsInserted(index, index); // Which thread ?
        if (slice.nChannels>maxChannels) {
            for (int i=maxChannels;i<slice.nChannels;i++) {
                globalFlagPerChannel.add(new Boolean(true));
                globalDisplaySettingsPerChannel.add(new Displaysettings(-1));
            }
            maxChannels = slice.nChannels;
            model.fireTableStructureChanged(); // All changed!
            modelSelect.fireTableStructureChanged();
        }
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        //log.accept(slice+" display changed");
        model.fireTableDataChanged();
    }

    @Override
    public void sliceVisibilityChanged(SliceSources slice) {
        int index = sortedSlices.indexOf(slice);
        model.fireTableRowsUpdated(index,index);
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

        if (lsm.isSelectionEmpty()) {
            //output.append(" <none>");
        } else {

            // Find out which indexes are selected.
            int minIndex = lsm.getMinSelectionIndex();
            int maxIndex = lsm.getMaxSelectionIndex();
            for (int i = minIndex; i <= maxIndex; i++) {
                if (lsm.isSelectedIndex(i)) {
                    currentSelection.add(i);
                }
            }
        }

        setCurrentlySelectedIndices(currentSelection);
    }

    List<SliceSources> sortedSlices = new ArrayList<>();

    public void sortSlices() {
        sortedSlices = mp.getSortedSlices();
    }

    class SliceDisplayTableModel extends AbstractTableModel {

        public String getColumnName(int columnIndex) {
            if ((columnIndex) == 0) {
                return "Vis.";
            }
            if (columnIndex%2 == 1) {
                int iChannel = (columnIndex-1)/2;
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
            return maxChannels*2+1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SliceSources slice =  sortedSlices.get(rowIndex); // Not efficient
            if ((columnIndex) == 0) {
                return new Boolean(slice.getGUIState().isSliceVisible());
            }
            if (columnIndex%2 == 1) {
                int iChannel = (columnIndex-1)/2;
                if (slice.nChannels>iChannel) {
                    return new Boolean(slice.getGUIState().channelVisible[iChannel]);
                } else {
                    return new Boolean(false);
                }
            } else {
                int iChannel = (columnIndex-2)/2;
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
            //System.out.println(rowIndex+":"+columnIndex);
            //System.out.println("V = "+aValue);
            //List<SliceSources> slices = mp.getSortedSlices();
            SliceSources slice =  sortedSlices.get(rowIndex); // Not efficient
            if ((columnIndex) == 0) {
                Boolean flag = (Boolean) aValue;
                //getSelectedIndices().forEach(idx -> {
                    //SliceSources cSlice = sortedSlices.get(idx);
                    if (flag) {
                        if (!slice.getGUIState().isSliceVisible())
                            slice.getGUIState().setSliceVisible();
                    } else {
                        if (slice.getGUIState().isSliceVisible()) {
                            slice.getGUIState().setSliceInvisible();
                        }
                    }
                //});
            } else
            if (columnIndex%2 == 1) {
                int iChannel = (columnIndex-1)/2;
                if (slice.nChannels>iChannel) {
                    Boolean flag = (Boolean) aValue;
                    //System.out.println("iChannel = "+iChannel+" flag = "+flag);
                    if (slice.getGUIState().isChannelVisible(iChannel)) {
                        if (!flag) {
                            //System.out.println("make it invisible");
                            slice.getGUIState().setChannelVisibility(iChannel, false);
                        }
                    } else {
                        if (flag) {
                            //System.out.println("make it visible");
                            slice.getGUIState().setChannelVisibility(iChannel, true);
                        }
                    }
                    //return new Boolean(slice.getGUIState().channelVisible[iChannel]);
                } else {
                    // Channel not available for this slice
                    //return new Boolean(false);
                }
            } else {
                int iChannel = (columnIndex-2)/2;
                if (slice.nChannels>iChannel) {
                    //return slice.getGUIState().getDisplaysettings()[iChannel];
                } else {
                    //return new Displaysettings(-1,"-");
                }
            }
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            if (columnIndex%2 == 1) {
                return Boolean.class;
            } else {
                return Displaysettings.class;
            }
        }

        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return true;
        }

    }

    class SelectedSliceDisplayTableModel extends SliceDisplayTableModel {

        /**
         *
         *  @param  aValue   value to assign to cell
         *  @param  rowIndex   row of cell
         *  @param  columnIndex  column of cell
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

            if ((columnIndex) == 0) {
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
            } else
            if (columnIndex%2 == 1) {
                int iChannel = (columnIndex-1)/2;
                Boolean flag = (Boolean) aValue;
                globalFlagPerChannel.set(iChannel, flag);
                getSelectedIndices().forEach(idx -> {
                    SliceSources cSlice = sortedSlices.get(idx);
                    if (cSlice.nChannels>iChannel) {
                        //System.out.println("iChannel = "+iChannel+" flag = "+flag);
                        if (cSlice.getGUIState().isChannelVisible(iChannel)) {
                            if (!flag) {
                                //System.out.println("make it invisible");
                                cSlice.getGUIState().setChannelVisibility(iChannel, false);
                            }
                        } else {
                            if (flag) {
                                //System.out.println("make it visible");
                                cSlice.getGUIState().setChannelVisibility(iChannel, true);
                            }
                        }
                        //return new Boolean(slice.getGUIState().channelVisible[iChannel]);
                    } else {
                        // Channel not available for this slice
                        //return new Boolean(false);
                    }
                });
            } else {
                int iChannel = (columnIndex-2)/2;
                /*if (cSlice.nChannels>iChannel) {
                    //return slice.getGUIState().getDisplaysettings()[iChannel];
                } else {
                    //return new Displaysettings(-1,"-");
                }*/
            }

        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if ((columnIndex) == 0) {
                return new Boolean(globalFlagVisible);
            }
            if (columnIndex%2 == 1) {
                int iChannel = (columnIndex-1)/2;
                if (sortedSlices.size()>0) {
                    return new Boolean(globalFlagPerChannel.get(iChannel));
                } else {
                    return new Boolean(false);
                }
            } else {
                int iChannel = (columnIndex-2)/2;
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

    // Copied from https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/ui/sourcetable/SourceTable.java

    // -- Process clicks on active and current checkboxes --
    // These clicks are consumed, because they should not cause selection changes, etc, in the table.

    /*private Point pressedAt;
    private boolean consumeNext = false;
    private long releasedWhen = 0;

    @Override
    protected void processMouseEvent( final MouseEvent e )
    {
        if ( e.getModifiers() == InputEvent.BUTTON1_MASK )
        {
            if ( e.getID() == MouseEvent.MOUSE_PRESSED )
            {
                final Point point = e.getPoint();
                pressedAt = point;
                final int vcol = columnAtPoint( point );
                final int vrow = rowAtPoint( point );
                if ( vcol >= 0 && vrow >= 0 )
                {
                    final int mcol = convertColumnIndexToModel( vcol );
                    switch ( mcol )
                    {
                        //case IS_ACTIVE_COLUMN:
                        //case IS_CURRENT_COLUMN:
                        //case COLOR_COLUMN:
                        default:
                            final int mrow = convertRowIndexToModel( vrow );
                            if ( isRowSelected( mrow ) )
                            {
                                e.consume();
                                consumeNext = true;
                            }
                    }
                }
            }
            else if ( e.getID() == MouseEvent.MOUSE_RELEASED )
            {
                if ( consumeNext )
                {
                    releasedWhen = e.getWhen();
                    consumeNext = false;
                    e.consume();
                }

                if ( pressedAt == null )
                    return;

                final Point point = e.getPoint();
                if ( point.distanceSq( pressedAt ) > 2 )
                    return;

                final int vcol = columnAtPoint( point );
                final int vrow = rowAtPoint( point );
                if ( vcol >= 0 && vrow >= 0 )
                {
                    final int mcol = convertColumnIndexToModel( vcol );
                    final int mrow = convertRowIndexToModel( vrow );
                    switch ( mcol )
                    {
                        //case IS_ACTIVE_COLUMN:
                        //case IS_CURRENT_COLUMN:
                        //case COLOR_COLUMN:
                        default:
                            //final SourceAndConverter< ? > source = model.getValueAt( mrow ).getSource();

                    }
                }
            }
            else if ( e.getID() == MouseEvent.MOUSE_CLICKED )
            {
                if ( e.getWhen() == releasedWhen )
                    e.consume();
            }
        }
        super.processMouseEvent( e );
    }

    @Override
    protected void processMouseMotionEvent( final MouseEvent e )
    {
        if ( consumeNext && e.getModifiers() == InputEvent.BUTTON1_MASK && e.getID() == MouseEvent.MOUSE_DRAGGED )
            e.consume();
        super.processMouseMotionEvent( e );
    }*/

}
