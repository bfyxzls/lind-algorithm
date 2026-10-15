<#import "layout.ftl" as ui>
<@ui.layout title="新建压测">
  <#if error??>
    <div class="flash error">${error?html}</div>
  </#if>
  <#if flash??>
    <div class="flash">${flash?html}</div>
  </#if>

  <div class="card">
    <h2>发起 HTTP 压测</h2>
    <p class="muted">填写目标接口后启动任务；任务与结果会保存到 H2。当前运行中：${runningCount!0}</p>
    <form method="post" action="/jobs">
      <div class="grid">
        <label class="full">目标 URL
          <input name="url" value="${form.url!''}" required placeholder="http://127.0.0.1:8080/api/ping"/>
        </label>
        <label>HTTP 方法
          <select name="method">
            <#list ["GET","POST","PUT","PATCH","DELETE","HEAD","OPTIONS"] as m>
              <option value="${m}" <#if (form.method!'GET') == m>selected</#if>>${m}</option>
            </#list>
          </select>
        </label>
        <label>压测模式
          <select name="mode" id="modeSelect">
            <option value="FIXED" <#if (form.mode!'FIXED')?string == "FIXED">selected</#if>>固定并发</option>
            <option value="FIND_MAX" <#if (form.mode!'FIXED')?string == "FIND_MAX">selected</#if>>阶梯找最大并发</option>
          </select>
        </label>
        <label>并发数
          <input type="number" name="concurrency" min="1" value="${form.concurrency!10}"/>
        </label>
        <label>周期（秒）
          <input type="number" name="durationSeconds" min="1" value="${form.durationSeconds!10}"/>
          <span class="hint">FIND_MAX 时表示每个阶梯的秒数</span>
        </label>
        <label>单次超时（ms）
          <input type="number" name="timeoutMs" min="100" value="${form.timeoutMs!5000}"/>
        </label>
        <label class="full">Headers（每行一个，Name:Value 或 Name=Value）
          <textarea name="headersText" placeholder="Authorization:Bearer xxx&#10;X-Trace=1">${form.headersText!''}</textarea>
        </label>
        <label class="full">Query 参数（每行 key=value）
          <textarea name="queryParamsText" placeholder="page=1&#10;size=20">${form.queryParamsText!''}</textarea>
        </label>
        <label class="full">Form 参数（每行 key=value，与 Body 互斥）
          <textarea name="formParamsText" placeholder="username=admin&#10;password=123">${form.formParamsText!''}</textarea>
        </label>
        <label class="full">Body（JSON 等原始文本，与 Form 互斥）
          <textarea name="body" placeholder='{"username":"test"}'>${form.body!''}</textarea>
        </label>
      </div>

      <div class="card find-max-box" id="findMaxBox" data-visible="false" style="margin-top:14px;">
        <h2>阶梯加压参数</h2>
        <div class="grid-3">
          <label>起始并发
            <input type="number" name="startConcurrency" min="1" value="${form.startConcurrency!10}"/>
          </label>
          <label>步进
            <input type="number" name="step" min="1" value="${form.step!10}"/>
          </label>
          <label>最大并发上限
            <input type="number" name="maxConcurrency" min="1" value="${form.maxConcurrency!200}"/>
          </label>
          <label>SLA 成功率 %
            <input type="number" step="0.1" name="minSuccessRatePercent" value="${form.minSuccessRatePercent!99.0}"/>
          </label>
          <label>SLA 最大响应 ms
            <input type="number" name="maxAllowedLatencyMillis" min="1" value="${form.maxAllowedLatencyMillis!3000}"/>
          </label>
        </div>
      </div>

      <div class="card" style="margin-top:14px;">
        <h2>保存为测试用例</h2>
        <div class="grid">
          <label>用例名称
            <input name="caseName" placeholder="例如：登录接口压测"/>
          </label>
          <label>保存到文件夹
            <select name="folderId">
              <#list folders as folder>
                <option value="${folder.id()}" <#if folder.id() == defaultFolderId>selected</#if>>${folder.name()?html}</option>
              </#list>
            </select>
          </label>
          <label class="full">用例说明
            <input name="caseDescription" placeholder="可选"/>
          </label>
        </div>
      </div>

      <div class="actions">
        <button class="btn primary" type="submit">开始压测</button>
        <button class="btn" type="submit" formaction="/cases/save-from-runner">保存为用例</button>
        <a class="btn" href="/cases">用例列表</a>
        <a class="btn" href="/jobs">查看历史</a>
      </div>
    </form>
  </div>

  <#if jobs?? && jobs?size gt 0>
  <div class="card">
    <h2>最近任务</h2>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>状态</th>
          <th>模式</th>
          <th>目标</th>
          <th>创建时间</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <#list jobs as job>
        <tr>
          <td>${job.id()}</td>
          <td><span class="badge ${job.status()}">${job.status()}</span></td>
          <td>${job.mode()}</td>
          <td>${job.config().method()} ${job.config().url()?html}</td>
          <td>${job.createdAt()}</td>
          <td><a href="/jobs/${job.id()}">详情</a></td>
        </tr>
        </#list>
      </tbody>
    </table>
  </div>
  </#if>

  <script>
    (function () {
      var select = document.getElementById('modeSelect');
      var box = document.getElementById('findMaxBox');
      function sync() {
        var findMax = select.value === 'FIND_MAX';
        box.setAttribute('data-visible', findMax ? 'true' : 'false');
        box.style.display = findMax ? 'block' : 'none';
      }
      select.addEventListener('change', sync);
      sync();
    })();
  </script>
</@ui.layout>
