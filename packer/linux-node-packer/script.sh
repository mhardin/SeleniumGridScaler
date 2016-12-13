cp google-chrome.repo /etc/yum.repos.d/
yum update -y
yum-config-manager --enable "rhui-REGION-rhel-server-optional"
yum install -y google-chrome-stable wget curl java-1.8.0-openjdk-headless tigervnc-server unzip xorg-x11-server-Xvfb
mkdir -p /opt/selenium \
  && wget --no-verbose https://selenium-release.storage.googleapis.com/2.53/selenium-server-standalone-2.53.0.jar -O /opt/selenium/selenium-server-standalone.jar
sudo useradd seluser --shell /bin/bash --create-home \
  && sudo usermod -a -G wheel seluser \
  && echo 'ALL ALL = (ALL) NOPASSWD: ALL' >> /etc/sudoers \
  && echo 'seluser:secret' | chpasswd
export SCREEN_WIDTH=1360
export SCREEN_HEIGHT=1020
export SCREEN_DEPTH=24
export DISPLAY=:99.0

export CHROME_DRIVER_VERSION=2.21
wget --no-verbose -O /tmp/chromedriver_linux64.zip https://chromedriver.storage.googleapis.com/$CHROME_DRIVER_VERSION/chromedriver_linux64.zip \
  && rm -rf /opt/selenium/chromedriver \
  && unzip /tmp/chromedriver_linux64.zip -d /opt/selenium \
  && rm -f /tmp/chromedriver_linux64.zip \
  && mv -f /opt/selenium/chromedriver /opt/selenium/chromedriver-$CHROME_DRIVER_VERSION \
  && chmod 755 /opt/selenium/chromedriver-$CHROME_DRIVER_VERSION \
  && ln -fs /opt/selenium/chromedriver-$CHROME_DRIVER_VERSION /usr/bin/chromedriver

wget "https://github.com/SeleniumHQ/docker-selenium/raw/master/NodeChrome/chrome_launcher.sh" -O /opt/google/chrome/google-chrome
chmod +x /opt/google/chrome/google-chrome

echo "/home/seluser/grid/grid_start_node.sh >> /home/seluser/grid/process.log 2>&1" >> etc/rc.local
