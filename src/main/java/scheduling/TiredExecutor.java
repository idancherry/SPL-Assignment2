package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        
        workers = new TiredThread[numThreads];

        for(int i=0; i<numThreads; i++){
            workers[i] = new TiredThread(i, Math.random() + 0.5);
            idleMinHeap.add(workers[i]);
            workers[i].start();
        }
    }

    public void submit(Runnable task) {
    inFlight.incrementAndGet();

    try {
        TiredThread worker = idleMinHeap.take();

        worker.newTask(() -> {
            try {
                task.run(); 
            } finally {
            
                inFlight.decrementAndGet();
                idleMinHeap.add(worker); 
                notifyAll();
            }
        });

    } catch (InterruptedException e) {
        inFlight.decrementAndGet();
        e.printStackTrace();
    }
}

    public void submitAll(Iterable<Runnable> tasks) {
        //submit tasks one by one and wait until all finish

       for (Runnable task : tasks){
        
            submit(task);
            inFlight.incrementAndGet();
        }
        synchronized(this){
            while (inFlight.get() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        for (TiredThread worker : workers){
            if (worker != null) {
                worker.shutdown();
            }
        }

        for (TiredThread worker : workers) {
            if (worker != null) {
                worker.join(); 
            }
        }
    }
    

    public synchronized String getWorkerReport() {
        String output ="";

        for (TiredThread worker : workers){
             output += "name of the worker: " + worker.getName()  + ", woker's id: "+ worker.getWorkerId() + 
             ", time he worked: " + worker.getTimeUsed() + ", time he rested: " 
             + worker.getTimeIdle() + ",his fatigue level" + worker.getFatigue() +"/n" ;

        }

        return output;
    }
}
