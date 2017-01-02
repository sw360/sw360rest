#
# Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
#
# Copying and distribution of this file, with or without modification,
# are permitted in any medium without royalty provided the copyright
# notice and this notice are preserved. This file is offered as-is,
# without any warranty.
#

FROM java:openjdk-8-jre-alpine
MAINTAINER kai.toedter@siemens.com

COPY build/docker/lib/ /app/BOOT-INF/lib/
COPY build/docker/app/ /app/
COPY src/main/web/hal-browser /app/BOOT-INF/classes/static
CMD java -Djava.security.egd=file:/dev/./urandom -Xmx200m -Xms200m -cp /app/ org.springframework.boot.loader.JarLauncher
EXPOSE 8091