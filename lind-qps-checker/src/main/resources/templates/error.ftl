<#import "layout.ftl" as ui>
<@ui.layout title="错误">
  <div class="flash error">
    <#if status??>${status} · </#if>${(error!'未知错误')?html}
  </div>
  <a class="btn" href="/">返回首页</a>
</@ui.layout>
