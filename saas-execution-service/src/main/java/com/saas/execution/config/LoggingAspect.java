package com.saas.execution.config;

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

    @Pointcut("execution(* com.saas.execution..*Service.*(..))")
    public void serviceMethods() {}

    @Pointcut("execution(* com.saas.execution..*Repository.*(..))")
    public void repositoryMethods() {}

    @Pointcut("serviceMethods() || repositoryMethods()")
    public void businessLogicMethods() {}

    @Around("serviceMethods()")
    public Object trackServiceLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackLatency(joinPoint, "SERVICE");
    }

    @Around("repositoryMethods()")
    public Object trackRepositoryLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackLatency(joinPoint, "REPOSITORY");
    }

    private Object trackLatency(ProceedingJoinPoint joinPoint, String layerType) throws Throwable {
        long startTime = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("[saas-execution-service] Starting {} timer for: {}.{}", layerType, className, methodName);

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (executionTime > 2000) {
                log.warn("[saas-execution-service] {} {}.{} took {}ms to execute (SLOW!)",
                        layerType, className, methodName, executionTime);
            } else if (executionTime > 500) {
                log.info("[saas-execution-service] {} {}.{} took {}ms to execute (MODERATE)",
                        layerType, className, methodName, executionTime);
            } else {
                log.debug("[saas-execution-service] {} {}.{} took {}ms to execute",
                        layerType, className, methodName, executionTime);
            }

            return result;

        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            log.error("[saas-execution-service] {} {}.{} failed after {}ms due to: {}",
                    layerType, className, methodName, (endTime - startTime), throwable.getMessage());
            throw throwable;
        }
    }
}
