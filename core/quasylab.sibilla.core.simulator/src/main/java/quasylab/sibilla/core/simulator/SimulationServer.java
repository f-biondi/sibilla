package quasylab.sibilla.core.simulator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class SimulationServer<S> {
    private ServerSocket serverSocket;
    private SimulationTask<?> task2;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        while(true){
            new Thread(new TaskHandler(serverSocket.accept())).start();
        }
    }

    private class TaskHandler implements Runnable{
        Socket socket;
        public TaskHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                
                NetworkTask<?> ntask = ((NetworkTask<?>) ois.readObject());
                
                SimulationTask<?> task = ntask.getTask();
                int repetitions = ntask.getRepetitions();
                List<Trajectory<?>> results = new LinkedList<>();
                for(int i = 0; i < repetitions; i++){
                    results.add(task.get());
                    task.reset();
                }
                oos.writeObject(results);

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}