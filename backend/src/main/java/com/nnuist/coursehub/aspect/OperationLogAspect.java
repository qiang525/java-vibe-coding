package com.nnuist.coursehub.aspect;

import com.nnuist.coursehub.common.annotation.LogOperation;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.entity.AuditLog;
import com.nnuist.coursehub.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 操作审计日志切面
 *
 * <p><b>切面（AOP）解决什么问题</b>：如果每写一个"需要记日志"的接口，
 * 都要在方法里加一段 {@code auditLogMapper.insert(...)}，那代码会到处重复，
 * 而且很容易漏。AOP 的做法是：给方法打个 {@code @LogOperation} 标记，
 * 切面自动在方法执行后<strong>统一</strong>记录。</p>
 *
 * <p><b>为什么用 @Around 而不是 @AfterReturning</b>：
 * 只有方法成功执行了才记录，失败的调用（比如权限不足被拒）不该写进审计日志。</p>
 *
 * <p><b>关键原则</b>：记日志失败绝不能影响业务。所以整段记录逻辑包在 try-catch 里，
 * 出问题只打 warn 日志，不往上抛。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint pjp, LogOperation logOperation) throws Throwable {
        // 先让业务方法执行；它抛异常就直接往外抛（不记录成功的日志）
        Object result = pjp.proceed();

        try {
            record(pjp, logOperation);
        } catch (Exception e) {
            // 审计日志失败不影响业务，只记一条警告
            log.warn("记录操作审计日志失败 | operation={} | error={}",
                    logOperation.value(), e.getMessage());
        }
        return result;
    }

    private void record(ProceedingJoinPoint pjp, LogOperation anno) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(UserContext.getUserId());
        auditLog.setUsername(UserContext.getUsername() == null ? "" : UserContext.getUsername());
        auditLog.setOperation(anno.value());
        auditLog.setTargetType(anno.targetType());

        // 取对象 ID：从指定的参数位置读
        if (anno.targetIdIndex() >= 0) {
            Object[] args = pjp.getArgs();
            int idx = anno.targetIdIndex();
            if (idx < args.length && args[idx] instanceof Long) {
                auditLog.setTargetId((Long) args[idx]);
            }
        }

        auditLog.setDetail(buildDetail(pjp));
        auditLog.setIp(getClientIp());

        auditLogMapper.insert(auditLog);

        log.debug("已记录操作日志 | {} | userId={}", anno.value(), auditLog.getUserId());
    }

    /** 把方法名和参数拼成一段可读的描述（超长截断，避免撑爆字段） */
    private String buildDetail(ProceedingJoinPoint pjp) {
        String method = pjp.getSignature().getName();
        String args = Arrays.toString(pjp.getArgs());
        String detail = method + "(" + args + ")";
        return detail.length() > 950 ? detail.substring(0, 950) + "..." : detail;
    }

    /**
     * 取客户端真实 IP
     *
     * <p>为什么要看 {@code X-Forwarded-For}：如果有 Nginx 反向代理，
     * {@code getRemoteAddr()} 拿到的是 Nginx 的 IP，不是真实用户 IP。</p>
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "";
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // X-Forwarded-For 可能是逗号分隔的链路，取第一个（最靠近客户端的）
            int comma = ip.indexOf(',');
            if (comma > 0) {
                ip = ip.substring(0, comma).trim();
            }
            return ip == null ? "" : (ip.length() > 50 ? ip.substring(0, 50) : ip);
        } catch (Exception e) {
            return "";
        }
    }
}
