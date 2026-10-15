package com.lind.qps.web.job;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 任务状态 JSON API（页面轮询）。
 */
@RestController
@RequestMapping("/api/jobs")
public class LoadJobApiController {

	private final LoadJobService loadJobService;

	public LoadJobApiController(LoadJobService loadJobService) {
		this.loadJobService = loadJobService;
	}

	@GetMapping
	public Map<String, Object> list() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("runningCount", loadJobService.runningCount());
		List<Map<String, Object>> jobs = loadJobService.list().stream().map(LoadJobViews::toMap)
				.collect(Collectors.toList());
		body.put("jobs", jobs);
		return body;
	}

	@GetMapping("/{id}")
	public Map<String, Object> detail(@PathVariable String id) {
		return loadJobService.find(id).map(LoadJobViews::toMap)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "job not found"));
	}

}
