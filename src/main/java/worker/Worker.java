package worker;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import handler.Handler;
import lifecycle.LifeCycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责{@link Handler}链的调用执行.
 *
 * @author skywalker
 */
public class Worker implements Runnable, LifeCycle {

    private final BlockingQueue<Runnable> jobs;
    private static final int defaultQueueSize = 100;
    private final ExecutorService executors;

    private static final Runnable POISON = () -> {};
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    protected Worker(ExecutorService executors) {
        this(0, executors);
    }

    protected Worker(int queueSize, ExecutorService executors) {
        if (queueSize < 1) {
            queueSize = defaultQueueSize;
        }
        this.jobs = new ArrayBlockingQueue<>(queueSize);
        this.executors = executors;
    }

    @Override
    public void start() {
        executors.execute(this);
    }

    @Override
    public void close() {
        try {
            // just wait
            jobs.put(POISON);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * 向队列添加任务.
     *
     * @param task {@link Runnable} 任务
     * @return true，如果添加成功
     */
    public boolean submit(Runnable task) {
        return jobs.offer(task);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Runnable task = jobs.take();
                if (task == POISON) {
                    break;
                }
                task.run();
            }
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted.");
        }
    }

}
