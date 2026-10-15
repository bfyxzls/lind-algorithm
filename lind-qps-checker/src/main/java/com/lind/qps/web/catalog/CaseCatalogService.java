package com.lind.qps.web.catalog;

import com.lind.qps.web.job.LoadForm;
import com.lind.qps.web.job.LoadFormMapper;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件夹 + 测试用例目录服务。
 */
@Service
public class CaseCatalogService {

	public static final String DEFAULT_FOLDER_NAME = "默认文件夹";

	private final JdbcFolderStore folderStore;

	private final JdbcCaseStore caseStore;

	public CaseCatalogService(JdbcFolderStore folderStore, JdbcCaseStore caseStore) {
		this.folderStore = folderStore;
		this.caseStore = caseStore;
	}

	@PostConstruct
	void ensureDefaultFolder() {
		if (folderStore.countAll() == 0) {
			folderStore.insert(new TestFolder(JdbcFolderStore.newId(), DEFAULT_FOLDER_NAME, null));
		}
	}

	public List<TestFolder> listFolders() {
		return folderStore.findAll();
	}

	public Optional<TestFolder> findFolder(String id) {
		return folderStore.findById(id);
	}

	public TestFolder createFolder(String name, String parentId) {
		String trimmed = requireName(name, "文件夹名称");
		if (parentId != null && !parentId.isBlank()) {
			folderStore.findById(parentId).orElseThrow(() -> new IllegalArgumentException("父文件夹不存在"));
		}
		else {
			parentId = null;
		}
		TestFolder folder = new TestFolder(JdbcFolderStore.newId(), trimmed, parentId);
		folderStore.insert(folder);
		return folder;
	}

	public TestFolder renameFolder(String id, String name) {
		TestFolder folder = folderStore.findById(id).orElseThrow(() -> new IllegalArgumentException("文件夹不存在"));
		folder.name(requireName(name, "文件夹名称"));
		folderStore.update(folder);
		return folder;
	}

	@Transactional
	public void deleteFolder(String id) {
		TestFolder folder = folderStore.findById(id).orElseThrow(() -> new IllegalArgumentException("文件夹不存在"));
		if (folderStore.countChildren(id) > 0) {
			throw new IllegalArgumentException("请先删除子文件夹");
		}
		if (caseStore.countByFolder(id) > 0) {
			throw new IllegalArgumentException("文件夹内仍有测试用例，无法删除");
		}
		if (folderStore.countAll() <= 1) {
			throw new IllegalArgumentException("至少保留一个文件夹");
		}
		folderStore.delete(folder.id());
	}

	public List<TestCase> listCases(String folderId) {
		if (folderId == null || folderId.isBlank()) {
			return caseStore.findAll();
		}
		return caseStore.findByFolder(folderId);
	}

	public Optional<TestCase> findCase(String id) {
		return caseStore.findById(id);
	}

	public TestCase saveCase(String id, String folderId, String name, String description, LoadForm form) {
		Objects.requireNonNull(form, "form");
		String folder = requireFolder(folderId);
		String caseName = requireName(name, "用例名称");
		LoadFormMapper.toLoadConfig(form); // validate
		if (id == null || id.isBlank()) {
			TestCase created = TestCase.fromLoadForm(JdbcCaseStore.newId(), folder, caseName, description, form);
			caseStore.insert(created);
			return created;
		}
		TestCase existing = caseStore.findById(id).orElseThrow(() -> new IllegalArgumentException("用例不存在"));
		existing.folderId(folder);
		existing.name(caseName);
		existing.description(description);
		existing.applyForm(form);
		caseStore.update(existing);
		return existing;
	}

	public void deleteCase(String id) {
		caseStore.findById(id).orElseThrow(() -> new IllegalArgumentException("用例不存在"));
		caseStore.delete(id);
	}

	public String defaultFolderId() {
		return folderStore.findAll().stream().findFirst().map(TestFolder::id)
				.orElseThrow(() -> new IllegalStateException("无可用文件夹"));
	}

	private String requireFolder(String folderId) {
		if (folderId == null || folderId.isBlank()) {
			throw new IllegalArgumentException("请选择文件夹");
		}
		folderStore.findById(folderId).orElseThrow(() -> new IllegalArgumentException("文件夹不存在"));
		return folderId;
	}

	private static String requireName(String name, String label) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException(label + "不能为空");
		}
		return name.trim();
	}

}
