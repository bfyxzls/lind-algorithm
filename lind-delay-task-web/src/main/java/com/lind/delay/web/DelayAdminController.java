package com.lind.delay.web;

import com.lind.delay.task.DelayTask;
import com.lind.delay.task.DelayTaskService;
import com.lind.delay.task.DelayTaskStatus;
import com.lind.delay.task.DemoDelayTaskHandlers;
import com.lind.delay.task.PageResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tasks")
@RequiredArgsConstructor
public class DelayAdminController {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private final DelayTaskService delayTaskService;

	@GetMapping
	public String list(@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "10") int size,
			@RequestParam(name = "status", required = false) String status, Model model) {
		String statusFilter = StringUtils.hasText(status) ? status : null;
		PageResult<DelayTask> result = delayTaskService.page(page, size, statusFilter);
		List<TaskRow> rows = result.getItems().stream().map(this::toRow).collect(Collectors.toList());
		model.addAttribute("rows", rows);
		model.addAttribute("page", result.getPage());
		model.addAttribute("size", result.getSize());
		model.addAttribute("total", result.getTotal());
		model.addAttribute("totalPages", result.getTotalPages());
		model.addAttribute("status", statusFilter == null ? "" : statusFilter);
		model.addAttribute("statuses", Arrays.stream(DelayTaskStatus.values()).map(Enum::name).toList());
		return "tasks/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("bizType", DemoDelayTaskHandlers.BIZ_TYPE_DEMO);
		return "tasks/form";
	}

	@PostMapping
	public String create(@RequestParam(name = "bizId") String bizId,
			@RequestParam(name = "bizType", defaultValue = "demo") String bizType,
			@RequestParam(name = "delaySeconds") long delaySeconds,
			@RequestParam(name = "payload", required = false) String payload, RedirectAttributes redirect) {
		if (!StringUtils.hasText(bizId) || delaySeconds < 0) {
			redirect.addFlashAttribute("error", "bizId 不能为空，delaySeconds 必须 >= 0");
			return "redirect:/admin/tasks/new";
		}
		String id = delayTaskService.schedule(bizType, bizId.trim(), delaySeconds * 1000L,
				payload == null ? "" : payload.trim());
		redirect.addFlashAttribute("message", "已创建任务 " + id);
		return "redirect:/admin/tasks";
	}

	@PostMapping("/cancel")
	public String cancel(@RequestParam(name = "taskId") String taskId, RedirectAttributes redirect) {
		boolean ok = delayTaskService.cancel(taskId);
		redirect.addFlashAttribute("message", ok ? "已取消 " + taskId : "取消失败（可能已结束）");
		return "redirect:/admin/tasks";
	}

	@PostMapping("/delete")
	public String delete(@RequestParam(name = "taskId") String taskId, RedirectAttributes redirect) {
		boolean ok = delayTaskService.delete(taskId);
		redirect.addFlashAttribute("message", ok ? "已删除 " + taskId : "删除失败");
		return "redirect:/admin/tasks";
	}

	private TaskRow toRow(DelayTask task) {
		return new TaskRow(task.getId(), task.getBizType(), task.getBizId(), task.getPayload(), task.getDelayMs(),
				task.getStatus().name(), FMT.format(Instant.ofEpochMilli(task.getCreatedAt())),
				FMT.format(Instant.ofEpochMilli(task.getExecuteAt())), task.getCreatedAt(), task.getExecuteAt());
	}

	public record TaskRow(String id, String bizType, String bizId, String payload, long delayMs, String status,
			String createdAt, String executeAt, long createdAtEpoch, long executeAtEpoch) {
	}

}
