package com.n0thappy.spring.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * @author shuqinghua
 * @version Id: ThreadPoolUtil.java, v 0.1 2021/8/30 4:03 下午 shuqinghua Exp $$
 */
public class ThreadPoolUtil {

    private static Executor executor;
    private static  ForkJoinPool forkJoinPool;


    public static Executor getEnglishLearningPool() {
        if(executor == null) {
            synchronized (ThreadPoolUtil.class) {
                if(executor == null) {
                    ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("englishLearning-pool-%d").build();
                    return new ThreadPoolExecutor(100, 200, 60, TimeUnit.SECONDS,
                            new LinkedBlockingDeque<>(100),
                            factory,
                            new ThreadPoolExecutor.DiscardOldestPolicy());
                }
            }
        }
        return executor;
    }

    public static ForkJoinPool getForkJoinPool() {
        if(forkJoinPool == null) {
            synchronized (ThreadPoolUtil.class){
                if(forkJoinPool == null) {
                    return new ForkJoinPool(100);
                }
            }
        }
        return forkJoinPool;
    }

}
