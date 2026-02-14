package task.worker.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TaskExecutor {

    @Value("${worker.nodeId}")
    private String nodeId;

    public String execute(String taskType, String taskDetailsJson) {
        // Keep it simple: real logic goes here
        return switch (taskType) {
            case "EMAIL_SEND" -> doEmail(taskDetailsJson);
            case "PDF_GENERATE" -> doPdf(taskDetailsJson);
            case "DATA_EXPORT" -> doExport(taskDetailsJson);
            case "REPORT_BUILD" -> doReport(taskDetailsJson);
            default -> throw new IllegalArgumentException("Unknown taskType: " + taskType);
        };
    }

    private String doEmail(String details) {
        System.out.println("Sending Email by node id : "+"tasks.node." + nodeId);
        return "{\"sent\":true}";
    }

    private String doPdf(String details) {
        System.out.println("Generating PDF by node id : "+"tasks.node." + nodeId);
        return "{\"pdf\":\"generated\"}";
    }

    private String doExport(String details) {
        System.out.println("Exporting CSV by node id : "+"tasks.node." + nodeId);
        return "{\"export\":\"done\"}";
    }

    private String doReport(String details) {
        System.out.println("Building Report by node id : "+"tasks.node." + nodeId);
        return "{\"report\":\"built\"}";
    }
}
