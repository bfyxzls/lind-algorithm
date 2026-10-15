<#import "layout.ftl" as ui>
<#assign pageTitle = editing?string('编辑用例', '新建用例')>
<@ui.layout title=pageTitle>
  <#if error??>
    <div class="flash error">${error?html}</div>
  </#if>

  <div class="card">
    <h2>${pageTitle}</h2>
    <form method="post" action="/cases">
      <#if form.id?? && form.id?has_content>
        <input type="hidden" name="id" value="${form.id}"/>
      </#if>
      <div class="grid">
        <label>用例名称
          <input name="name" required value="${form.name!''}" placeholder="例如：登录接口-固定并发"/>
        </label>
        <label>所属文件夹
          <select name="folderId" required>
            <#list folders as folder>
              <option value="${folder.id()}" <#if (form.folderId!'') == folder.id()>selected</#if>>${folder.name()?html}</option>
            </#list>
          </select>
        </label>
        <label class="full">说明
          <input name="description" value="${form.description!''}" placeholder="可选"/>
        </label>
        <label class="full">目标 URL
          <input name="url" value="${form.url!''}" required/>
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
        </label>
        <label>单次超时（ms）
          <input type="number" name="timeoutMs" min="100" value="${form.timeoutMs!5000}"/>
        </label>
        <label class="full">Headers
          <textarea name="headersText">${form.headersText!''}</textarea>
        </label>
        <label class="full">Query 参数
          <textarea name="queryParamsText">${form.queryParamsText!''}</textarea>
        </label>
        <label class="full">Form 参数
          <textarea name="formParamsText">${form.formParamsText!''}</textarea>
        </label>
        <label class="full">Body
          <textarea name="body">${form.body!''}</textarea>
        </label>
      </div>

      <div class="card find-max-box" id="findMaxBox" style="margin-top:14px;display:none;">
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

      <div class="actions">
        <button class="btn primary" type="submit">保存用例</button>
        <a class="btn" href="/cases?folderId=${form.folderId!''}">返回列表</a>
      </div>
    </form>
  </div>

  <script>
    (function () {
      var select = document.getElementById('modeSelect');
      var box = document.getElementById('findMaxBox');
      function sync() {
        box.style.display = select.value === 'FIND_MAX' ? 'block' : 'none';
      }
      select.addEventListener('change', sync);
      sync();
    })();
  </script>
</@ui.layout>
