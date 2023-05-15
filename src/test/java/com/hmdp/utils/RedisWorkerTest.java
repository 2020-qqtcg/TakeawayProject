package com.hmdp.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : mj
 * @since 2023/5/5 10:13
 */
@SpringBootTest
class RedisWorkerTest {

    @Autowired
    private RedisWorker redisWorker;

    private final ExecutorService es = Executors.newFixedThreadPool(300);

    @Test
    void testNextId() throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(300);
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++){
            es.submit(() -> {
                for (int j = 0; j < 100; j++){
                    long id = redisWorker.nextId("order");

                    System.out.println("id = " + id);
                }
                latch.countDown();
            });
        }
        latch.await();
        long end = System.currentTimeMillis();

        System.out.println("花费时间："+ (end - begin));
    }
}