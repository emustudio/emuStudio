#!/usr/bin/env python

# Modified gist https://gist.github.com/neothemachine/4060735

import sys
import os
import os.path
import xml.dom.minidom
 
if os.environ["TRAVIS_SECURE_ENV_VARS"] == "false":
  print "no secure env vars available, skipping deployment"
  sys.exit()
 
homedir = os.path.expanduser("~")
 
m2 = xml.dom.minidom.parse(homedir + '/.m2/settings.xml')
settings = m2.getElementsByTagName("settings")[0]
 
serversNodes = settings.getElementsByTagName("servers")
if not serversNodes:
  serversNode = m2.createElement("servers")
  settings.appendChild(serversNode)
else:
  serversNode = serversNodes[0]

repositoryServerNode = m2.createElement("server")
repositoryServerId = m2.createElement("id")
repositoryServerUser = m2.createElement("username")
repositoryServerPass = m2.createElement("password")
 
idNode = m2.createTextNode("emustudio-repository")
userNode = m2.createTextNode(os.environ["EMUSTUDIO_USERNAME"])
passNode = m2.createTextNode(os.environ["EMUSTUDIO_PASSWORD"])
 
repositoryServerId.appendChild(idNode)
repositoryServerUser.appendChild(userNode)
repositoryServerPass.appendChild(passNode)

# Try to disable strict host checking

configNode = m2.createElement("configuration")
knownHostsNode = m2.createElement("knownHostsProvider")
configNode.appendChild(knownHostsNode)
knownHostsNode.setAttribute('implementation',"org.apache.maven.wagon.providers.ssh.knownhost.NullKnownHostProvider")
disableHostNode = m2.createElement("hostKeyChecking")
disableHostValue = m2.createTextNode("no")
disableHostNode.appendChild(disableHostValue)
knownHostsNode.appendChild(disableHostNode)
 
repositoryServerNode.appendChild(repositoryServerId)
repositoryServerNode.appendChild(repositoryServerUser)
repositoryServerNode.appendChild(repositoryServerPass)
repositoryServerNode.appendChild(configNode)
 
serversNode.appendChild(repositoryServerNode)

# Turn off interactive mode
interactiveNode = m2.createElement("interactiveMode")
settings.appendChild(interactiveNode)
interactiveValue = m2.createTextNode("false")
interactiveNode.appendChild(interactiveValue)
  
m2Str = m2.toxml()
f = open(homedir + '/.m2/mySettings.xml', 'w')
f.write(m2Str)
f.close()

# append sourceforge.net public ssh key fingerprint (since disabling strict host checking doesn't work)
with open(homedir + "/.ssh/known_hosts", "a") as knownHostsFile:
    knownHostsFile.write("web.sourceforge.net,216.34.181.70 ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA2uifHZbNexw6cXbyg1JnzDitL5VhYs0E65Hk/tLAPmcmm5GuiGeUoI/B0eUSNFsbqzwgwrttjnzKMKiGLN5CWVmlN1IXGGAfLYsQwK6wAu7kYFzkqP4jcwc5Jr9UPRpJdYIK733tSEmzab4qc5Oq8izKQKIaxXNe7FgmL15HjSpatFt9w/ot/CHS78FUAr3j3RwekHCm/jhPeqhlMAgC+jUgNJbFt3DlhDaRMa0NYamVzmX8D47rtmBbEDU3ld6AezWBPUR5Lh7ODOwlfVI58NAf/aYNlmvl2TZiauBCTa7OPYSyXJnIPbQXg6YQlDknNCr0K769EjeIlAfY87Z4tw==")