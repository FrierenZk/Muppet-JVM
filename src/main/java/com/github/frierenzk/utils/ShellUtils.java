package com.github.frierenzk.utils;

import java.io.*;
import java.util.Iterator;
import java.util.List;

public class ShellUtils {
    private BufferedReader inputBuffer, errorBuffer;
    private Process process;

    public BufferedReader getInputBuffer() {
        return inputBuffer;
    }

    public BufferedReader getErrorBuffer() {
        return errorBuffer;
    }

    public void exec(List<String> command) {
        var processBuilder = new ProcessBuilder(command);
        try {
            process = processBuilder.start();
        } catch (Exception exception) {
            System.out.println(exception.toString());
            inputBuffer = new BufferedReader(new StringReader(exception.toString()));
            return;
        }
        inputBuffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        errorBuffer = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    }
    
    public void execCommands(List<String> commands) throws IOException {
        var processBuilder = new ProcessBuilder("sh");
        process = processBuilder.start();
        inputBuffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        errorBuffer = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        var stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        for (var i : commands) {
            stdin.write(i);
            stdin.newLine();
            stdin.flush();
        }
        stdin.close();
    }

    public int getReturnCode() throws InterruptedException {
        if (process != null) {
            if (process.isAlive()) {
                return process.waitFor() + 1;
            }
            return process.exitValue() + 1;
        }
        return -1;
    }

    public boolean getAlive() {
        if (process != null) return process.isAlive();
        else return false;
    }

    public int terminate() {
        if (process == null) return -1;
        process.destroy();
        try {
            process.wait(3000);
        } catch (Exception ignored) {
        }
        if (process.isAlive()) process.destroyForcibly();
        try {
            process.wait();
        } catch (Exception ignored) {
        }
        return process.exitValue();
    }
}