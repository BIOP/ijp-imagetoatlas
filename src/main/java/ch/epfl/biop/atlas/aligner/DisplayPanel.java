package ch.epfl.biop.atlas.aligner;

import com.sun.beans.editors.BooleanEditor;
import spimdata.util.Displaysettings;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.EventObject;
import java.util.List;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class DisplayPanel implements MultiSlicePositioner.SliceChangeListener, ListSelectionListener {

    final JPanel paneDisplay;

    final MultiSlicePositioner mp;

    final JTable table;

    final SliceDisplayTableModel model;

    Consumer<String> log = (str) -> System.out.println(DisplayPanel.class+":"+str);

    int maxChannels = 0;

    int nSlices = 0;

    public DisplayPanel(MultiSlicePositioner mp) {
        this.mp = mp;
        paneDisplay = new JPanel(new FlowLayout());

        JButton toggleDisplayMode = new JButton("Multi/Single Slice");
        toggleDisplayMode.addActionListener(e -> {
            mp.changeSliceDisplayMode();
        });

        //paneDisplay.add(toggleDisplayMode);
        mp.addSliceListener(this);

        model = new SliceDisplayTableModel();
        table = new JTable(model);

        table.setColumnSelectionAllowed(true);
        //table.setRowSelectionAllowed(true);



        table.getSelectionModel().addListSelectionListener(this);


        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setWheelScrollingEnabled(true);
        table.setFillsViewportHeight(true);

        paneDisplay.add(scrollpane);

        System.out.println("------------------"+table.getDefaultEditor(Boolean.class));

        //table.getDefaultEditor(Boolean.class)

        DefaultTableModel dft;
        /*model.getColumn(0)
             .getCellEditor()
             .addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                e.getSource();
            }

            @Override
            public void editingCanceled(ChangeEvent e) {

            }
        });*/

    }

    public JPanel getPanel() {
        return paneDisplay;
    }

    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        //log.accept(slice+" deleted");
        nSlices--;
        List<SliceSources> slices = mp.getSortedSlices();
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
            }

        } else {
            // No need to worry
        }
    }

    @Override
    public synchronized void sliceCreated(SliceSources slice) {
        log.accept(slice+" created");
        nSlices++;
        int index = mp.getSortedSlices().indexOf(slice);
        model.fireTableRowsInserted(index, index); // Which thread ?
        if (slice.nChannels>maxChannels) {
            maxChannels = slice.nChannels;
            model.fireTableStructureChanged(); // All changed!
        }
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        log.accept(slice+" display changed");
        model.fireTableDataChanged();
    }

    @Override
    public void sliceVisibilityChanged(SliceSources slice) {
        int index = mp.getSortedSlices().indexOf(slice);
        model.fireTableRowsUpdated(index,index);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        log.accept("Selection Changed!");
    }


    class SliceDisplayTableModel extends AbstractTableModel {

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
            SliceSources slice =  mp.getSortedSlices().get(rowIndex); // Not efficient
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
            System.out.println(rowIndex+":"+columnIndex);
            System.out.println("V = "+aValue);
            SliceSources slice =  mp.getSortedSlices().get(rowIndex); // Not efficient
            if ((columnIndex) == 0) {
                Boolean flag = (Boolean) aValue;
                if (flag) {
                    if (!slice.getGUIState().isSliceVisible())
                        slice.getGUIState().setSliceVisible();
                } else {
                    if (slice.getGUIState().isSliceVisible()) {
                        slice.getGUIState().setSliceInvisible();
                    }
                }
            } else
            if (columnIndex%2 == 1) {
                int iChannel = (columnIndex-1)/2;
                if (slice.nChannels>iChannel) {
                    Boolean flag = (Boolean) aValue;
                    System.out.println("iChannel = "+iChannel+" flag = "+flag);
                    if (slice.getGUIState().isChannelVisible(iChannel)) {
                        if (!flag) {
                            System.out.println("make it invisible");
                            slice.getGUIState().setChannelVisibility(iChannel, false);
                        }
                    } else {
                        if (flag) {
                            System.out.println("make it visible");
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

}
