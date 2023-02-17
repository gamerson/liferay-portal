# TODO Replace this with official version, LPS-176095

FROM alpine:latest

COPY batch/ /src/batch
COPY src/job.sh /src/job.sh
COPY rootCA.pem .

RUN \
	apk add --no-cache bash curl jq tree && \
	chmod +x /src/job.sh

WORKDIR /src/

ENTRYPOINT ["/src/job.sh"]