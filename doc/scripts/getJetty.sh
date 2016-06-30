#!/bin/sh
set -e
JETTY=C:/jars/jetty-distribution-9.1.0.RC0/org.eclipse.jetty.project

pushd ../../lib/jetty/org/eclipse/jetty
rm -fR *

pushd $JETTY

git checkout refs/tags/jetty-9.2.16.v20160407
popd


for package in http io security server servlet util
do
    cp -R $JETTY/jetty-$package/src/main/java/org/eclipse/jetty/$package .
done

cp -R $JETTY/jetty-http/src/main/resources/org/eclipse/jetty/http .



cp -R $JETTY/jetty-$package/src/main/java/org/eclipse/jetty/$package .

rm -R server/session/jmx
rm -R server/handler/jmx
rm -R server/jmx
rm -R servlet/jmx

rm util/log/JettyAwareLogger.java
rm util/log/Slf4jLog.java
rm server/Slf4jRequestLog.java
popd
