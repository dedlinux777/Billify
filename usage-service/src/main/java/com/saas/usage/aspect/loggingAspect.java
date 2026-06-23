package com.saas.usage.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class loggingAspect {

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
     * Combines both pointcuts for convenience
     */
    @Pointcut("serviceMethods() || repositoryMethods()")
    public void businessLogicMethods() {}

    /**
     * AROUND ADVICE FOR SERVICE LAYER
     * Tracks performance of service methods
     */
    @Around("serviceMethods()")
    public Object trackServiceLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackLatency(joinPoint, "SERVICE");
    }

    /**
     * AROUND ADVICE FOR REPOSITORY LAYER
     * Tracks performance of repository methods
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

        // Get metadata about the target method being called
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String fullClassName = joinPoint.getTarget().getClass().getName();

        log.info("[usage-service] Starting {} timer for: {}.{}", layerType, className, methodName);

        try {
            // This line triggers the execution of the actual business method
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Log with different thresholds for service vs repository
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

    /**
     * Alternative: Single pointcut for all business logic
     * Uncomment this if you want to track both service and repository
     * with a single advice
     */
    /*
    @Around("businessLogicMethods()")
    public Object trackAllLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        String layerType = joinPoint.getTarget().getClass().getSimpleName()
            .contains("Repository") ? "REPOSITORY" : "SERVICE";
        return trackLatency(joinPoint, layerType);
    }
    */
}