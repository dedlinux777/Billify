package com.billify.aspect;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceTrackingAspect {
    /**
     * POINTCUT EXPRESSION
     * execution: matches method execution join points.
     * * : matches any return type.
     * com.billify.plan.service..* : matches any class inside the service package or subpackages.
     * .*(..) : matches any method name with any number/type of arguments.
     */
    @Pointcut("execution(* com.billify.plan..*.*(..))")
    public void planLayerMethods() {
        // Empty body
    }
    /**
     * AROUND ADVICE
     * Uses the pointcut defined above.
     * ProceedingJoinPoint is mandatory here; it represents the actual method waiting to execute.
     */
    @Around("planLayerMethods()")
    public Object trackLatency(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Get metadata about the target method being called
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        System.out.println("[Billify] Starting timer for: " + className + "." + methodName);

        try {
            // This line triggers the execution of the actual business method (Target Object)
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.println("[Billify] " + className + "." + methodName + " took " + executionTime + "ms to execute.");

            return result; // Pass the actual method result back to the caller

        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            System.out.println("[Billify] Method failed after " + (endTime - startTime) + "ms due to: " + throwable.getMessage());
            throw throwable; // Rethrow so the application behaves normally
        }

    }

}
