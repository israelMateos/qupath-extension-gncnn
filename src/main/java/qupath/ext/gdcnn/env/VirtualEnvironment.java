package qupath.ext.gdcnn.env;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.controlsfx.tools.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.ext.gdcnn.entities.ProgressListener;

/**
 * Wraps a shell environment to be used for the extension
 * and provides methods to execute commands in the environment.
 * 
 * @author Israel Mateos Aparicio
 */
public class VirtualEnvironment {

    private final Logger logger = LoggerFactory.getLogger(VirtualEnvironment.class);

    private String name;

    private List<String> arguments;

    private ProgressListener progressListener;

    public VirtualEnvironment(String name, ProgressListener progressListener) {
        this.name = name;
        this.progressListener = progressListener;
    }

    private double parseDetectionProgress(String line) {
        String[] splitted = line.split("\\s+");
        for (String s : splitted) {
            if (s.contains("%")) {
                // Remove the everything after the percentage sign (including the sign itself)
                return Double.parseDouble(s.substring(0, s.indexOf("%"))) / 100.0;
            }
        }
        return -1.0;
    }

    /**
     * Sets the arguments to be used in the command
     * 
     * @param arguments
     */
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    /**
     * Builds the command to be run and executes it
     * 
     * @throws IOException          // In case there is an issue starting the
     *                              process
     * @throws InterruptedException // In case there is an issue after the process
     *                              is started
     */
    public void runCommand() throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();

        // Get the arguments specific to the command we want to run
        command.addAll(arguments);

        List<String> shell = new ArrayList<>();
        switch (Platform.getCurrent()) {
            // For UNIX, encapsulate the command in a single string
            case UNIX:
            case OSX:
                shell.addAll(Arrays.asList("bash", "-c"));

                // If there are spaces, encapsulate the command with quotes
                command = command.stream().map(s -> {
                    if (s.trim().contains(" "))
                        return "\"" + s.trim() + "\"";
                    return s;
                }).collect(Collectors.toList());

                // The last part needs to be sent as a single string, otherwise it does not run
                String cmdString = command.toString().replace(",", "");

                shell.add(cmdString.substring(1, cmdString.length() - 1));
                break;

            // For windows, continue appending the command;
            case WINDOWS:
            default:
                shell.addAll(Arrays.asList("cmd.exe", "/C"));
                shell.addAll(command);
                break;
        }

        // Make the command human readable
        List<String> printable = shell.stream().map(s -> {
            // Add quotes if there are spaces in the paths
            if (s.contains(" "))
                return "\"" + s + "\"";
            else
                return s;
        }).collect(Collectors.toList());
        String executionString = printable.toString().replace(",", "");

        logger.info("Executing command:\n{}", executionString.substring(1, executionString.length() - 1));
        logger.info("This command should run directly if copy-pasted into your shell");

        ProcessBuilder pb = new ProcessBuilder(shell).redirectErrorStream(true);

        // Check if the thread has been interrupted before starting the process
        if (Thread.interrupted()) {
            logger.warn("Thread interrupted");
            return;
        }

        Process p = pb.start();

        Thread t = new Thread(Thread.currentThread().getName() + "-" + p.hashCode()) {
            @Override
            public void run() {
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    double currentProgress = 0.0;
                    if (name.equals("GlomerulusDetectionTask")) {
                        currentProgress = progressListener.getProgress();
                    }

                    for (String line = stdIn.readLine(); line != null;) {
                        logger.info("{}: {}", name, line);
                        if (name.equals("GlomerulusDetectionTask") && line.contains("%")) {
                            double detectionProgress = parseDetectionProgress(line);
                            if (detectionProgress != -1.0) {
                                progressListener.updateProgress(currentProgress, detectionProgress);
                            }
                        }
                        line = stdIn.readLine();
                    }
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
        };
        t.setDaemon(true);
        t.start();

        try {
            p.waitFor();
            // If the thread is interrupted while running the command, stop the
            // process and the thread
        } catch (InterruptedException e) {
            logger.warn("Thread interrupted");
            p.destroy();
            t.interrupt();
            throw e;
        }

        logger.info("Virtual Environment Runner Finished");

        int exitValue = p.exitValue();

        if (exitValue != 0) {
            logger.error("Runner '{}' exited with value {}. Please check output above for indications of the problem.",
                    name, exitValue);
        }
    }

    /**
     * Interrupts the thread running the command
     */
    public void interrupt() {
        Thread.currentThread().interrupt();
    }
}
