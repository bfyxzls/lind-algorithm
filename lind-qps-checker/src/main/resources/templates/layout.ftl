<#macro layout title>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>${title} · QPS Checker</title>
  <link rel="stylesheet" href="/css/app.css"/>
</head>
<body>
  <div class="top">
    <h1>QPS Checker</h1>
    <div class="nav">
      <a class="btn" href="/">新建压测</a>
      <a class="btn" href="/cases">测试用例</a>
      <a class="btn" href="/jobs">历史任务</a>
    </div>
  </div>
  <#nested>
</body>
</html>
</#macro>
