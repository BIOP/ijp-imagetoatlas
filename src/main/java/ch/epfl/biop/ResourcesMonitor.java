package ch.epfl.biop;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import com.sun.management.OperatingSystemMXBean;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;

public class ResourcesMonitor extends JPanel {

    final JLabel cpuLabelJava;
    final JProgressBar cpuBarJava;

    final JLabel cpuLabelSystem;
    final JProgressBar cpuBarSystem;

    final JLabel memLabel;
    final JProgressBar memBar;

    final JButton gcButton;
    //final JButton clearActionStack;

    public ResourcesMonitor(){//MultiSlicePositioner mp) {
        this.setLayout(new GridLayout(8,1));

        cpuLabelSystem = new JLabel("CPU Usage - System (%)");
        cpuBarSystem = new JProgressBar();

        cpuLabelJava = new JLabel("CPU Usage - Java (%)");
        cpuBarJava = new JProgressBar();

        memLabel = new JLabel("Mem (/)");
        memBar = new JProgressBar();

        gcButton = new JButton("Trigger GC");
        //clearActionStack = new JButton("Clear Action Stack");

        this.add(cpuLabelSystem);
        this.add(cpuBarSystem);
        this.add(cpuLabelJava);
        this.add(cpuBarJava);
        this.add(memLabel);
        this.add(memBar);
        this.add(gcButton);
        //this.add(clearActionStack);
        gcButton.addActionListener(e -> System.gc());
        //clearActionStack.addActionListener(e -> mp.clearUserActions());

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);

        Thread monitor = new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int loadJava = (int)(osBean.getProcessCpuLoad()*100);
                cpuBarJava.setValue(loadJava);
                cpuLabelJava.setText("CPU Usage - Java ("+loadJava+"%)");

                int loadSystem = (int)(osBean.getSystemCpuLoad()*100);
                cpuBarSystem.setValue(loadSystem);
                cpuLabelSystem.setText("CPU Usage - System ("+loadSystem+"%)");

                double usedMemMb = ((double)Runtime.getRuntime().totalMemory() - (double)Runtime.getRuntime().freeMemory())/(1024*1024);
                double memTotalMb = ((double)Runtime.getRuntime().totalMemory())/(1024*1024);

                memLabel.setText("Mem ("+((int)usedMemMb)+" Mb / "+((int) memTotalMb)+" Mb)");
                memBar.setValue((int)(usedMemMb/memTotalMb*100));
            }
        });

        monitor.start();
    }

}
