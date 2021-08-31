package com.n0thappy.spring.controller;

import com.n0thappy.spring.handler.EnglishLearningHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * @author shuqinghua
 * @version Id: EnglishLearningController.java, v 0.1 2021/8/30 9:10 下午 shuqinghua Exp $$
 */
@Slf4j
@RestController
public class EnglishLearningController {

    @Autowired
    private EnglishLearningHandler handler;

    @GetMapping("/api/englishlearning/sync/{num}")
    public String findEnglishLinesSync(@PathVariable int num) {
        long start = Instant.now().toEpochMilli();
        List<String> linesSync = handler.findEnglishLinesSync("keyword", num);
        String res = "同步请求耗时: "+(Instant.now().toEpochMilli()-start)+"ms, 集合大小: "+linesSync.size();
        log.info(res);
        return res;
    }

    @GetMapping("/api/englishlearning/async1/{num}")
    public String findEnglishLinesParallelSync(@PathVariable int num) {
        long start = Instant.now().toEpochMilli();
        List<String> linesSync = handler.findEnglishLinesParallelAsync("keyword", num);
        String res = "parallelStream异步请求耗时: "+(Instant.now().toEpochMilli()-start)+"ms, 集合大小: "+linesSync.size();
        log.info(res);
        return res;
    }

    @GetMapping("/api/englishlearning/async2/{num}")
    public String findEnglishLinesAsync(@PathVariable int num) {
        long start = Instant.now().toEpochMilli();
        List<String> linesAsync = handler.findEnglishLinesAsync("keyword", num);
        String res = "completableFuture异步请求耗时: "+(Instant.now().toEpochMilli()-start)+"ms, 集合大小: "+linesAsync.size();
        log.info(res);
        return res;
    }

}
