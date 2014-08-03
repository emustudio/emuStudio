#!/usr/bin/env python

import sys
import os
import os.path
import shutil
import xml.dom.minidom
from xml.dom.minidom import getDOMImplementation
from xml.dom.minidom import parseString
from subprocess import call

def get_vars():
    errorMsg = ""

    travisSecurityVars = os.environ["TRAVIS_SECURE_ENV_VARS"]
    if travisSecurityVars == "false":
        errorMsg = "\nNo secure env vars available; "

    masterPassword = os.getenv("MASTER_PASSWORD", "false")
    if masterPassword == "false":
       errorMsg += "\nMaster security password is not set; "

    userName = os.getenv("EMUSTUDIO_USERNAME", "false")
    if userName == "false":
       errorMsg += "\nServer user name is not set; "

    password = os.getenv("EMUSTUDIO_PASSWORD", "false")
    if password == "false":
       errorMsg += "\nServer password is not set"

    if errorMsg != "":
        print errorMsg
        sys.exit(1)

    return (masterPassword, userName, password)

def get_or_create(xmldoc, name, element=None):
    if element == None:
        element = xmldoc
    children = element.getElementsByTagName(name)
    if len(children) == 0:
        children = [xmldoc.createElement(name)]
        element.appendChild(children[0])
    return children[0]

def recreate(xmldoc, name, element=None):
    if element == None:
        element = xmldoc
    children = element.getElementsByTagName(name)
    if len(children) == 0:
        theChild = xmldoc.createElement(name)
        element.appendChild(theChild)
    else:
        theChild = children[0]
        for child in theChild.childNodes:
            theChild.removeChild(child)
    return theChild

def prettify(node):
    return '\n'.join([line for line in node.toprettyxml(indent='  ').split('\n') if line.strip()])

def create_settings_security(path, masterPassword):
    try:
        xmldoc = xml.dom.minidom.parse(path)
    except:
        xmldoc = getDOMImplementation().createDocument(None, "settingsSecurity", None)

    securityElement = get_or_create(xmldoc, "settingsSecurity")
    masterElement = recreate(xmldoc, "master", securityElement)

    securityNode = xmldoc.createTextNode(masterPassword)
    masterElement.appendChild(securityNode)
    return prettify(xmldoc)

def create_settings(path, userName, password):
    try:
        xmldoc = xml.dom.minidom.parse(path)
    except:
        xmldoc = getDOMImplementation().createDocument(None, "settings", None)

    settingsElement = get_or_create(xmldoc, "settings")
    serversElement = get_or_create(xmldoc, "servers", settingsElement)

    for child in serversElement.getElementsByTagName("server"):
        serversElement.removeChild(child)

    serverElement = recreate(xmldoc, "server", serversElement)

    serverIdElement = xmldoc.createElement("id")
    serverUserElement = xmldoc.createElement("username")
    serverPasswordElement = xmldoc.createElement("password")

    serverIdNode = xmldoc.createTextNode("emustudio-repository")
    serverUserNode = xmldoc.createTextNode(userName)
    serverPasswordNode = xmldoc.createTextNode(password)

    serverIdElement.appendChild(serverIdNode)
    serverUserElement.appendChild(serverUserNode)
    serverPasswordElement.appendChild(serverPasswordNode)

    serverElement.appendChild(serverIdElement)
    serverElement.appendChild(serverUserElement)
    serverElement.appendChild(serverPasswordElement)

    # Try to disable strict host checking
    configElement = get_or_create(xmldoc, "configuration", serverElement)
    knownHostsProviderElement = recreate(xmldoc, "knownHostsProvider", configElement)
    knownHostsProviderElement.setAttribute('implementation',"org.apache.maven.wagon.providers.ssh.knownhost.NullKnownHostProvider")

    hostKeyCheckingElement = xmldoc.createElement("hostKeyChecking")
    hostKeyCheckingNode = xmldoc.createTextNode("no")

    hostKeyCheckingElement.appendChild(hostKeyCheckingNode)
    knownHostsProviderElement.appendChild(hostKeyCheckingElement)

    # Turn off interactive mode
    interactiveNode = recreate(xmldoc, "interactiveMode", settingsElement)
    interactiveValue = xmldoc.createTextNode("false")
    interactiveNode.appendChild(interactiveValue)

    return prettify(xmldoc)

def write_file(path, content, mode='w'):
    file = open(path, mode)
    file.write(content)
    file.close()

def backup_or_create(path):
    if os.path.exists(path):
        shutil.copy2(path, path + ".bak")
    else:
        write_file(path, "")


homedir = os.path.expanduser("~")
settingsSecurityPath = homedir + '/.m2/settings-security.xml'
settingsPath = homedir + '/.m2/settings.xml'
knownHostsPath = homedir + "/.ssh/known_hosts"

vars = get_vars()
backup_or_create(settingsSecurityPath)
backup_or_create(settingsPath)

try:
    settingsSecurityXml = create_settings_security(settingsSecurityPath, vars[0])
    settingsXml = create_settings(settingsPath, vars[1], vars[2])

    write_file(settingsSecurityPath, settingsSecurityXml)
    write_file(settingsPath, settingsXml)

    # append sourceforge.net public ssh key fingerprint (if disabling strict host checking doesn't work)
    call(['ssh-keygen', '-R', 'web.sourceforge.net'])
    write_file(
      knownHostsPath,
      "web.sourceforge.net,216.34.181.70 ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA2uifHZbNexw6cXbyg1JnzDitL5VhYs0E65Hk/tLAPmcmm5GuiGeUoI/B0eUSNFsbqzwgwrttjnzKMKiGLN5CWVmlN1IXGGAfLYsQwK6wAu7kYFzkqP4jcwc5Jr9UPRpJdYIK733tSEmzab4qc5Oq8izKQKIaxXNe7FgmL15HjSpatFt9w/ot/CHS78FUAr3j3RwekHCm/jhPeqhlMAgC+jUgNJbFt3DlhDaRMa0NYamVzmX8D47rtmBbEDU3ld6AezWBPUR5Lh7ODOwlfVI58NAf/aYNlmvl2TZiauBCTa7OPYSyXJnIPbQXg6YQlDknNCr0K769EjeIlAfY87Z4tw==\n",
      'a'
    )
except:
    print "Unexpected error occured"
    pass

