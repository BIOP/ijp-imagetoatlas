package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.DisplaySettingsCommand;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import spimdata.util.Displaysettings;
import spimdata.util.DisplaysettingsHelper;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AtlasDisplayPanel implements MultiSlicePositioner.ModeListener {//}, ListSelectionListener {

    final JPanel paneDisplay;

    final MultiSlicePositioner mp;

    final JTable tableAtlasDisplay;

    //final JTable tableSelectionControl;

    final AtlasDisplayTableModel model;

    //final SelectedSliceDisplayTableModel modelSelect;

    Consumer<String> log = (str) -> System.out.println(AtlasDisplayPanel.class+":"+str);

    //int maxChannels = 0;

    int nSlices = 0;

    boolean globalFlagVisible = true;

    List<Boolean> globalFlagPerChannel = new ArrayList<>();

    List<Displaysettings> globalDisplaySettingsPerChannel = new ArrayList<>();

    ReslicedAtlas ra;

    final int nChannels;

    public AtlasDisplayPanel(MultiSlicePositioner mp) {
        this.mp = mp;

        ra = mp.reslicedAtlas;
        nChannels = ra.nonExtendedSlicedSources.length;
        globalFlagPerChannel = new ArrayList<>(nChannels);
        for (int i=0;i<nChannels;i++) {
            globalFlagPerChannel.add(new Boolean(true));
        }
        paneDisplay = new JPanel(new BorderLayout());

        mp.addModeListener(this);

        model = new AtlasDisplayTableModel();
        tableAtlasDisplay = new JTable(model);
        tableAtlasDisplay.setShowGrid( false );

        tableAtlasDisplay.setModel( model );

        tableAtlasDisplay.setFillsViewportHeight(true);
        tableAtlasDisplay.setDefaultRenderer(Displaysettings.class, new SliceDisplayPanel.DisplaySettingsRenderer(true));
        tableAtlasDisplay.setDefaultRenderer(Boolean.class, new SliceDisplayPanel.VisibilityRenderer(true));

        /*tableSelectionControl.setFillsViewportHeight(true);
        tableSelectionControl.setShowHorizontalLines(true);
        tableSelectionControl.setDefaultRenderer(Displaysettings.class, new DisplaySettingsRenderer(true));
        tableSelectionControl.setDefaultRenderer(Boolean.class, new VisibilityRenderer(true));*/

        // table.setDefaultEditor(Displaysettings.class, new DisplaysettingsEditor());
        // tableSelectionControl.setDefaultEditor(Displaysettings.class, new DisplaysettingsEditor());

        JScrollPane scPane = new JScrollPane(tableAtlasDisplay);
        Dimension d = new Dimension(tableAtlasDisplay.getPreferredSize());

        //d.height*=2;
        tableAtlasDisplay.setPreferredScrollableViewportSize(d);//new Dimension(400,500));


        //paneDisplay.add(scPane, BorderLayout.NORTH);
        paneDisplay.add(scPane, BorderLayout.CENTER);

        tableAtlasDisplay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = tableAtlasDisplay.rowAtPoint(evt.getPoint());
                int col = tableAtlasDisplay.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    if ((col>1)&&(col%2 == 1)) {
                        int iChannel = (col-3)/2;

                        SourceAndConverter[] sacs = new SourceAndConverter[1];

                        if (nChannels>iChannel) {
                            sacs[0] = getSources()[iChannel];//sortedSlices.get(row).getGUIState().getCurrentSources()[iChannel];

                            Runnable update = () -> {model.fireTableCellUpdated(row, col);};

                            // ---- Just to have the correct parameters displayed (dirty hack)
                            Displaysettings ds_in = new Displaysettings(-1);
                            DisplaysettingsHelper.GetDisplaySettingsFromCurrentConverter(sacs[0], ds_in);
                            DisplaySettingsCommand.IniValue = ds_in;

                            mp.scijavaCtx
                                    .getService(CommandService.class)
                                    .run(DisplaySettingsCommand.class, true,
                                            "sacs", sacs,
                                            "postrun", update);
                        } else {
                            mp.log.accept("This slice has no channel indexed "+iChannel);
                        }
                    }
                }
            }
        });
    }

    public SourceAndConverter<?>[] getSources() {
        switch (mp.getDisplayMode()) {
            case MultiSlicePositioner.POSITIONING_MODE_INT:
                return ra.extendedSlicedSources;

            case MultiSlicePositioner.REGISTRATION_MODE_INT:
                return ra.nonExtendedSlicedSources;

            default:
                return null;
        }
    }

    public JPanel getPanel() {
        return paneDisplay;
    }

    int currentIndex = -1;

    @Override
    public void modeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {

        if (newmode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            SourceAndConverterUtils.transferColorConverters(ra.extendedSlicedSources, ra.nonExtendedSlicedSources);
        } else {
            SourceAndConverterUtils.transferColorConverters(ra.nonExtendedSlicedSources, ra.extendedSlicedSources);
        }

        for (SourceAndConverter sac : ra.extendedSlicedSources) {
            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }
        for (SourceAndConverter sac : ra.nonExtendedSlicedSources) {
            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }

        if (globalFlagVisible) {
            switch (newmode) {
                case MultiSlicePositioner.POSITIONING_MODE_INT:
                    for (int i = 0;i<nChannels;i++) {
                        SourceAndConverter sac = ra.extendedSlicedSources[i];
                        if (globalFlagPerChannel.get(i))
                            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, true);
                    }
                    break;
                case MultiSlicePositioner.REGISTRATION_MODE_INT:
                    for (int i = 0;i<nChannels;i++) {
                        SourceAndConverter sac = ra.nonExtendedSlicedSources[i];
                        if (globalFlagPerChannel.get(i))
                            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, true);
                    }
                    break;
                default:
                    ;
            }
        }

    }

    @Override
    public void sliceDisplayModeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {

    }

    class AtlasDisplayTableModel extends AbstractTableModel {

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
            return 1;
        }

        @Override
        public int getColumnCount() {
            return nChannels*2+2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            //SliceSources slice =  sortedSlices.get(rowIndex); // Not efficient
            if ((columnIndex == 0)) {
                if (rowIndex == currentIndex) {
                    return "["+new Integer(rowIndex).toString()+"]";
                }
                return " "+new Integer(rowIndex).toString();
            } else if ((columnIndex) == 1) {
                return globalFlagVisible;
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                if (nChannels>iChannel) {
                    return globalFlagPerChannel.get(iChannel);
                } else {
                    return new Boolean(false);
                }
            } else {
                int iChannel = (columnIndex-3)/2;
                if (nChannels>iChannel) {
                    SourceAndConverter sac = getSources()[iChannel];
                    Displaysettings ds = new Displaysettings(-1);
                    DisplaysettingsHelper.GetDisplaySettingsFromCurrentConverter(sac, ds);
                    return ds;
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
            if ((columnIndex) == 0) {
                // Nothing
            } else if ((columnIndex) == 1) {
                Boolean flag = (Boolean) aValue;

                //log.accept(" All sources atlas set "+flag);
                globalFlagVisible = flag;
                for (int i=0; i<getSources().length;i++) {
                    SourceAndConverter sac = getSources()[i];
                    if (globalFlagPerChannel.get(i)) {
                        mp.getBdvh().getViewerPanel().state().setSourceActive(sac, flag);
                    }
                }
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                SourceAndConverter<?> sac = getSources()[iChannel];
                if (nChannels>iChannel) {
                    Boolean flag = (Boolean) aValue;
                    globalFlagPerChannel.set(iChannel, flag);
                    mp.getBdvh().getViewerPanel().state().setSourceActive(sac, flag);
                }
            } else {
                // Done in the selection listener
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
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return col>0;
        }
    }

}
