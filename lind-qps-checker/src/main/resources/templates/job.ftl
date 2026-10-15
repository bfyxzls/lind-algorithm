<#import "layout.ftl" as ui>
<@ui.layout title="任务详情">
  <#if flash??>
    <div class="flash">${flash?html}</div>
  </#if>

  <div class="card">
    <h2>任务 ${job.id()}</h2>
    <p>
      状态：<span id="statusBadge" class="badge ${job.status()}">${job.status()}</span>
      · 模式：${job.mode()}
      · 目标：${job.config().method()} ${job.config().url()?html}
    </p>
    <p class="muted">创建：${job.createdAt()} · 完成：<span id="finishedAt">${job.finishedAt()!'-'}</span></p>
    <p id="message">${(job.message()!'')?html}</p>
    <#if running>
      <p class="muted" id="runningHint">压测进行中，页面将自动刷新状态…</p>
    </#if>
  </div>

  <div class="card" id="reportCard" <#if !job.report()??>style="display:none"</#if>>
    <h2>指标摘要</h2>
    <div class="metrics" id="metrics">
      <#if job.report()??>
        <#assign r = job.report()>
        <div class="metric"><div class="k">QPS</div><div class="v">${r.qps()?string["0.##"]}</div></div>
        <div class="metric"><div class="k">成功率</div><div class="v">${r.successRate()?string["0.##"]}%</div></div>
        <div class="metric"><div class="k">总请求</div><div class="v">${r.totalRequests()}</div></div>
        <div class="metric"><div class="k">并发</div><div class="v">${r.concurrency()}</div></div>
        <div class="metric"><div class="k">平均 RT</div><div class="v">${r.avgLatencyMillis()} ms</div></div>
        <div class="metric"><div class="k">最大 RT</div><div class="v">${r.maxLatencyMillis()} ms</div></div>
        <div class="metric"><div class="k">P95</div><div class="v">${r.p95LatencyMillis()} ms</div></div>
        <div class="metric"><div class="k">P99</div><div class="v">${r.p99LatencyMillis()} ms</div></div>
      </#if>
    </div>
    <pre class="report" id="reportText"><#if view.reportText??>${view.reportText?html}</#if></pre>
  </div>

  <div class="card" id="failureCard" <#if !(job.report()??) || job.report().failureSamples()?size == 0>style="display:none"</#if>>
    <h2>失败样本（含 HTTP 状态码与响应体）</h2>
    <p class="muted">最多保留 20 条，避免高失败率时占用过多内存。</p>
    <table id="failureTable">
      <thead>
        <tr><th>#</th><th>HTTP 状态</th><th>耗时</th><th>错误信息</th><th>响应体</th></tr>
      </thead>
      <tbody>
        <#if job.report()??>
          <#list job.report().failureSamples() as sample>
            <tr>
              <td>${sample?index + 1}</td>
              <td>${sample.statusCode()}</td>
              <td>${sample.latencyMillis()} ms</td>
              <td>${(sample.errorMessage()!'')?html}</td>
              <td><pre class="report" style="margin:0;max-height:160px;">${(sample.responseBody()!'')?html}</pre></td>
            </tr>
          </#list>
        </#if>
      </tbody>
    </table>
  </div>

  <div class="card" id="capacityCard" <#if !view.capacity??>style="display:none"</#if>>
    <h2>阶梯加压明细</h2>
    <pre class="report" id="capacityText"><#if view.capacity??>${view.capacity.text?html}</#if></pre>
    <table id="stepTable">
      <thead>
        <tr><th>并发</th><th>成功率</th><th>QPS</th><th>最大RT</th><th>总请求</th></tr>
      </thead>
      <tbody>
        <#if view.capacity??>
          <#list view.capacity.steps as s>
            <tr>
              <td>${s.concurrency}</td>
              <td>${s.successRate?string["0.##"]}%</td>
              <td>${s.qps?string["0.##"]}</td>
              <td>${s.maxLatencyMillis} ms</td>
              <td>${s.totalRequests}</td>
            </tr>
          </#list>
        </#if>
      </tbody>
    </table>
  </div>

  <div class="actions">
    <a class="btn" href="/">再测一次</a>
    <a class="btn" href="/jobs">历史任务</a>
  </div>

  <script>
    (function () {
      var jobId = "${job.id()}";
      var running = ${running?c};
      if (!running) return;

      function fmt(n, digits) {
        if (n == null) return '-';
        return Number(n).toFixed(digits == null ? 2 : digits);
      }
      function escapeHtml(text) {
        return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      }

      function render(data) {
        var badge = document.getElementById('statusBadge');
        badge.className = 'badge ' + data.status;
        badge.textContent = data.status;
        document.getElementById('message').textContent = data.message || '';
        document.getElementById('finishedAt').textContent = data.finishedAt || '-';
        if (data.report) {
          document.getElementById('reportCard').style.display = 'block';
          var r = data.report;
          document.getElementById('metrics').innerHTML =
            '<div class="metric"><div class="k">QPS</div><div class="v">' + fmt(r.qps) + '</div></div>' +
            '<div class="metric"><div class="k">成功率</div><div class="v">' + fmt(r.successRate) + '%</div></div>' +
            '<div class="metric"><div class="k">总请求</div><div class="v">' + r.totalRequests + '</div></div>' +
            '<div class="metric"><div class="k">并发</div><div class="v">' + r.concurrency + '</div></div>' +
            '<div class="metric"><div class="k">平均 RT</div><div class="v">' + r.avgLatencyMillis + ' ms</div></div>' +
            '<div class="metric"><div class="k">最大 RT</div><div class="v">' + r.maxLatencyMillis + ' ms</div></div>' +
            '<div class="metric"><div class="k">P95</div><div class="v">' + r.p95LatencyMillis + ' ms</div></div>' +
            '<div class="metric"><div class="k">P99</div><div class="v">' + r.p99LatencyMillis + ' ms</div></div>';
          document.getElementById('reportText').textContent = data.reportText || '';
          var failures = r.failureSamples || [];
          var failureCard = document.getElementById('failureCard');
          if (failures.length > 0) {
            failureCard.style.display = 'block';
            document.querySelector('#failureTable tbody').innerHTML = failures.map(function (s, i) {
              return '<tr><td>' + (i + 1) + '</td><td>' + s.statusCode + '</td><td>' + s.latencyMillis +
                ' ms</td><td>' + escapeHtml(s.errorMessage || '') + '</td><td><pre class="report" style="margin:0;max-height:160px;">' +
                escapeHtml(s.responseBody || '') + '</pre></td></tr>';
            }).join('');
          } else {
            failureCard.style.display = 'none';
          }
        }
        if (data.capacity) {
          document.getElementById('capacityCard').style.display = 'block';
          document.getElementById('capacityText').textContent = data.capacity.text || '';
          var rows = (data.capacity.steps || []).map(function (s) {
            return '<tr><td>' + s.concurrency + '</td><td>' + fmt(s.successRate) + '%</td><td>' +
              fmt(s.qps) + '</td><td>' + s.maxLatencyMillis + ' ms</td><td>' + s.totalRequests + '</td></tr>';
          }).join('');
          document.querySelector('#stepTable tbody').innerHTML = rows;
        }
        if (data.status === 'SUCCESS' || data.status === 'FAILED') {
          var hint = document.getElementById('runningHint');
          if (hint) hint.style.display = 'none';
          clearInterval(timer);
        }
      }

      var timer = setInterval(function () {
        fetch('/api/jobs/' + jobId).then(function (r) { return r.json(); }).then(render).catch(function () {});
      }, 1500);
    })();
  </script>
</@ui.layout>
