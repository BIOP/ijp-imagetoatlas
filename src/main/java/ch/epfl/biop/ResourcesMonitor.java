package ch.epfl.biop;

import com.sun.management.OperatingSystemMXBean;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

//import java.lang.management.OperatingSystemMXBean;

import javax.management.MBeanServerConnection;
import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
//import java.lang.management.ManagementFactory;

public class ResourcesMonitor extends JPanel {

    //final JLabel cpuLabelJava;
    //final JProgressBar cpuBarJava;

    final JLabel cpuLabelSystem;
    final JProgressBar cpuBarSystem;

    final JLabel memLabel;
    final JProgressBar memBar;

    final JButton gcButton;

    public ResourcesMonitor() {
        this.setLayout(new GridLayout(8, 1));

        cpuLabelSystem = new JLabel("CPU Usage - System (%)");
        cpuBarSystem = new JProgressBar();

        //cpuLabelJava = new JLabel("CPU Usage - Java (%)");
        //cpuBarJava = new JProgressBar();

        memLabel = new JLabel("Mem (/)");
        memBar = new JProgressBar();

        gcButton = new JButton("Trigger GC");
        //clearActionStack = new JButton("Clear Action Stack");

        this.add(cpuLabelSystem);
        this.add(cpuBarSystem);
        //this.add(cpuLabelJava);
        //this.add(cpuBarJava);
        this.add(memLabel);
        this.add(memBar);
        this.add(gcButton);

        gcButton.addActionListener(e -> System.gc());

        //OperatingSystemMXBean osBean =
        //        ManagementFactory.getPlatformMXBean(
        //                OperatingSystemMXBean.class);

        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //int loadJava = 0;//(int) (osBean.getProcessCpuLoad() * 100);
                //cpuBarJava.setValue(loadJava);
                //cpuLabelJava.setText("CPU Usage - Java (" + loadJava + "%)");

                int loadSystem = (int) getCPU();//proc.g;//0;//(int) (osBean.getSystemCpuLoad() * 100);
                cpuBarSystem.setValue(loadSystem);
                cpuLabelSystem.setText("CPU Usage - System (" + loadSystem + "%)");

                double usedMemMb = ((double) Runtime.getRuntime().totalMemory() - (double) Runtime.getRuntime().freeMemory()) / (1024 * 1024);
                double memTotalMb = ((double) Runtime.getRuntime().totalMemory()) / (1024 * 1024);

                memLabel.setText("Mem (" + ((int) usedMemMb) + " Mb / " + ((int) memTotalMb) + " Mb)");
                memBar.setValue((int) (usedMemMb / memTotalMb * 100));
            }
        });

        monitor.start();

    }

    private SystemInfo si = new SystemInfo();
    private HardwareAbstractionLayer hal = si.getHardware();
    private CentralProcessor cpu = hal.getProcessor();
    long[] prevTicks = new long[CentralProcessor.TickType.values().length];

    //private SystemInfo systemInformation = new SystemInfo();
    //private CentralProcessor cpu = systemInformation.getHardware().getProcessor();

    public double getCPU() {
        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = cpu.getSystemCpuLoadTicks();
        //System.out.println("cpuLoad : " + cpuLoad);
        return cpuLoad;
    }
}
