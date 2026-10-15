package com.lind.qps.web.job;

import com.lind.qps.web.catalog.CaseCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * FreeMarker 页面：发起压测、查看任务与历史。
 */
@Controller
public class LoadJobController {

	private final LoadJobService loadJobService;

	private final CaseCatalogService catalogService;

	public LoadJobController(LoadJobService loadJobService, CaseCatalogService catalogService) {
		this.loadJobService = loadJobService;
		this.catalogService = catalogService;
	}

	@GetMapping({ "/", "/index" })
	public String index(Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new LoadForm());
		}
		model.addAttribute("jobs", loadJobService.list());
		model.addAttribute("runningCount", loadJobService.runningCount());
		model.addAttribute("modes", LoadMode.values());
		model.addAttribute("folders", catalogService.listFolders());
		model.addAttribute("defaultFolderId", catalogService.defaultFolderId());
		return "index";
	}

	@PostMapping("/jobs")
	public String start(@ModelAttribute("form") LoadForm form, RedirectAttributes redirectAttributes) {
		try {
			LoadJob job = loadJobService.submit(form);
			redirectAttributes.addFlashAttribute("flash", "压测任务已启动：" + job.id());
			return "redirect:/jobs/" + job.id();
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("form", form);
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/";
		}
	}

	@GetMapping("/jobs")
	public String history(Model model) {
		model.addAttribute("jobs", loadJobService.list());
		model.addAttribute("runningCount", loadJobService.runningCount());
		return "history";
	}

	@GetMapping("/jobs/{id}")
	public String detail(@PathVariable String id, Model model) {
		LoadJob job = loadJobService.find(id).orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
		model.addAttribute("job", job);
		model.addAttribute("view", LoadJobViews.toMap(job));
		model.addAttribute("running", job.status() == LoadJobStatus.RUNNING);
		return "job";
	}

}
