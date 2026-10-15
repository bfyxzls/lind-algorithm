<#import "layout.ftl" as ui>
<@ui.layout title="测试用例">
  <#if error??>
    <div class="flash error">${error?html}</div>
  </#if>
  <#if flash??>
    <div class="flash">${flash?html}</div>
  </#if>

  <div class="workspace">
    <aside class="folder-pane">
      <div class="pane-head">
        <h2>文件夹</h2>
        <button type="button" class="btn small" id="btn-new-folder" title="新建文件夹">新建</button>
      </div>
      <nav class="folder-nav" aria-label="文件夹列表">
        <#list folders as folder>
          <div class="folder-row <#if folder.id() == currentFolderId>is-active</#if>">
            <a class="folder-item <#if folder.id() == currentFolderId>active</#if>"
               href="/cases?folderId=${folder.id()}"
               aria-current="<#if folder.id() == currentFolderId>page<#else>false</#if>">
              <span class="folder-marker" aria-hidden="true"></span>
              <span class="folder-name">${folder.name()?html}</span>
              <#if folder.id() == currentFolderId>
                <span class="folder-count">${cases?size}</span>
              </#if>
            </a>
            <#if folder.id() == currentFolderId>
              <button type="button"
                      class="btn icon-btn folder-manage"
                      id="btn-manage-folder"
                      title="管理文件夹"
                      data-folder-id="${folder.id()}"
                      data-folder-name="${folder.name()?html}">⋯</button>
            </#if>
          </div>
        </#list>
      </nav>
      <#if currentFolderId??>
      <div class="folder-footer">
        <button type="button" class="btn small full" id="btn-import-curl"
                data-folder-id="${currentFolderId}"
                title="粘贴 curl 命令导入为用例">导入 curl</button>
      </div>
      </#if>
    </aside>

    <main class="case-pane">
      <div class="pane-head">
        <div>
          <h2>${(currentFolderName!'用例')?html}</h2>
          <p class="muted">当前文件夹 ${cases?size} 条 · 全部 ${allCases?size} 条</p>
        </div>
        <a class="btn primary" href="/cases/new?folderId=${currentFolderId!''}">新建用例</a>
      </div>

      <#if cases?size == 0>
        <div class="empty-state">
          <p>当前文件夹暂无用例</p>
          <a class="btn primary" href="/cases/new?folderId=${currentFolderId!''}">新建第一条用例</a>
        </div>
      <#else>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>方法</th>
                <th>URL</th>
                <th>模式</th>
                <th>并发</th>
                <th>更新时间</th>
                <th class="ops-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <#list cases as c>
                <tr>
                  <td>
                    <strong>${c.name()?html}</strong>
                    <#if (c.description()!'')?has_content>
                      <div class="muted">${c.description()?html}</div>
                    </#if>
                  </td>
                  <td><span class="tag">${c.method()}</span></td>
                  <td class="url-cell">${c.url()?html}</td>
                  <td>${c.mode()}</td>
                  <td>${c.concurrency()}</td>
                  <td class="muted">${c.updatedAt()}</td>
                  <td class="ops ops-col">
                    <a class="btn small" href="/cases/${c.id()}/load">加载</a>
                    <form class="inline" method="post" action="/cases/${c.id()}/run">
                      <button class="btn small primary" type="submit">运行</button>
                    </form>
                    <a class="btn small" href="/cases/${c.id()}/edit">编辑</a>
                    <form class="inline" method="post" action="/cases/${c.id()}/delete"
                          onsubmit="return confirm('确认删除该用例？');">
                      <button class="btn small danger" type="submit">删除</button>
                    </form>
                  </td>
                </tr>
              </#list>
            </tbody>
          </table>
        </div>
      </#if>
    </main>
  </div>

  <#-- 新建文件夹 -->
  <div class="modal" id="modal-new-folder" hidden>
    <div class="modal-backdrop" data-close-modal></div>
    <div class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="modal-new-title">
      <div class="modal-head">
        <h3 id="modal-new-title">新建文件夹</h3>
        <button type="button" class="btn icon-btn" data-close-modal aria-label="关闭">×</button>
      </div>
      <form method="post" action="/folders" class="modal-body stack">
        <label>
          名称
          <input name="name" required maxlength="128" placeholder="例如：订单接口" autofocus/>
        </label>
        <input type="hidden" name="parentId" value=""/>
        <div class="modal-actions">
          <button type="button" class="btn" data-close-modal>取消</button>
          <button class="btn primary" type="submit">创建</button>
        </div>
      </form>
    </div>
  </div>

  <#-- 从 curl 导入用例 -->
  <div class="modal" id="modal-import-curl" hidden>
    <div class="modal-backdrop" data-close-modal></div>
    <div class="modal-panel modal-panel-lg" role="dialog" aria-modal="true" aria-labelledby="modal-curl-title">
      <div class="modal-head">
        <h3 id="modal-curl-title">导入 curl</h3>
        <button type="button" class="btn icon-btn" data-close-modal aria-label="关闭">×</button>
      </div>
      <form method="post" action="/cases/import-curl" class="modal-body stack">
        <input type="hidden" name="folderId" id="input-curl-folder-id" value="${currentFolderId!''}"/>
        <label>
          用例名称 <span class="muted">（可空，默认按 URL 生成）</span>
          <input name="name" maxlength="128" placeholder="例如：创建订单"/>
        </label>
        <label>
          curl 命令
          <textarea name="curl" required rows="12"
                    placeholder="粘贴浏览器 DevTools「Copy as cURL」或终端 curl 命令"></textarea>
        </label>
        <p class="hint">将解析 URL、Method、Header、Query、Body / Form，并保存到当前文件夹。</p>
        <div class="modal-actions">
          <button type="button" class="btn" data-close-modal>取消</button>
          <button class="btn primary" type="submit">导入为用例</button>
        </div>
      </form>
    </div>
  </div>

  <#-- 管理文件夹：重命名 / 删除 -->
  <div class="modal" id="modal-manage-folder" hidden>
    <div class="modal-backdrop" data-close-modal></div>
    <div class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="modal-manage-title">
      <div class="modal-head">
        <h3 id="modal-manage-title">管理文件夹</h3>
        <button type="button" class="btn icon-btn" data-close-modal aria-label="关闭">×</button>
      </div>
      <div class="modal-body stack">
        <form method="post" id="form-rename-folder" class="stack">
          <label>
            重命名
            <input name="name" id="input-rename-folder" required maxlength="128"/>
          </label>
          <div class="modal-actions">
            <button class="btn primary" type="submit">保存名称</button>
          </div>
        </form>
        <div class="modal-divider"></div>
        <form method="post" id="form-delete-folder"
              onsubmit="return confirm('确认删除该文件夹？文件夹内不能有用例。');">
          <p class="muted">删除后不可恢复。文件夹内仍有用例时无法删除。</p>
          <div class="modal-actions">
            <button class="btn danger" type="submit">删除文件夹</button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <script>
    (function () {
      function openModal(el) {
        el.hidden = false;
        document.body.classList.add('modal-open');
        var focus = el.querySelector('input, button.primary');
        if (focus) focus.focus();
      }
      function closeModal(el) {
        el.hidden = true;
        if (!document.querySelector('.modal:not([hidden])')) {
          document.body.classList.remove('modal-open');
        }
      }
      document.querySelectorAll('[data-close-modal]').forEach(function (node) {
        node.addEventListener('click', function () {
          closeModal(node.closest('.modal'));
        });
      });
      document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') return;
        document.querySelectorAll('.modal:not([hidden])').forEach(closeModal);
      });

      var newModal = document.getElementById('modal-new-folder');
      var manageModal = document.getElementById('modal-manage-folder');
      var curlModal = document.getElementById('modal-import-curl');
      var btnNew = document.getElementById('btn-new-folder');
      var btnManage = document.getElementById('btn-manage-folder');
      var btnCurl = document.getElementById('btn-import-curl');

      if (btnNew) {
        btnNew.addEventListener('click', function () { openModal(newModal); });
      }
      if (btnManage) {
        btnManage.addEventListener('click', function () {
          var id = btnManage.getAttribute('data-folder-id');
          var name = btnManage.getAttribute('data-folder-name') || '';
          document.getElementById('form-rename-folder').action = '/folders/' + id + '/rename';
          document.getElementById('form-delete-folder').action = '/folders/' + id + '/delete';
          document.getElementById('input-rename-folder').value = name;
          openModal(manageModal);
        });
      }
      if (btnCurl) {
        btnCurl.addEventListener('click', function () {
          var id = btnCurl.getAttribute('data-folder-id') || '';
          document.getElementById('input-curl-folder-id').value = id;
          openModal(curlModal);
          var ta = curlModal.querySelector('textarea[name="curl"]');
          if (ta) ta.focus();
        });
      }
    })();
  </script>
</@ui.layout>
