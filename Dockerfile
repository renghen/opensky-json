FROM openjdk:11-jre
COPY open-sky /open-sky
EXPOSE 9090 9090
CMD /open-sky/bin/start -Dplay.http.secret.key=secrete123