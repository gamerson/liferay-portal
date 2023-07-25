FROM liferay/jar-runner:latest

#ENV LIFERAY_JAR_RUNNER_JAVA_OPTS="-Xmx512m"

ENV JAVA_VERSION=zulu17

LABEL LABEL_ZULU_17_VERSION="17.0.6"

USER root

RUN curl -H 'accept: */*' -L -s -X 'GET' -o /tmp/jdk17.deb "https://api.azul.com/zulu/download/community/v1.0/bundles/latest/binary/?arch=${TARGETARCH}&bundle_type=jdk&ext=deb&hw_bitness=64&java_version=17.0&javafx=false&os=linux&zulu_version=${LABEL_ZULU_17_VERSION}" && \
	DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt-get install --no-install-recommends --yes /tmp/jdk17.deb && \
	rm /tmp/jdk17.deb && \
	/usr/local/bin/set_java_version.sh

USER liferay

COPY *.jar /opt/liferay/jar-runner.jar