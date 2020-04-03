---
layout: page
title: Introduction
permalink: /introduction/
---

# Introduction

This guide shall help you, the developer, to write your own virtual computer for emuStudio. API is designed for simplicity and tries to save the developer from solving the most common problems. Created emulators can mimic either real or abstract computers. I hope you will have fun!

There exist some sister projects, which will be used by the developer during your programming journey. The following
section will provide more information. 


## emuLib

emuLib is a run-time library used by emuStudio and plugins. It also provides the API to be implemented by plug-ins.
Javadoc can be opened [here][emulib]{:target="_blank"}.  

## edigen

Edigen is CPU instruction decoder and disassembler generator. It takes the burden of the common disassembling/decoding
code from the developer when programming emulator. The project website, which includes documentation,
is [here][edigen]{:target="_blank"}. 

## Other documentation

For reference, here are provided some documents for some older emuStudio versions.

|---
| Version | Year | Document or paper
|-|-|-
|  | 2017 | [RASP Abstract Machine Emulator — Extending the emuStudio Platform][rasp-2017]{:target="_blank"}
|---
|  | 2012 | [An instruction decoder and disassembler generator for EmuStudio platform][edigen-2012]{:target="_blank"} - Proceeding of the Faculty of Electrical Engineering and Informatics of the Technical University of Košice. Page 660-663. ISBN 978-80-553-0890-6
|---
|  | 2010 | [Preserving host independent emulation speed][cse-2010]{:target="_blank"}
|---
|  | 2010 | [Standardization of computer emulation][standard-2010]{:target="_blank"}
|---
|  | 2010 | [Communication model of emuStuio emulation platform][model-2010]{:target="_blank"}
|---
| 0.37b | 2009 | [User manual (in Slovak)][manual-2009]{:target="_blank"}
|---
| 0.37b | 2009 | [Plugins' vade-mecum (in Slovak)][vademecum-2009]{:target="_blank"}
|---
|  | 2008 | [Software-based CPU emulation][emulation-2008]{:target="_blank"}
|===


[emulib]: {{ site.baseurl }}/emulib_javadoc/
[edigen]: https://github.com/emustudio/edigen
[rasp-2017]: https://www.researchgate.net/publication/320277321_RASP_ABSTRACT_MACHINE_EMULATOR_-_EXTENDING_THE_EMUSTUDIO_PLATFORM
[edigen-2012]: http://people.tuke.sk/dusan.medved/APVV/clanky/Bena4.pdf
[standard-2010]: https://ieeexplore.ieee.org/document/5423733
[model-2010]: https://www.researchgate.net/publication/220482121_Communication_model_of_emuStudio_emulation_platform
[manual-2009]: {{ site.baseurl }}/../../../files/manual-0.37b-draft.pdf
[vademecum-2009]: {{ site.baseurl }}/../../../files/plugins-vademecum-old.pdf
[emulation-2008]: http://www.aei.tuke.sk/papers/2008/4/08_Simonak.pdf
[cse-2010]: {{ site.baseurl }}/../../../files/speed_final_en.pdf
