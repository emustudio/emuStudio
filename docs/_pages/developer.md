---
layout: default
title: Developer documentation
permalink: /docs/developer/
---

Here you can find developer documentation of the following emuStudio versions:

{% for version in site.data.global.userdocs %}
- [emuStudio {{ version }}]({{ base }}/docs/developer/{{version}})
{% endfor %}
