package ru.otus.chat.server;

public class InactivityChecker implements Runnable {
    Server server;
    private final long timeoutMillis;
    public volatile boolean running = true;

    public InactivityChecker(long timeoutMillis, Server server) {
        this.timeoutMillis = timeoutMillis;
        this.server = server;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(60000);
                checkInacvtiveUsers();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void checkInacvtiveUsers() {
        long now = System.currentTimeMillis();
        for (ClientHandler c : server.getClients()) {
            if (c.getInactiveMillis() > timeoutMillis) {
                server.broadcastMessage(c.getUsername() + " был отключен за неактивность");
                c.sendSystemMsg("/kickok бездействие");
                c.disconnect();
            }
        }

    }

    public void stop() {
        running = false;
    }
}

