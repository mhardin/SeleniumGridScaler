netsh advfirewall firewall add rule name="SeleniumNode" protocol=TCP dir=in localport=5555 action=allow

cinst -y googlechrome wget unzip autologon --allow-empty-checksums
cinst -y jdk8 --version 8.0.102 --allow-empty-checksums
cinst -y seleniumhub --version 2.43.1.2 --allow-empty-checksums
cinst -y selenium-chrome-driver --version 2.25 --allow-empty-checksums

$EC2_INSTANCE_ID=(C:\ProgramData\chocolatey\lib\wget\tools\wget -q -O - http://169.254.169.254/latest/meta-data/instance-id)
$env:NODE_TEMPLATE = "c:\tools\selenium\nodeConfigTemplate.json"
C:\ProgramData\chocolatey\lib\wget\tools\wget http://169.254.169.254/latest/user-data -O c:\tools\selenium\data.zip
C:\ProgramData\chocolatey\lib\unzip\tools\unzip -o c:\tools\selenium\data.zip -d c:\tools\selenium

cat $env:NODE_TEMPLATE | %{$_ -replace "<INSTANCE_ID>","$EC2_INSTANCE_ID"} | Set-Content -Encoding Ascii c:\tools\selenium\nodeConfig.json

## http://stackoverflow.com/questions/33150351/how-do-i-install-chromedriver-on-windows-10-and-run-selenium-tests-with-chrome
cp c:\tools\selenium\chromedriver.exe c:\windows

# # Finally, run the java process in a window so browsers can run
& 'C:\Program Files\Java\jdk1.8.0_102\bin\java.exe' -jar  C:\SeleniumHub\wrapper-windows-x86-32-3.5.25\lib\selenium-server-standalone-2.43.1.jar -role node -nodeConfig c:\tools\selenium\nodeConfig.json -log c:\tools\selenium\log
