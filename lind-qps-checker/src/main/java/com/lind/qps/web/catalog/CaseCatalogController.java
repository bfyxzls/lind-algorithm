package com.lind.qps.web.catalog;

import com.lind.qps.web.job.LoadForm;
import com.lind.qps.web.job.LoadJob;
import com.lind.qps.web.job.LoadJobService;
import com.lind.qps.web.job.LoadMode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 文件夹 / 测试用例管理页。
 */
@Controller
public class CaseCatalogController {

	private final CaseCatalogService catalogService;

	private final LoadJobService loadJobService;

	public CaseCatalogController(CaseCatalogService catalogService, LoadJobService loadJobService) {
		this.catalogService = catalogService;
		this.loadJobService = loadJobService;
	}

	@GetMapping("/cases")
	public String list(@RequestParam(value = "folderId", required = false) String folderId, Model model) {
		var folders = catalogService.listFolders();
		if (folderId == null || folderId.isBlank()) {
			folderId = catalogService.defaultFolderId();
		}
		String currentFolderId = folderId;
		String currentFolderName = catalogService.findFolder(currentFolderId).map(TestFolder::name).orElse("用例");
		model.addAttribute("folders", folders);
		model.addAttribute("currentFolderId", currentFolderId);
		model.addAttribute("currentFolderName", currentFolderName);
		model.addAttribute("cases", catalogService.listCases(currentFolderId));
		model.addAttribute("allCases", catalogService.listCases(null));
		return "case-list";
	}

	@PostMapping("/folders")
	public String createFolder(@RequestParam("name") String name,
			@RequestParam(value = "parentId", required = false) String parentId,
			RedirectAttributes redirectAttributes) {
		try {
			TestFolder folder = catalogService.createFolder(name, parentId);
			redirectAttributes.addFlashAttribute("flash", "已创建文件夹：" + folder.name());
			return "redirect:/cases?folderId=" + folder.id();
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/cases";
		}
	}

	@PostMapping("/folders/{id}/rename")
	public String renameFolder(@PathVariable String id, @RequestParam("name") String name,
			RedirectAttributes redirectAttributes) {
		try {
			catalogService.renameFolder(id, name);
			redirectAttributes.addFlashAttribute("flash", "文件夹已重命名");
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/cases?folderId=" + id;
	}

	@PostMapping("/folders/{id}/delete")
	public String deleteFolder(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			catalogService.deleteFolder(id);
			redirectAttributes.addFlashAttribute("flash", "文件夹已删除");
			return "redirect:/cases";
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/cases?folderId=" + id;
		}
	}

	@GetMapping("/cases/new")
	public String createForm(@RequestParam(value = "folderId", required = false) String folderId, Model model) {
		CaseForm form = new CaseForm();
		form.setFolderId(folderId == null || folderId.isBlank() ? catalogService.defaultFolderId() : folderId);
		model.addAttribute("form", form);
		model.addAttribute("folders", catalogService.listFolders());
		model.addAttribute("modes", LoadMode.values());
		model.addAttribute("editing", false);
		return "case-form";
	}

	@GetMapping("/cases/{id}/edit")
	public String editForm(@PathVariable String id, Model model) {
		TestCase testCase = catalogService.findCase(id).orElseThrow(() -> new IllegalArgumentException("用例不存在"));
		model.addAttribute("form", CaseForm.from(testCase));
		model.addAttribute("folders", catalogService.listFolders());
		model.addAttribute("modes", LoadMode.values());
		model.addAttribute("editing", true);
		return "case-form";
	}

	@PostMapping("/cases/import-curl")
	public String importCurl(@RequestParam("folderId") String folderId, @RequestParam("curl") String curl,
			@RequestParam(value = "name", required = false) String name, RedirectAttributes redirectAttributes) {
		try {
			CurlImporter.Result parsed = CurlImporter.parse(curl);
			String caseName = (name == null || name.isBlank()) ? parsed.suggestedName() : name.trim();
			TestCase saved = catalogService.saveCase(null, folderId, caseName, "从 curl 导入", parsed.form());
			redirectAttributes.addFlashAttribute("flash", "已从 curl 导入用例：" + saved.name());
			return "redirect:/cases?folderId=" + saved.folderId();
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/cases?folderId=" + nullToEmpty(folderId);
		}
	}

	@PostMapping("/cases")
	public String save(@ModelAttribute("form") CaseForm form, RedirectAttributes redirectAttributes) {
		try {
			TestCase saved = catalogService.saveCase(form.getId(), form.getFolderId(), form.getName(),
					form.getDescription(), form);
			redirectAttributes.addFlashAttribute("flash", "用例已保存：" + saved.name());
			return "redirect:/cases?folderId=" + saved.folderId();
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("form", form);
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			if (form.getId() == null || form.getId().isBlank()) {
				return "redirect:/cases/new?folderId=" + nullToEmpty(form.getFolderId());
			}
			return "redirect:/cases/" + form.getId() + "/edit";
		}
	}

	@PostMapping("/cases/{id}/delete")
	public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			TestCase testCase = catalogService.findCase(id).orElseThrow(() -> new IllegalArgumentException("用例不存在"));
			String folderId = testCase.folderId();
			catalogService.deleteCase(id);
			redirectAttributes.addFlashAttribute("flash", "用例已删除");
			return "redirect:/cases?folderId=" + folderId;
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/cases";
		}
	}

	@GetMapping("/cases/{id}/load")
	public String loadToRunner(@PathVariable String id, RedirectAttributes redirectAttributes) {
		TestCase testCase = catalogService.findCase(id).orElseThrow(() -> new IllegalArgumentException("用例不存在"));
		redirectAttributes.addFlashAttribute("form", testCase.toLoadForm());
		redirectAttributes.addFlashAttribute("flash", "已加载用例：" + testCase.name());
		return "redirect:/";
	}

	@PostMapping("/cases/{id}/run")
	public String run(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			TestCase testCase = catalogService.findCase(id).orElseThrow(() -> new IllegalArgumentException("用例不存在"));
			LoadJob job = loadJobService.submit(testCase.toLoadForm());
			redirectAttributes.addFlashAttribute("flash", "已从用例启动压测：" + testCase.name());
			return "redirect:/jobs/" + job.id();
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/cases";
		}
	}

	@PostMapping("/cases/save-from-runner")
	public String saveFromRunner(@ModelAttribute("form") LoadForm form, @RequestParam("caseName") String caseName,
			@RequestParam(value = "caseDescription", required = false) String description,
			@RequestParam("folderId") String folderId, RedirectAttributes redirectAttributes) {
		try {
			TestCase saved = catalogService.saveCase(null, folderId, caseName, description, form);
			redirectAttributes.addFlashAttribute("flash", "已保存为用例：" + saved.name());
			return "redirect:/cases?folderId=" + saved.folderId();
		}
		catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("form", form);
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/";
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

}
