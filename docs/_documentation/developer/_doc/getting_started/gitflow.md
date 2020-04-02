---
layout: default
title: Git workflow
nav_order: 2
parent: Getting started
permalink: /getting_started/gitflow
---

# Git workflow

Since emuStudio is available at GitHub, it using `git` as the version control system (VCS).

Basic workflow is a much simplified version of the official [Git Flow model][gitflow]{:target="_blank"}.
The simplifications are as follows:

- there is maintained just one version of emuStudio (the not-yet released). Releasing older versions with hotfixes is
  not supported
- there are no "release branches". Branch `development` is considered as always-stable branch, from which are performed
  releases.
  
## Releases

Release is performed in several steps.

0. Pre-check everything. Build, documentation, etc.
1. Merge `development` branch into `master` branch
2. Last commit in the `master` branch is tagged with tag `RELEASE-XXX`, where `XXX` is the released version
3. Release is edited in GitHub to add release notes, and binary artefacts


[gitflow]: https://datasift.github.io/gitflow/IntroducingGitFlow.html
