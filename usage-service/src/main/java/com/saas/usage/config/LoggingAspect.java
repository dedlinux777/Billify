package com.saas.usage.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * POINTCUT FOR SERVICE LAYER
     * Matches all methods in classes under com.saas.usage.service package
     * and any subpackages
     */
    @Pointcut("execution(* com.saas.usage.service..*.*(..))")
    public void serviceMethods() {}

    /**
     * POINTCUT FOR REPOSITORY LAYER
     * Matches all methods in classes under com.saas.usage.repository package
     * and any subpackages
     */
    @Pointcut("execution(* com.saas.usage.repository..*.*(..))")
    public void repositoryMethods() {}

    /**
     * POINTCUT FOR BOTH SERVICE AND REPOSITORY LAYERS
     */
    @Pointcut("serviceMethods() || repositoryMethods()")
    public void businessLogicMethods() {}

    /**
     * AROUND ADVICE FOR SERVICE LAYER
     */
    @Around("serviceMethods()")
    public Object trackServiceLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackLatency(joinPoint, "SERVICE");
    }

    /**
     * AROUND ADVICE FOR REPOSITORY LAYER
     */
    @Around("repositoryMethods()")
    public Object trackRepositoryLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackLatency(joinPoint, "REPOSITORY");
    }

    /**
     * Generic method to track latency for any join point
     */
    private Object trackLatency(ProceedingJoinPoint joinPoint, String layerType) throws Throwable {
        long startTime = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("[usage-service] Starting {} timer for: {}.{}", layerType, className, methodName);

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (executionTime > 1000) {
                log.warn("[usage-service] {} {}.{} took {}ms to execute (SLOW!)",
                        layerType, className, methodName, executionTime);
            } else {
                log.info("[usage-service] {} {}.{} took {}ms to execute",
                        layerType, className, methodName, executionTime);
            }

            return result;

        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            log.error("[usage-service] {} {}.{} failed after {}ms due to: {}",
                    layerType, className, methodName, (endTime - startTime), throwable.getMessage());
            throw throwable;
        }
    }
}
