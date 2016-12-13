<powershell>

Start-Transcript -path C:\output.txt -append

winrm quickconfig -q
winrm set winrm/config '@{MaxTimeoutms="1800000"}'
winrm set winrm/config/service '@{AllowUnencrypted="true"}'
winrm set winrm/config/service/auth '@{Basic="true"}'

netsh advfirewall firewall add rule name="WinRM 5985" protocol=TCP dir=in localport=5985 action=allow

net stop winrm
sc config winrm start=auto

Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope LocalMachine

# http://stackoverflow.com/questions/34325704/setting-windows-server-password-in-ec2-user-data
$ec2config = [xml] (get-content 'C:\\Program Files\\Amazon\\Ec2ConfigService\\Settings\\config.xml')
($ec2config.ec2configurationsettings.plugins.plugin | where {$_.name -eq "Ec2HandleUserData"}).state = "Enabled"
($ec2config.ec2configurationsettings.GlobalSettings.RemoveCredentialsfromSysprepOnStartup) = "false"
$ec2config.save("C:\\Program Files\\Amazon\\Ec2ConfigService\\Settings\\config.xml")

$admin = [adsi]("WinNT://./administrator, user")
$admin.psbase.invoke("SetPassword", "selenium123#")

iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
cinst -y googlechrome wget unzip autologon --allow-empty-checksums
cinst -y jdk8 --version 8.0.102 --allow-empty-checksums
cinst -y seleniumhub --version 2.43.1.2 --allow-empty-checksums
cinst -y selenium-chrome-driver --version 2.25 --allow-empty-checksums

autologon administrator localhost selenium123#

net start winrm
Stop-Transcript
</powershell>
