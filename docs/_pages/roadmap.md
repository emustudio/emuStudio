---
layout: roadmap
title: Roadmap
permalink: /roadmap/
---

<div class="jumbotron">
<div class="table-responsive">
  <table class="table">
    <tr>
      <td>Next milestone:</td><th>{{ site.data.global.next_milestone }}</th>
    </tr>
    <tr>
      <td>Open issues:</td><td><span id="issuesOpen" /></td>
    </tr>
    <tr>
      <td>Closed issues:</td><td><span id="issuesClosed"/></td>
    </tr>
    <tr>
      <td>All issues:</td><td><span id="issuesAll" /></td>
    </tr>
  </table>
</div>
</div>



Most of the future plans of emuStudio are represented either by issues [at GitHub](https://github.com/vbmacher/emuStudio/issues),
or they are kind-of ad-hoc. All Issues <em>should be</em> collected into [milestones](https://github.com/vbmacher/emuStudio/milestones).
 
Release dates are not set for any of the milestones. The reason is that it's still a hobby project and the time
allocated for the project is quite low. Contribution is welcome.

# 5 Latest activities

The following list shows 5 latest activities in emuStudio overall.

<div id="feed"></div>

<script>
  GitHubActivity.feed({
    username: "vbmacher",
    repo: "emuStudio",
    selector: "#feed",
    limit: 5, // optional
    milestone: {{ site.data.global.milestone_number }}, // only for issues
    openSelector: "#issuesOpen",
    closedSelector: "#issuesClosed",
    allSelector: "#issuesAll"
  });
</script>