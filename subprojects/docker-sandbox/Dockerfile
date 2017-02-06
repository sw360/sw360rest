#
# Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
#
# Copying and distribution of this file, with or without modification,
# are permitted in any medium without royalty provided the copyright
# notice and this notice are preserved. This file is offered as-is,
# without any warranty.
#

FROM jeanblanchard/tomcat:8
MAINTAINER kai.toedter@siemens.com

RUN mkdir /etc/sw360
RUN echo "couchdb.url = http://sw360-couchdb:5984" > /etc/sw360/couchdb.properties

COPY build/webapps/*.war /opt/tomcat/webapps/

EXPOSE 8080
