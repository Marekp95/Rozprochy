/**
 * A simple example program to use DataMonitor to start and
 * stop executables based on a znode. The program watches the
 * specified znode and saves the data that corresponds to the
 * znode in the filesystem. It also starts the specified program
 * with the specified arguments when the znode exists and kills
 * the program if the znode goes away.
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.*;
import java.util.List;

public class Executor
        implements Watcher, Runnable, DataMonitor.DataMonitorListener {
    private static final String znode = "/znode_testowy";

    private final DataMonitor dm;

    private final ZooKeeper zk;

    private final String exec[];

    private Process child;

    Executor(String hostPort,
                    String exec[]) throws KeeperException, IOException {
        this.exec = exec;
        zk = new ZooKeeper(hostPort, 3000, this);
        dm = new DataMonitor(zk, znode, null, this);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err
                    .println("USAGE: Executor hostPort program [args ...]");
            System.exit(2);
        }
        final String hostPort = args[0];
        final String exec[] = new String[args.length - 1];
        System.arraycopy(args, 1, exec, 0, exec.length);
        try {
            new Executor(hostPort, exec).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***************************************************************************
     * We do process any events ourselves, we just need to forward them on.
     *
     */
    @Override
    public void process(WatchedEvent event) {
        dm.process(event);
    }

    @Override
    public void run() {
        showNodes();
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        mainLoop:
        while (true) {
            try {
                final String line = bufferedReader.readLine();
                switch (line) {
                    case "quit":
                        break mainLoop;
                    case "show":
                        showNodes();
                        break;
                    default:
                        System.out.println("Invalid command");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showNodes(){
        showNodes(znode, 0);
    }

    private void showNodes(String basePath, int depth) {
        System.out.print(StringUtils.repeat('\t', depth));
        System.out.println(basePath.replaceAll(".*/", ""));
        try {
            final List<String> children = zk.getChildren(basePath, this);
            children.forEach(c -> showNodes(basePath + "/" + c, depth + 1));
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }

    static class StreamWriter extends Thread {
        final OutputStream os;

        final InputStream is;

        StreamWriter(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            start();
        }

        @Override
        public void run() {
            byte b[] = new byte[80];
            int rc;
            try {
                while ((rc = is.read(b)) > 0) {
                    os.write(b, 0, rc);
                }
            } catch (IOException ignored) {
            }

        }
    }

    @Override
    public void exists(byte[] data) {
        if (data == null) {
            if (child != null) {
                System.out.println("Killing process");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException ignored) {
                }
            }
            child = null;
        } else {
            if (child != null) {
                System.out.println("Stopping child");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                zk.getChildren(znode, this);
                System.out.println("Starting child");
                child = Runtime.getRuntime().exec(exec);
                new StreamWriter(child.getInputStream(), System.out);
                new StreamWriter(child.getErrorStream(), System.err);
            } catch (IOException | InterruptedException | KeeperException e) {
                e.printStackTrace();
            }
        }
    }
}