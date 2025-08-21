package com.potential_radar.PR.search.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
@Slf4j
public class AsyncConfig {

    /**
     * 검색 동기화 전용 스레드 풀
     */
    @Bean("searchSyncExecutor")
    public Executor searchSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 설정
        executor.setCorePoolSize(2);           // 기본 스레드 수
        executor.setMaxPoolSize(5);            // 최대 스레드 수
        executor.setQueueCapacity(100);        // 대기 큐 크기
        executor.setKeepAliveSeconds(60);      // 유휴 스레드 생존 시간
        
        // 스레드 이름 설정
        executor.setThreadNamePrefix("SearchSync-");
        
        // 거부 정책 설정 (큐가 가득 찰 때)
        executor.setRejectedExecutionHandler(new SearchSyncRejectedExecutionHandler());
        
        // 애플리케이션 종료 시 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("SearchSync ThreadPool initialized - Core: {}, Max: {}, Queue: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * 스케줄러 전용 스레드 풀
     */
    @Bean("schedulerExecutor")
    public Executor schedulerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Scheduler-");
        executor.setRejectedExecutionHandler(new SchedulerRejectedExecutionHandler());
        
        executor.initialize();
        return executor;
    }

    /**
     * 검색 동기화 전용 거부 처리 핸들러
     */
    private static class SearchSyncRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("SearchSync task rejected: Queue full ({}/{}), Active: {}/{}", 
                    executor.getQueue().size(), 
                    executor.getQueue().size() + executor.getQueue().remainingCapacity(),
                    executor.getActiveCount(), 
                    executor.getMaximumPoolSize());
            
            // 동기적으로 실행 (데이터 일관성 보장)
            try {
                r.run();
                log.info("Rejected SearchSync task executed synchronously");
            } catch (Exception e) {
                log.error("Failed to execute rejected SearchSync task", e);
            }
        }
    }

    /**
     * 스케줄러 전용 거부 처리 핸들러
     */
    private static class SchedulerRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Scheduler task rejected - skipping this execution cycle");
            // 스케줄러 작업은 다음 주기에 다시 실행되므로 무시
        }
    }
}