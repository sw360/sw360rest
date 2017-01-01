FROM jeanblanchard/tomcat:8

RUN mkdir /etc/sw360
RUN echo "couchdb.url = http://sw360-couchdb:5984" > /etc/sw360/couchdb.properties

COPY build/webapps/*.war /opt/tomcat/webapps/

EXPOSE 8080
