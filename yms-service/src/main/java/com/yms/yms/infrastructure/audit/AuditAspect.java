package com.yms.yms.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository repository;
    private final HttpServletRequest request;

    public AuditAspect(AuditLogRepository repository, HttpServletRequest request) {
        this.repository = repository;
        this.request = request;
    }

    @Around("@annotation(audit)")
    public Object record(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long started = System.currentTimeMillis();
        Object result;
        String status = "SUCCESS";
        String error = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            status = "FAILURE";
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actor = authentication != null ? authentication.getName() : "anonymous";
            try {
                repository.save(AuditLogEntity.from(
                        actor,
                        audit.module(),
                        audit.action(),
                        status,
                        request.getRequestURI(),
                        System.currentTimeMillis() - started,
                        error));
            } catch (Exception auditFailure) {
                log.error("Unable to persist audit event {}:{}", audit.module(), audit.action(), auditFailure);
            }
        }
        return result;
    }
}
