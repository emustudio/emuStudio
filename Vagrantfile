# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "hashicorp/precise32"
  config.vm.synced_folder "release/target/", "/emustudio"
  config.ssh.forward_x11 = true

  config.vm.provision "shell", inline: <<-SHELL
    if ! which java > /dev/null; then
      sudo add-apt-repository ppa:webupd8team/java
      sudo apt-get update
      sudo apt-get install -y xauth software-properties-common python-software-properties
      sudo apt-get install -y libxrender1 libxtst6 libxi6
      sudo echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
      sudo apt-get install -y oracle-java8-installer
    fi

    tar xzvf /emustudio/emuStudio*.tar.gz 
    chown -R vagrant:vagrant .
  SHELL
end
