#!/usr/bin/env python

import os
import os.path
import shutil

def restore_backup(path, postfix = '.bak'):
    if os.path.exists(path + postfix):
        shutil.copy2(path + postfix, path)
        os.remove(path + postfix)

homedir = os.path.expanduser("~")
settingsSecurityPath = homedir + '/.m2/settings-security.xml'
settingsPath = homedir + '/.m2/settings.xml'
knownHostsPath = homedir + "/.ssh/known_hosts"

restore_backup(settingsSecurityPath)
restore_backup(settingsPath)
restore_backup(knownHostsPath, '.old')
