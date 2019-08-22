package com.diamonddrake.util.semaphore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class App {

    public static void main(String[] args) {

        // note we are writing to error stream so that logging doesn't get written back
        // to
        // consumer of this applet

        int timeout = 3000;

        int portNumber = 1234;
        String semaphore = "ready";

        String remoteHost = "localhost";

        if (args.length > 0)
            semaphore = args[0];

        if (semaphore.equals("?")) {
            System.out.println("usage: semaphore remotehost port");
            System.exit(0);
        }

        if (args.length > 1)
            remoteHost = args[1];

        if (args.length > 2 && args[2].matches("[\\d]+"))
            portNumber = Integer.parseInt(args[2]);

        boolean waitingForMessage = true;
        while (waitingForMessage) {

            // first send a messsage to see if anyone is listening,
            // if they are listening, see if you're on the same message an if you are return
            // happy
            System.err.println(
                    "Looking for semaphore " + semaphore + " on host " + remoteHost + " and port " + portNumber);

            try (Socket sendSocket = new Socket(remoteHost, portNumber)) {
                sendSocket.setSoTimeout(timeout);

                try (PrintWriter out = new PrintWriter(sendSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(sendSocket.getInputStream()));
                        ) {

                    System.err.println("reached a listener");
                    out.write(semaphore + "\n");
                    out.flush();
                    while(!in.ready()){
                        System.err.print(".");
                    }
                    String invalue = in.readLine();
                    System.err.println(invalue);
                    if (invalue.contains(semaphore)) {
                        System.err.println("reached expected");
                        out.println(semaphore);
                        waitingForMessage = false;
                    } else {
                        System.err.println("semaphore not ready or wrong one");
                    }
                } catch (SocketTimeoutException ex) {
                } catch (IOException ex) {
                }
            } catch (UnknownHostException uhe) {
                // here we are okay with it falling out
                System.err.println("host not found");
            } catch (IOException ex) {
                System.err.println("not found.");
            }

            // don't go into listening mode if we were heard by who we wanted
            if (waitingForMessage == false)
                break;

            // if no one was listening, listen for someone else for a couple seconds, see if
            // they
            // contact you, if not start over and contact them.

            try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
                serverSocket.setSoTimeout(timeout);
                System.err.println("Listening for semaphore " + semaphore + " on port " + portNumber);
                try (Socket clientSocket = serverSocket.accept();
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {

                    System.err.println("got a request");
                    while(!in.ready()){
                        System.err.print(".");
                    }
                    String invalue = in.readLine();
                    System.err.println(invalue);
                    if (invalue.contains(semaphore)) {
                        System.err.println("got expected");
                        out.write(semaphore + "\n");
                        out.flush();
                        waitingForMessage = false;
                    } else {
                        System.err.println("recevied not ready or not semaphore");
                    }
                } catch (SocketTimeoutException ioe) {
                    // every 2 seconds of not getting a connection this happens
                    System.err.println("No one contacted me, trying again to send");
                }
            } catch (IOException ioe) {
                System.err.println("io exception I wasn't expecting happened.");
            }
        }
    }
}
