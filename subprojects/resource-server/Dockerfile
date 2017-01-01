FROM java:openjdk-8-jre-alpine
COPY build/docker/lib/ /app/BOOT-INF/lib/
COPY build/docker/app/ /app/
CMD java -Djava.security.egd=file:/dev/./urandom -Xmx200m -Xms200m -cp /app/ org.springframework.boot.loader.JarLauncher
EXPOSE 8092