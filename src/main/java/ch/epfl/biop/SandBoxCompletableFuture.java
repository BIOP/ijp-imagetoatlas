package ch.epfl.biop;

import ij.gui.YesNoCancelDialog;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Proof of principle : combining automated registration tasks and manual ones
 * for multiple sources.
 */

public class SandBoxCompletableFuture {

    static List<Sources> sources = new ArrayList<>();

    static JFrame demoFrame = new JFrame("Demo ASync jobs"); // Just for the yes no cancel dialog

    static JPanel demoReportingPanel;

    public static void main(String[] args) throws Exception {
        demoFrame.setSize(600,200);

        //http://www.migcalendar.com/miglayout/mavensite/docs/cheatsheet.html

        //https://www.oracle.com/technetwork/systems/ts-4928-159120.pdf

        demoReportingPanel = new JPanel(new MigLayout(new LC().fill(),
                new AC().fill(),
                new AC()));

        demoReportingPanel.setSize(600,200);
        //demoReportingPanel.add(new JPanel(),"cell 0 0");

        demoFrame.setVisible(true);
        demoFrame.setSize(600,200);
        demoFrame.add(demoReportingPanel);

        //demoFrame.pack();

        Sources srcs1 = new Sources("Source 1",1);
        Sources srcs2 = new Sources("Source 2",2);
        sources.add(srcs1);
        sources.add(srcs2);

        srcs1.addRegistration(new ManualReg1());
        srcs1.addRegistration(new AutoReg1());
        srcs1.addRegistration(new ManualReg1());

        srcs2.addRegistration(new AutoReg1());
        srcs2.addRegistration(new ManualReg1());
        srcs2.addRegistration(new AutoReg1());

        Thread.sleep(4000);
        //srcs1.registrationTasks.getNumberOfDependents().cancel(true);
        System.out.println("srcs1.registrationTasks.getNumberOfDependents() = "+srcs1.registrationTasks.getNumberOfDependents());
        System.out.println("srcs1.registrationTasks.getNumberOfDependents() = "+srcs2.registrationTasks.getNumberOfDependents());
        try {
            CompletableFuture.allOf(srcs1.registrationTasks, srcs2.registrationTasks).get();
        } catch (Exception e) {
            System.out.println("jobs partially cancelled");
        }

        System.out.println("srcs1.registrationTasks.getNumberOfDependents() = "+srcs1.registrationTasks.getNumberOfDependents());
        System.out.println("srcs1.registrationTasks.getNumberOfDependents() = "+srcs2.registrationTasks.getNumberOfDependents());

        System.out.println("# registrations 1 = "+srcs1.registrations.size());
        System.out.println("# registrations 2 = "+srcs2.registrations.size());

    }

    public interface Reg {
        boolean isManual();
        boolean register();
    }

    static public class ManualReg1 implements Reg {
        public boolean isManual() {
            return true;
        }
        public boolean register() {
            System.out.println("Start manual Reg");
            //WaitForUserDialog dialog = new WaitForUserDialog("Manual Registration", "Click ok when you're done.");
            //dialog.show();

            YesNoCancelDialog dialogw = new YesNoCancelDialog(demoFrame,
                    "Register slice", "Add a registration ?");

            if (dialogw.cancelPressed()||(!dialogw.yesPressed())) {
                System.out.println("Manual Reg cancelled!");
                return false;
            }

            System.out.println("End manual Reg");
            return true;
        }
    }

    static public class AutoReg1 implements Reg {
        public boolean isManual() {
            return false;
        }
        public boolean register() {
            System.out.println("Start AutoReg1");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("AutoReg1 Done");
            return true;
        }
    }

    static public class Sources {
        private List<Reg> registrations = new ArrayList<>();
        private List<JComponent> components = new ArrayList<>();

        static Object manualLock = new Object();

        String name;
        int location;

        public Sources(String name, int location) {
            this.name = name;
            this.location = location;
        }

        public CompletableFuture<Boolean> registrationTasks = CompletableFuture.completedFuture(true);

        public void addRegistration(Reg reg) {

            if (registrationTasks == null || registrationTasks.isDone()) {
                registrationTasks = CompletableFuture.supplyAsync(() -> true); // Starts async computation, maybe there's a better way
            }

            registrationTasks = registrationTasks.thenApplyAsync((flag) -> {
                JLabel current = new JLabel();
                components.add(current);
                demoReportingPanel.add(components.get(components.size()-1), "cell "+location+" "+registrations.size());

                if (flag==false) {
                    System.out.println("Downstream registration failed");
                    components.remove(current);
                    return false;
                }
                boolean out;
                if (reg.isManual()) {
                    current.setText("Lock (Manual)");
                    synchronized (manualLock) {
                        current.setText("Current");
                        out = reg.register();
                        if (!out) {
                            components.remove(current);
                            demoReportingPanel.remove(current);
                            current.setText("Canceled");
                        }
                    }
                } else {
                    current.setText("In progress...");
                    out = reg.register();
                }
                if (out) {
                    current.setText("Done");
                    registrations.add(reg);
                } else {
                    components.remove(current);
                    demoReportingPanel.remove(current);
                }
                System.out.println("Reg : "+name+" done");
                demoFrame.repaint();
                return out;
            });

            registrationTasks.handle((result, exception) -> {
                System.out.println(result);
                if (result == false) {
                    System.out.println("Registration task failed");
                }
                System.out.println(exception);
                return exception;
            });
        }

    }

}
