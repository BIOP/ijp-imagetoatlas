package ch.epfl.biop;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import javax.swing.*;
import java.awt.*;

public class ResourcesMonitor extends JPanel {

    final JLabel cpuLabelSystem;
    final JProgressBar cpuBarSystem;

    final JLabel memLabel;
    final JProgressBar memBar;

    final JButton gcButton;
    Thread monitor;

    public ResourcesMonitor() {
        this.setLayout(new GridLayout(8, 1));

        cpuLabelSystem = new JLabel("CPU Usage - System (%)");
        cpuBarSystem = new JProgressBar();

        memLabel = new JLabel("Mem (/)");
        memBar = new JProgressBar();

        gcButton = new JButton("Trigger GC");

        this.add(cpuLabelSystem);
        this.add(cpuBarSystem);
        this.add(memLabel);
        this.add(memBar);
        this.add(gcButton);

        gcButton.addActionListener(e -> System.gc());

         monitor = new Thread(() -> {
            boolean monitoring = true;
            while (monitoring) {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    monitoring = false;
                }
                int loadSystem = (int) getCPU();
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

    private final SystemInfo si = new SystemInfo();
    private final HardwareAbstractionLayer hal = si.getHardware();
    private final CentralProcessor cpu = hal.getProcessor();
    long[] prevTicks = new long[CentralProcessor.TickType.values().length];

    public double getCPU() {
        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = cpu.getSystemCpuLoadTicks();
        return cpuLoad;
    }

    public void stop() {
        monitor.interrupt();
    }
}
