# A Liferay image built from the bundle this branch develops against, so the
# portal-k8s-agent bundles in the image are version compatible with the ones
# built from this source tree.

FROM eclipse-temurin:21-jdk

ENV LIFERAY_HOME=/opt/liferay
ENV CATALINA_HOME=/opt/liferay/tomcat

RUN groupadd --gid 1001 liferay && \
	useradd --create-home --gid 1001 --uid 1001 liferay

COPY --chown=liferay:liferay bundle /opt/liferay

COPY --chown=liferay:liferay entrypoint.sh /usr/local/bin/entrypoint.sh

RUN chmod +x /usr/local/bin/entrypoint.sh /opt/liferay/tomcat/bin/*.sh

USER 1001:1001

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
