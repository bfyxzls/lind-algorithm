<#import "layout.ftl" as ui>
<@ui.layout title="历史任务">
  <div class="card">
    <h2>历史任务</h2>
    <p class="muted">已持久化到 H2（resources/h2）。当前运行中：${runningCount!0}</p>
    <#if jobs?size == 0>
      <p class="muted">暂无任务，去 <a href="/">新建压测</a>。</p>
    <#else>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>状态</th>
            <th>模式</th>
            <th>目标</th>
            <th>摘要</th>
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
            <td>${(job.message()!'')?html}</td>
            <td>${job.createdAt()}</td>
            <td><a href="/jobs/${job.id()}">详情</a></td>
          </tr>
          </#list>
        </tbody>
      </table>
    </#if>
  </div>
</@ui.layout>
