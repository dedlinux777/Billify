package com.saas.billing.aspect;

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
     * Matches all methods in classes that end with "Service"
     * within com.saas.billing and its subpackages
     * Examples: AuthService, InvoiceService, PaymentService, PlanService, SubscriptionService
     */
    @Pointcut("execution(* com.saas.billing..*Service.*(..))")
    public void serviceMethods() {}

    /**
     * POINTCUT FOR REPOSITORY LAYER
     * Matches all methods in classes that end with "Repository"
     * within com.saas.billing and its subpackages
     * Examples: InvoiceRepository, PaymentRepository, PlanRepository, SubscriptionRepository, UserRepository
     */
    @Pointcut("execution(* com.saas.billing..*Repository.*(..))")
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

        log.info("[billify-core-service] Starting {} timer for: {}.{}", layerType, className, methodName);

        try {
            // This line triggers the execution of the actual business method (Target Object)
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Log with different thresholds for service vs repository
            if (executionTime > 2000) {
                log.warn("[billify-core-service] {} {}.{} took {}ms to execute (SLOW!)",
                        layerType, className, methodName, executionTime);
            } else if (executionTime > 500) {
                log.info("[billify-core-service] {} {}.{} took {}ms to execute (MODERATE)",
                        layerType, className, methodName, executionTime);
            } else {
                log.debug("[billify-core-service] {} {}.{} took {}ms to execute",
                        layerType, className, methodName, executionTime);
            }

            return result; // Pass the actual method result back to the caller

        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            log.error("[billify-core-service] {} {}.{} failed after {}ms due to: {}",
                    layerType, className, methodName, (endTime - startTime), throwable.getMessage());
            throw throwable; // Rethrow so the application behaves normally
        }
    }

    /**
     * Alternative: SINGLE ADVICE FOR ALL BUSINESS LOGIC
     * Uncomment this if you want to track both service and repository with a single advice
     */
    /*
    @Around("businessLogicMethods()")
    public Object trackAllLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String layerType = className.endsWith("Repository") ? "REPOSITORY" : "SERVICE";
        return trackLatency(joinPoint, layerType);
    }
    */
}