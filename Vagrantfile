# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "puppetlabs/centos-7.0-64-puppet"
  config.vm.synced_folder "release/target/", "/emustudio"
  config.ssh.forward_x11 = true

  config.vm.provision "shell", inline: <<-SHELL
    sudo yum update 
    sudo yum install -y unzip
    sudo yum install -y curl
    sudo yum install -y jre
    sudo yum install -y xauth
    cd /emustudio
    unzip -o /emustudio/emuStudio*.zip 
    chown -R vagrant:vagrant .
  SHELL
end
