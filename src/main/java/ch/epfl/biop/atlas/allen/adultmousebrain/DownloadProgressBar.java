package ch.epfl.biop.atlas.allen.adultmousebrain;

import java.io.BufferedOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class DownloadProgressBar {

    public static void urlToFile(URL url, File file, String frameTitle, long fileSize) throws Exception {

        final JProgressBar jProgressBar = new JProgressBar();
        jProgressBar.setMaximum(10000);
        JFrame frame = new JFrame(frameTitle);
        frame.setContentPane(jProgressBar);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(600, 250);
        frame.setVisible(true);

        RunnableWithException updatethread = () -> {
                HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
                long completeFileSize = httpConnection.getContentLength();
                System.out.println("File Size : "+completeFileSize);

                if (completeFileSize == -1) completeFileSize = fileSize;

                java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(
                        file.getAbsolutePath());
                BufferedOutputStream bout = new BufferedOutputStream(
                        fos, 1024*1024);
                byte[] data = new byte[1024*1024];
                long downloadedFileSize = 0;
                int x = 0;
                while ((x = in.read(data, 0, 1024*1024)) >= 0) {
                    downloadedFileSize += x;

                    // calculate progress
                    final int currentProgress = (int) ((((double)downloadedFileSize) / ((double)completeFileSize)) * 10000);

                    // update progress bar
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            jProgressBar.setValue(currentProgress);
                        }
                    });

                    bout.write(data, 0, x);
                }
                bout.close();
                in.close();
        };

        Thread t = new Thread(() -> {
            try {
                updatethread.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
        t.join();
        frame.setVisible(false);
        frame.dispose();
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

}